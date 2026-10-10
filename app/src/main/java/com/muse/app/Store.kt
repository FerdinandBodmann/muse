package com.muse.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class Store(ctx: Context) {

    private val prefs: SharedPreferences = try {
        val mk = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx, "muse_secure", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        ctx.getSharedPreferences("muse_plain", Context.MODE_PRIVATE)
    }

    private val chatDir = File(ctx.filesDir, "chats").apply { mkdirs() }
    private val imgDir = File(ctx.filesDir, "images").apply { mkdirs() }

    // ---- providers ----
    fun apiKey(id: String): String = prefs.getString("key_$id", "") ?: ""
    fun baseUrl(p: Provider): String = prefs.getString("url_${p.id}", null) ?: p.baseUrl
    fun saveProvider(id: String, key: String, url: String) {
        prefs.edit().putString("key_$id", key).putString("url_$id", url).apply()
    }

    fun models(p: Provider): List<String> {
        val s = prefs.getString("models_${p.id}", null) ?: return p.models
        return try {
            val a = JSONArray(s)
            List(a.length()) { a.getString(it) }
        } catch (e: Exception) {
            p.models
        }
    }

    fun saveModels(id: String, list: List<String>) {
        prefs.edit().putString("models_$id", JSONArray(list).toString()).apply()
    }

    // ---- selection ----
    fun selectedModel(): ModelRef? {
        val p = prefs.getString("sel_p", null)
        val m = prefs.getString("sel_m", null)
        return if (p != null && m != null) ModelRef(p, m) else null
    }

    fun saveSelectedModel(r: ModelRef) {
        prefs.edit().putString("sel_p", r.providerId).putString("sel_m", r.model).apply()
    }

    fun compareRefs(): List<ModelRef> {
        val s = prefs.getString("compare", null) ?: return emptyList()
        return try {
            val a = JSONArray(s)
            List(a.length()) { val o = a.getJSONObject(it); ModelRef(o.getString("p"), o.getString("m")) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCompareRefs(list: List<ModelRef>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("p", it.providerId).put("m", it.model)) }
        prefs.edit().putString("compare", a.toString()).apply()
    }

    // ---- personas ----
    fun personaId(): String = prefs.getString("persona", "default") ?: "default"
    fun savePersonaId(id: String) {
        prefs.edit().putString("persona", id).apply()
    }

    fun personas(): List<Persona> {
        val s = prefs.getString("personas", null) ?: return DEFAULT_PERSONAS
        return try {
            val a = JSONArray(s)
            List(a.length()) {
                val o = a.getJSONObject(it)
                Persona(o.getString("id"), o.getString("name"), o.optString("prompt"))
            }
        } catch (e: Exception) {
            DEFAULT_PERSONAS
        }
    }

    fun savePersonas(list: List<Persona>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("prompt", it.prompt)) }
        prefs.edit().putString("personas", a.toString()).apply()
    }

    // ---- conversations ----
    fun listConversations(): List<Conversation> =
        (chatDir.listFiles() ?: emptyArray())
            .mapNotNull { f -> try { parseConv(JSONObject(f.readText())) } catch (e: Exception) { null } }
            .sortedByDescending { it.updatedAt }

    fun saveConversation(c: Conversation) {
        File(chatDir, "${c.id}.json").writeText(toJson(c).toString())
    }

    fun deleteConversation(c: Conversation) {
        c.messages.flatMap { it.images }.forEach { File(imgDir, it).delete() }
        File(chatDir, "${c.id}.json").delete()
    }

    private fun parseConv(o: JSONObject): Conversation {
        val arr = o.getJSONArray("messages")
        val msgs = List(arr.length()) { i ->
            val m = arr.getJSONObject(i)
            val im = m.optJSONArray("images") ?: JSONArray()
            ChatMessage(
                id = m.getString("id"),
                role = m.getString("role"),
                text = m.optString("text"),
                images = List(im.length()) { im.getString(it) },
                model = if (m.has("model")) m.getString("model") else null,
                error = if (m.has("error")) m.getString("error") else null,
            )
        }
        return Conversation(o.getString("id"), o.optString("title", "Chat"), o.optLong("updatedAt"), msgs)
    }

    private fun toJson(c: Conversation): JSONObject {
        val arr = JSONArray()
        c.messages.forEach { m ->
            val o = JSONObject()
                .put("id", m.id).put("role", m.role).put("text", m.text)
                .put("images", JSONArray(m.images))
            if (m.model != null) o.put("model", m.model)
            if (m.error != null) o.put("error", m.error)
            arr.put(o)
        }
        return JSONObject().put("id", c.id).put("title", c.title)
            .put("updatedAt", c.updatedAt).put("messages", arr)
    }

    // ---- images ----
    fun saveImage(bytes: ByteArray): String {
        val name = "${UUID.randomUUID()}.jpg"
        File(imgDir, name).writeBytes(bytes)
        return name
    }

    fun deleteImage(name: String) {
        File(imgDir, name).delete()
    }

    fun imageB64(name: String): String =
        Base64.encodeToString(File(imgDir, name).readBytes(), Base64.NO_WRAP)

    fun loadBitmap(name: String, sample: Int = 1): Bitmap? {
        val f = File(imgDir, name)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    companion object {
        val DEFAULT_PERSONAS = listOf(
            Persona("default", "Default", ""),
            Persona("concise", "Concise", "Answer briefly and directly. No filler, no preamble."),
            Persona("coder", "Coder", "You are a senior software engineer. Give correct, idiomatic code with short explanations."),
            Persona("writer", "Editor", "You are a careful editor. Improve clarity and tone while keeping the author's voice."),
        )
    }
}
