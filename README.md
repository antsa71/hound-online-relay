# Hound Online Relay

Hound Android file-sharing application with an Internet relay powered by Cloudflare Workers and Durable Objects.

## Repository layout

- `android/` — Android app source with `server` and `client` product flavors.
- `worker/` — Cloudflare Worker relay and Durable Object configuration.
- `SETUP.md` — setup guide in Malagasy: GitHub, Cloudflare, APK build, installation, and troubleshooting.

## Quick links

- Deploy the Worker from `worker/` with `npx wrangler deploy`.
- Build the Android variants from `android/` with `./gradlew assembleServerDebug assembleClientDebug`.

Do not commit pairing tokens, Cloudflare API tokens, keystores, or passwords.
