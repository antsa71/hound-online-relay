# Hound Online Relay — deployment

This Worker implements the API expected by `RelayApi.kt`. It uses a Cloudflare Durable Object per 16-character pairing token, so the Server and Client can communicate over the Internet without being on the same Wi-Fi. Files are streamed only after a Client requests them.

## Deploy from a computer with Node.js and Wrangler

1. Install Wrangler: `npm install -g wrangler`
2. Log in: `wrangler login`
3. From this `discovery` folder run: `wrangler deploy`
4. Copy the deployed URL, for example `https://hound-online-relay.<your-subdomain>.workers.dev`.
5. Build the two APK variants with that URL:

```bash
cd ..
./gradlew assembleServerDebug assembleClientDebug \
  -PhoundRelayUrl=https://hound-online-relay.<your-subdomain>.workers.dev \
  -PhoundPairingToken=YOUR16CHARACTERTOKEN
```

Use a random 16-character uppercase alphanumeric token and keep it private. If `houndPairingToken` is not compiled into the Client, the Client asks the user to enter the token shown by/provided from the Server. Both apps must use the same token and the same Worker URL.

## Important notes

- The Server must remain open/running and Android must allow its foreground service/background activity. If it stops, remote downloads will wait until it returns.
- Grant the Server access only to folders/files you intend to share. The pairing token grants access to the file list and requested files.
- This is a relay implementation, not a file-hosting service. It stores the manifest and requested transfer chunks temporarily in Durable Object storage; the Client calls `/api/download/complete` to remove transfer chunks.
- Deploying this Worker is required. The APK cannot make a Worker exist automatically, and I cannot deploy into your Cloudflare account without your authorization/access.
