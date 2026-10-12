package com.muse.app

import java.util.UUID

enum class ApiType { OPENAI, ANTHROPIC }

data class Provider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val type: ApiType = ApiType.OPENAI,
    val models: List<String> = emptyList(),
)

data class ModelRef(val providerId: String, val model: String)

data class Persona(val id: String, val name: String, val prompt: String)

data class ChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val images: List<String> = emptyList(),
    val model: String? = null,
    val error: String? = null,
    /** Chain-of-thought text streamed by reasoning models, kept separate from the answer. */
    val reasoning: String? = null,
)

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>,
    /** Per-conversation sampling overrides. Null means "let the provider decide". */
    val temperature: Float? = null,
    val maxTokens: Int? = null,
)

/** One turn sent to an API. Images are base64 JPEG strings. */
data class Turn(val role: String, val text: String, val images: List<String> = emptyList())

fun newId(): String = UUID.randomUUID().toString()

fun blankConversation() = Conversation(newId(), "New chat", System.currentTimeMillis(), emptyList())

object Providers {
    val all = listOf(
        Provider("anthropic", "Anthropic", "https://api.anthropic.com/v1", ApiType.ANTHROPIC,
            listOf("claude-sonnet-5-5", "claude-opus-5-5", "claude-haiku-5-5")),
        Provider("openai", "OpenAI", "https://api.openai.com/v1",
            models = listOf("gpt-4o", "gpt-4o-mini")),
        Provider("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai",
            models = listOf("gemini-2.5-flash", "gemini-2.5-pro")),
        Provider("perplexity", "Perplexity", "https://api.perplexity.ai",
            models = listOf("sonar", "sonar-pro", "sonar-reasoning-pro", "sonar-deep-research")),
        Provider("openrouter", "OpenRouter", "https://openrouter.ai/api/v1",
            models = listOf("openrouter/auto")),
        Provider("groq", "Groq", "https://api.groq.com/openai/v1",
            models = listOf("llama-3.3-70b-versatile")),
        Provider("mistral", "Mistral", "https://api.mistral.ai/v1",
            models = listOf("mistral-large-latest", "mistral-small-latest")),
        Provider("deepseek", "DeepSeek", "https://api.deepseek.com",
            models = listOf("deepseek-chat", "deepseek-reasoner")),
        Provider("xai", "xAI", "https://api.x.ai/v1", models = listOf("grok-3")),
        Provider("together", "Together AI", "https://api.together.xyz/v1",
            models = listOf("meta-llama/Llama-3.3-70B-Instruct-Turbo")),
        Provider("cerebras", "Cerebras", "https://api.cerebras.ai/v1",
            models = listOf("llama-3.3-70b")),
        Provider("fireworks", "Fireworks", "https://api.fireworks.ai/inference/v1",
            models = listOf("accounts/fireworks/models/llama-v3p3-70b-instruct")),
        Provider("nvidia", "NVIDIA NIM", "https://integrate.api.nvidia.com/v1",
            models = listOf("meta/llama-3.3-70b-instruct")),
        Provider("sambanova", "SambaNova", "https://api.sambanova.ai/v1",
            models = listOf("Meta-Llama-3.3-70B-Instruct")),
        Provider("huggingface", "Hugging Face", "https://router.huggingface.co/v1",
            models = listOf("meta-llama/Llama-3.3-70B-Instruct")),
        Provider("ollama", "Ollama", "http://localhost:11434/v1"),
        Provider("lmstudio", "LM Studio", "http://localhost:1234/v1"),
        Provider("moonshot", "Moonshot (Kimi)", "https://api.moonshot.ai/v1"),
        Provider("custom", "Custom (OpenAI-compatible)", ""),
    )

    fun byId(id: String): Provider? = all.firstOrNull { it.id == id }
}
