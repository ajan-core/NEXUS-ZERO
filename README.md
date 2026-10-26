# JANIXIA AI — Personal Assistant Starter

A mobile-first, premium dark UI starter for a voice-first personal assistant.

## What works in this starter
- Responsive mobile and desktop layout
- Animated reactive-style orb
- Chat interface with local demo responses
- Quick command chips
- Browser speech recognition where supported and permitted
- Browser text-to-speech
- Stop speaking and clear chat controls
- No API key embedded in frontend

## Important current limitation
This is a front-end starter/demo, not yet connected to Gemini or OpenAI. Live AI responses require a backend/server function that calls the provider securely using a server-side secret. Do not place API keys in `index.html` or any public client-side code. Direct Android app control requires a native Android layer and user permissions; this web demo cannot perform those actions.

## Publish with GitHub Pages
1. Open this repository on GitHub.
2. Go to **Settings → Pages**.
3. Under **Build and deployment**, select **Deploy from a branch**.
4. Choose branch **main**, folder **/(root)**, then save.
5. Wait for GitHub Pages to finish publishing. The site URL should be shown in the Pages section.

Repository: https://github.com/ajan-core/NEXUS-ZERO

## Next steps
1. Add a server-side Gemini integration with secret management.
2. Add error handling, rate limits and privacy disclosures.
3. Test voice input on supported browsers and devices.
4. If an Android APK is needed, build a native wrapper or Android project separately and test on-device.

This project is an independent concept and does not copy proprietary LIA assets or branding.
