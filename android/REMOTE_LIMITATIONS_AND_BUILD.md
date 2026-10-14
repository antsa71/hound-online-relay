# Hound role-separated build preparation

This source now defines two Android product flavors:
- `server`: `com.straylabs.hound.server`, opens the Server tab only.
- `client`: `com.straylabs.hound.client`, opens the Client tab only.

## Build
Use Android Studio with JDK 17 and Android SDK 35, then run:

```bash
./gradlew assembleServerDebug assembleClientDebug
```

Expected APKs (after a successful build):
- `app/build/outputs/apk/server/debug/app-server-debug.apk`
- `app/build/outputs/apk/client/debug/app-client-debug.apk`

## Important limitation
This is a role-separated build of Hound's existing LAN HTTP server/client. It does **not** implement a remote relay, device pairing service, or secure internet transport. Entering a public IP alone will not make it work through NAT/mobile networks. Before using it over the internet, implement a secured relay/VPN, TLS, strong authentication, and owner-visible sharing controls. Do not expose the existing HTTP endpoints directly to the public internet; the server includes upload/delete functionality and the current local-network HTTP setup is not suitable for public exposure.

The server requires the user to choose/authorize the shared folder. Android may restrict automatic background startup, and a long-running foreground service normally needs a visible notification.
