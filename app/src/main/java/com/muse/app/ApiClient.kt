package com.muse.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.max

object ApiClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    private val JSON_TYPE = "application/json".toMediaType()

    /** Streams text deltas. Errors surface as exceptions on the flow. */
    fun stream(
        provider: Provider,
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        turns: List<Turn>,
    ): Flow<String> = callbackFlow {
        var call: Call? = null
        val job = launch(Dispatchers.IO) {
            try {
                val c = http.newCall(buildRequest(provider, baseUrl, apiKey, model, system, turns))
                call = c
                c.execute().use { resp ->
                    val body = resp.body ?: throw IOException("Empty response")
                    if (!resp.isSuccessful) throw IOException(errorMessage(resp.code, body.string()))
                    val source = body.source()
                    var citations: JSONArray? = null
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val data = line.removePrefix("data:").trim()
                        if (data.isEmpty() || data == "[DONE]") continue
                        val j = try { JSONObject(data) } catch (e: Exception) { continue }
                        if (provider.type == ApiType.OPENAI) {
                            j.optJSONArray("citations")?.let { citations = it }
                            val delta = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                            if (delta != null && !delta.isNull("content")) {
                                val t = delta.optString("content", "")
                                if (t.isNotEmpty()) trySend(t)
                            }
                        } else {
                            when (j.optString("type")) {
                                "content_block_delta" -> {
                                    val t = j.optJSONObject("delta")?.optString("text", "") ?: ""
                                    if (t.isNotEmpty()) trySend(t)
                                }
                                "error" -> throw IOException(
                                    j.optJSONObject("error")?.optString("message") ?: "Provider error"
                                )
                            }
                        }
                    }
                    citations?.let { c2 ->
                        if (c2.length() > 0) trySend(sourcesText(c2))
                    }
                }
                close()
            } catch (e: Exception) {
                close(e)
            }
        }
        awaitClose {
            call?.cancel()
            job.cancel()
        }
    }

    private fun sourcesText(c: JSONArray): String {
        val sb = StringBuilder("\n\n**Sources**\n")
        for (i in 0 until c.length()) {
            val u = c.optString(i)
            val host = try { java.net.URI(u).host ?: u } catch (e: Exception) { u }
            sb.append("- [").append(i + 1).append("] [").append(host.removePrefix("www."))
                .append("](").append(u).append(")\n")
        }
        return sb.toString()
    }

    private fun buildRequest(
        provider: Provider, baseUrl: String, apiKey: String,
        model: String, system: String, turns: List<Turn>,
    ): Request {
        require(baseUrl.isNotBlank()) { "Base URL is missing" }
        val base = baseUrl.trimEnd('/')
        val msgs = JSONArray()
        return if (provider.type == ApiType.ANTHROPIC) {
            turns.forEach { t ->
                val o = JSONObject().put("role", t.role)
                if (t.images.isEmpty()) {
                    o.put("content", t.text)
                } else {
                    val parts = JSONArray()
                    t.images.forEach { b64 ->
                        parts.put(
                            JSONObject().put("type", "image").put(
                                "source",
                                JSONObject().put("type", "base64").put("media_type", "image/jpeg").put("data", b64)
                            )
                        )
                    }
                    if (t.text.isNotBlank()) parts.put(JSONObject().put("type", "text").put("text", t.text))
                    o.put("content", parts)
                }
                msgs.put(o)
            }
            val body = JSONObject().put("model", model).put("max_tokens", 8192)
                .put("stream", true).put("messages", msgs)
            if (system.isNotBlank()) body.put("system", system)
            Request.Builder().url("$base/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .post(body.toString().toRequestBody(JSON_TYPE)).build()
        } else {
            if (system.isNotBlank()) msgs.put(JSONObject().put("role", "system").put("content", system))
            turns.forEach { t ->
                val o = JSONObject().put("role", t.role)
                if (t.images.isEmpty()) {
                    o.put("content", t.text)
                } else {
                    val parts = JSONArray()
                    if (t.text.isNotBlank()) parts.put(JSONObject().put("type", "text").put("text", t.text))
                    t.images.forEach { b64 ->
                        parts.put(
                            JSONObject().put("type", "image_url").put(
                                "image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64")
                            )
                        )
                    }
                    o.put("content", parts)
                }
                msgs.put(o)
            }
            val body = JSONObject().put("model", model).put("messages", msgs).put("stream", true)
            val rb = Request.Builder().url("$base/chat/completions")
                .post(body.toString().toRequestBody(JSON_TYPE))
            if (apiKey.isNotBlank()) rb.header("Authorization", "Bearer $apiKey")
            rb.build()
        }
    }

    suspend fun fetchModels(provider: Provider, baseUrl: String, apiKey: String): List<String> =
        withContext(Dispatchers.IO) {
            require(baseUrl.isNotBlank()) { "Base URL is missing" }
            val base = baseUrl.trimEnd('/')
            val rb = if (provider.type == ApiType.ANTHROPIC) {
                Request.Builder().url("$base/models?limit=1000")
                    .header("x-api-key", apiKey).header("anthropic-version", "2023-06-01")
            } else {
                Request.Builder().url("$base/models").apply {
                    if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
                }
            }
            http.newCall(rb.build()).execute().use { r ->
                val text = r.body?.string()
                if (!r.isSuccessful) throw IOException(errorMessage(r.code, text))
                val arr = JSONObject(text ?: "{}").optJSONArray("data") ?: JSONArray()
                (0 until arr.length())
                    .mapNotNull { arr.optJSONObject(it)?.optString("id") }
                    .map { it.removePrefix("models/") }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
            }
        }

    private fun errorMessage(code: Int, body: String?): String {
        val detail: String? = try {
            val j = JSONObject(body ?: "")
            val e = j.opt("error")
            when {
                e is JSONObject -> e.optString("message")
                e is String -> e
                else -> j.optString("message")
            }
        } catch (ex: Exception) {
            body?.take(200)
        }
        return "HTTP $code" + if (!detail.isNullOrBlank()) ": $detail" else ""
    }
}

/** Decode, downscale and re-encode an image as JPEG so uploads stay small. */
fun loadScaledJpeg(ctx: Context, uri: Uri, maxDim: Int = 1280): ByteArray? {
    val cr = ctx.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
    val scale = maxDim.toFloat() / max(bmp.width, bmp.height)
    val out = if (scale < 1f)
        Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
    else bmp
    val bos = ByteArrayOutputStream()
    out.compress(Bitmap.CompressFormat.JPEG, 85, bos)
    return bos.toByteArray()
}
