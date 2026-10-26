# JANIXIA AI Android starter

This Android project builds a debug APK with GitHub Actions. It provides a native chat UI and API Settings for the user's own Google Gemini key or an OpenAI-compatible endpoint.

## Build
Open the Actions tab and select **Build JANIXIA AI APK**. The workflow uploads an artifact named **JANIXIA-AI-debug-APK**. Download the artifact ZIP and extract `app-debug.apk`.

## Important
- No API key is bundled in the app. Each user must provide their own key.
- API usage may cost money depending on provider/account limits.
- This is an early starter app, not a full personal assistant. It does not control phone settings or perform actions outside the app.
- API keys are stored in app-private preferences, not encrypted vault storage. Do not use a shared device.
