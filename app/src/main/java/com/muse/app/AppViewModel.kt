package com.muse.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One column in the compare view. */
class CompareSlot(val ref: ModelRef) {
    var text by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var running by mutableStateOf(true)
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val store = Store(app)

    // ---- provider configuration ----
    var keys by mutableStateOf(Providers.all.associate { it.id to store.apiKey(it.id) })
        private set
    var urls by mutableStateOf(Providers.all.associate { it.id to store.baseUrl(it) })
        private set
    var modelLists by mutableStateOf(Providers.all.associate { it.id to store.models(it) })
        private set
    var fetching by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)

    // ---- chat ----
    var conversations by mutableStateOf(store.listConversations())
        private set
    var current by mutableStateOf(blankConversation())
        private set
    var selected by mutableStateOf(store.selectedModel())
        private set
    var streaming by mutableStateOf(false)
        private set
    var pendingImages by mutableStateOf<List<String>>(emptyList())
        private set
    private var job: Job? = null

    // ---- personas ----
    var personas by mutableStateOf(store.personas())
        private set
    var personaId by mutableStateOf(store.personaId())
        private set

    // ---- compare ----
    var compareRefs by mutableStateOf(store.compareRefs())
        private set
    var compareSlots by mutableStateOf<List<CompareSlot>>(emptyList())
        private set
    var comparePrompt by mutableStateOf("")
        private set
    var compareImages by mutableStateOf<List<String>>(emptyList())
        private set
    private var compareJobs: List<Job> = emptyList()

    // =========================================================== providers

    fun isConfigured(p: Provider): Boolean =
        if (p.id == "custom") urls[p.id].orEmpty().isNotBlank() else keys[p.id].orEmpty().isNotBlank()

    val configured: List<Provider>
        get() = Providers.all.filter { isConfigured(it) }

    fun saveProvider(p: Provider, key: String, url: String) {
        val k = key.trim()
        val u = url.trim().ifBlank { p.baseUrl }
        store.saveProvider(p.id, k, u)
        keys = keys + (p.id to k)
        urls = urls + (p.id to u)
        if (selected == null && isConfigured(p)) {
            modelLists[p.id]?.firstOrNull()?.let { choose(ModelRef(p.id, it)) }
        }
    }

    fun addModel(p: Provider, model: String) {
        val list = ((modelLists[p.id] ?: emptyList()) + model).distinct()
        store.saveModels(p.id, list)
        modelLists = modelLists + (p.id to list)
        if (selected == null) choose(ModelRef(p.id, model))
    }

    fun refreshModels(p: Provider) {
        viewModelScope.launch {
            fetching = p.id
            try {
                val list = ApiClient.fetchModels(p, urls[p.id].orEmpty(), keys[p.id].orEmpty())
                if (list.isEmpty()) {
                    notice = "${p.name}: no models returned"
                } else {
                    store.saveModels(p.id, list)
                    modelLists = modelLists + (p.id to list)
                    notice = "${p.name}: ${list.size} models"
                    if (selected == null) choose(ModelRef(p.id, list.first()))
                }
            } catch (e: Exception) {
                notice = "${p.name}: ${e.message ?: "couldn't fetch models"}. Add model IDs manually if needed."
            }
            fetching = null
        }
    }

    fun choose(ref: ModelRef) {
        selected = ref
        store.saveSelectedModel(ref)
    }

    // =========================================================== personas

    private val systemPrompt: String
        get() = personas.firstOrNull { it.id == personaId }?.prompt.orEmpty()

    val personaName: String
        get() = personas.firstOrNull { it.id == personaId }?.name ?: "Default"

    fun setPersona(id: String) {
        personaId = id
        store.savePersonaId(id)
    }

    fun savePersona(p: Persona) {
        personas = if (personas.any { it.id == p.id }) personas.map { if (it.id == p.id) p else it } else personas + p
        store.savePersonas(personas)
    }

    fun deletePersona(id: String) {
        if (id == "default") return
        personas = personas.filter { it.id != id }
        if (personaId == id) setPersona("default")
        store.savePersonas(personas)
    }

    // =========================================================== chat

    fun newChat() {
        stop()
        pendingImages.forEach { store.deleteImage(it) }
        pendingImages = emptyList()
        current = blankConversation()
    }

    fun openConversation(c: Conversation) {
        stop()
        current = c
    }

    fun deleteConversation(c: Conversation) {
        if (c.id == current.id) {
            stop()
            current = blankConversation()
        }
        store.deleteConversation(c)
        conversations = conversations.filter { it.id != c.id }
    }

    fun attach(uris: List<Uri>, forCompare: Boolean = false) {
        viewModelScope.launch {
            val names = withContext(Dispatchers.IO) {
                uris.mapNotNull { u ->
                    try { loadScaledJpeg(getApplication<Application>(), u)?.let { store.saveImage(it) } } catch (e: Exception) { null }
                }
            }
            if (forCompare) compareImages = (compareImages + names).take(4)
            else pendingImages = (pendingImages + names).take(4)
        }
    }

    fun removeImage(name: String, forCompare: Boolean = false) {
        store.deleteImage(name)
        if (forCompare) compareImages = compareImages - name else pendingImages = pendingImages - name
    }

    fun send(text: String) {
        val sel = selected
        val provider = sel?.let { Providers.byId(it.providerId) }
        if (sel == null || provider == null || !isConfigured(provider)) {
            notice = "Add an API key and pick a model first"
            return
        }
        if (streaming) return
        val clean = text.trim()
        val imgs = pendingImages
        if (clean.isEmpty() && imgs.isEmpty()) return

        val user = ChatMessage(newId(), "user", clean, imgs)
        val bot = ChatMessage(newId(), "assistant", "", model = sel.model)
        val first = current.messages.isEmpty()
        current = current.copy(
            title = if (first) titleFrom(clean) else current.title,
            updatedAt = System.currentTimeMillis(),
            messages = current.messages + user + bot,
        )
        pendingImages = emptyList()
        streaming = true

        val convId = current.id
        val history = current.messages.dropLast(1)
        val system = systemPrompt
        job = viewModelScope.launch {
            val sb = StringBuilder()
            try {
                ApiClient.stream(
                    provider, urls[provider.id].orEmpty(), keys[provider.id].orEmpty(),
                    sel.model, system, toTurns(history),
                ).collect { d ->
                    sb.append(d)
                    setLast(sb.toString(), null)
                }
                if (sb.isEmpty()) setLast("", "The model returned an empty response")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setLast(sb.toString(), e.message ?: "Request failed")
            } finally {
                streaming = false
                if (current.id == convId) persist()
            }
        }
    }

    fun regenerate() {
        if (streaming) return
        val msgs = current.messages
        val idx = msgs.indexOfLast { it.role == "user" }
        if (idx < 0) return
        val u = msgs[idx]
        current = current.copy(messages = msgs.take(idx))
        pendingImages = u.images
        send(u.text)
    }

    fun stop() {
        job?.cancel()
        job = null
        streaming = false
        val last = current.messages.lastOrNull()
        if (last != null && last.role == "assistant" && last.text.isBlank() && last.error == null) {
            current = current.copy(messages = current.messages.dropLast(1))
        }
        persist()
    }

    private fun toTurns(msgs: List<ChatMessage>): List<Turn> =
        msgs.filter { it.role == "user" || it.text.isNotBlank() }
            .map { m -> Turn(m.role, m.text, m.images.mapNotNull { runCatching { store.imageB64(it) }.getOrNull() }) }

    private fun setLast(text: String, error: String?) {
        val msgs = current.messages
        val last = msgs.lastOrNull() ?: return
        if (last.role != "assistant") return
        current = current.copy(messages = msgs.dropLast(1) + last.copy(text = text, error = error))
    }

    private fun persist() {
        val c = current
        if (c.messages.isEmpty()) return
        store.saveConversation(c)
        conversations = listOf(c) + conversations.filter { it.id != c.id }
    }

    private fun titleFrom(text: String): String =
        text.lineSequence().first().take(42).ifBlank { "Image" }

    // =========================================================== compare

    fun toggleCompare(ref: ModelRef) {
        compareRefs = when {
            ref in compareRefs -> compareRefs - ref
            compareRefs.size < 3 -> compareRefs + ref
            else -> compareRefs
        }
        store.saveCompareRefs(compareRefs)
    }

    fun runCompare(prompt: String) {
        val imgs = compareImages
        val clean = prompt.trim()
        if (clean.isEmpty() && imgs.isEmpty()) return
        if (compareRefs.isEmpty()) {
            notice = "Pick at least one model to compare"
            return
        }
        cancelCompare()
        val system = systemPrompt
        val turns = listOf(Turn("user", clean, imgs.mapNotNull { runCatching { store.imageB64(it) }.getOrNull() }))
        val slots = compareRefs.map { CompareSlot(it) }
        compareSlots = slots
        comparePrompt = clean
        compareImages = emptyList()
        compareJobs = slots.map { slot ->
            viewModelScope.launch {
                val p = Providers.byId(slot.ref.providerId)
                if (p == null || !isConfigured(p)) {
                    slot.error = "Provider isn't set up"
                    slot.running = false
                    return@launch
                }
                try {
                    ApiClient.stream(p, urls[p.id].orEmpty(), keys[p.id].orEmpty(), slot.ref.model, system, turns)
                        .collect { slot.text += it }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    slot.error = e.message ?: "Request failed"
                } finally {
                    slot.running = false
                }
            }
        }
    }

    fun cancelCompare() {
        compareJobs.forEach { it.cancel() }
        compareJobs = emptyList()
    }
}
