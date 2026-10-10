# Muse

A minimal Android client for your own AI API keys. Anthropic, OpenAI, Gemini, Perplexity,
OpenRouter, Groq, Mistral, DeepSeek, xAI, Together, Cerebras, Fireworks, NVIDIA NIM,
SambaNova, Hugging Face, Moonshot, plus any OpenAI-compatible endpoint.

Features: streaming chat, saved history, image attachments, side-by-side model compare,
personas (system prompts), light/dark following the system.

## Build with GitHub Actions
1. Create a repo and push this folder (the `.github` folder must be at the repo root).
2. Open the **Actions** tab, then run **Build APK** (it also runs on every push to main).
3. Download `muse-debug-apk` from the run's Artifacts, unzip, and install the APK.

Each CI build uses a fresh debug signing key, so uninstall the previous build before installing a new one.

## Local build
Open the folder in Android Studio and run the `app` configuration.
