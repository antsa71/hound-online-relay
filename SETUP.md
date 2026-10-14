# Hound Online Relay — Torolalana feno

Ity torolalana ity dia manazava ny fomba hampandehanana ilay Hound Android app miaraka amin’ny Cloudflare Worker relay, manomboka amin’ny famoronana repository GitHub ka hatramin’ny fametrahana sy fanandramana APK.

## 1. Zavatra ilaina

Mila manana:

- Kaonty GitHub.
- Kaonty Cloudflare.
- Solosaina misy **Android Studio**, **JDK 17**, ary **Android SDK 35**.
- Node.js 16.17 na ambony ary npm.
- Git.
- Ireo rakitra roa nomenao:
  - `Hound-online-relay-fixed-source.zip`
  - `Hound-cloudflare-relay-worker.zip`

Ny build test natao tamin’ity tontolo ity dia tsy afaka namokatra APK satria tsy nisy Android SDK, fa nahavita namaky sy nanamarina ny Gradle configuration. Mila atao amin’ny Android Studio na solosaina efa misy Android SDK ny build farany.

## 2. Firafitry ny repository GitHub

Aleo repository iray no mitahiry ny Android app sy ny Cloudflare Worker, toy izao:

```text
Hound-online-relay/
├── android/
│   ├── app/
│   ├── gradle/
│   ├── gradlew
│   ├── build.gradle.kts
│   └── ...
└── worker/
    ├── worker.js
    ├── wrangler.toml
    └── README.md
```

### Fomba famoronana azy

1. Extract-o `Hound-online-relay-fixed-source.zip`.
2. Ao anatiny dia misy folder `Hound-main`. Ovay anarana ho `android`.
3. Extract-o `Hound-cloudflare-relay-worker.zip` ary ovay anarana ho `worker`.
4. Apetraho ao anaty folder iray `Hound-online-relay` ireo folder roa ireo.

Aza ampidirina ao GitHub ireto zavatra ireto:

- `android/local.properties`
- `android/.gradle/`
- `android/build/`
- `android/app/build/`
- `.idea/`
- Android keystore na password.
- Cloudflare API token.
- Pairing token tena ampiasainao raha tsy ilaina.

Mamoròna `.gitignore` ao amin’ny root `Hound-online-relay`:

```gitignore
# Android / Gradle
**/.gradle/
**/build/
**/local.properties
.idea/
*.iml
captures/
.externalNativeBuild/
.cxx/

# Secrets and local configuration
.env
.env.*
.dev.vars
*.keystore
*.jks

# Node
node_modules/

# OS files
.DS_Store
Thumbs.db
```

## 3. Famoronana repository ao GitHub

Ao GitHub:

1. Tsindrio **New repository**.
2. Omeo anarana, ohatra `Hound-online-relay`.
3. Safidio **Private** raha tsy tianao ho hitan’ny olona ny source.
4. Aza asiana README, license, na `.gitignore` rehefa mamorona azy, satria efa manana ireo ao amin’ny local folder ianao.
5. Tsindrio **Create repository**.

Avy eo sokafy Terminal ao amin’ny folder root `Hound-online-relay`:

```bash
git init -b main
git add .
git commit -m "Initial Hound Android app and Cloudflare relay"
git remote add origin https://github.com/USERNAME/Hound-online-relay.git
git push -u origin main
```

Soloy `USERNAME` amin’ny GitHub username-nao.

Raha mangataka authentication i Git, ampiasao GitHub login/credential manager na GitHub CLI. Aza manoratra password na token ao anatin’ny source.

GitHub mihitsy no mampitandrina fa tsy tokony hatao `git add`, `commit`, na `push` ny API keys, passwords, na secrets [1].

## 4. Fanomanana Cloudflare Worker

### Safidy A — Deploy voalohany amin’ny Wrangler

Ity no fomba tsotra indrindra hanamarinana fa mandeha ilay Worker.

Ao amin’ny Terminal:

```bash
cd Hound-online-relay/worker
npm install -g wrangler
npx wrangler login
npx wrangler deploy
```

Hivoaka amin’ny farany ny URL mitovitovy amin’izao:

```text
https://hound-online-relay.<subdomain>.workers.dev
```

Tehirizo io URL io. Izy no ampidirina amin’ny Android build.

Ny `wrangler.toml` dia efa mamaritra:

- Worker name: `hound-online-relay`
- Worker entry file: `worker.js`
- Durable Object binding: `HOUND_RELAY`
- Durable Object class: `HoundRelay`

Cloudflare dia mitaky binding sy class declaration ho an’ny Durable Object, ary ny deployment dia atao amin’ny `npx wrangler deploy` [2].

### Fanandramana haingana

Mamoròna pairing token 16-character uppercase alphanumeric. Aza mampiasa espace na symbol:

```bash
python3 -c "import secrets,string; print(''.join(secrets.choice(string.ascii_uppercase+string.digits) for _ in range(16)))"
```

Ohatra: `Q7M2X9K4P8R1T6ZA`

Andramo ny Worker:

```bash
curl -i \
  -H "X-Pairing-Token: Q7M2X9K4P8R1T6ZA" \
  https://hound-online-relay.<subdomain>.workers.dev/api/status
```

Valiny andrasana voalohany:

```json
{"online":false,"last_seen":null,"device_label":null}
```

Raha `invalid_pairing_token` na HTTP `401` no mivoaka, jereo raha 16 characters marina ilay token. Raha `404` no mivoaka amin’ny URL fototra, ara-dalàna izany satria API path toy ny `/api/status` ihany no tohanan’ilay Worker.

## 5. Fampifandraisana GitHub amin’ny Cloudflare

Rehefa voasedra amin’ny Wrangler ilay Worker, ampifandraiso amin’ny GitHub izy mba hivoaka ho azy isaky ny manao push ianao:

1. Midira ao amin’ny Cloudflare Dashboard.
2. Sokafy **Workers & Pages**.
3. Safidio **Create application** na **Connect to Git**.
4. Safidio GitHub, ary omeo access amin’ilay repository `Hound-online-relay` ihany.
5. Safidio ilay repository.
6. Apetraho ny root directory ho:

```text
worker
```

7. Raha misy build command, avelao foana na ampiasao:

```bash
npm install
```

8. Apetraho ny deploy command:

```bash
npx wrangler deploy
```

9. Safidio ny branch `main`.
10. Alefaso ny deployment.

Rehefa misy push vaovao ao amin’ny `worker/`, Cloudflare dia afaka manao build sy deploy automatique. Ny GitHub integration ofisialin’i Cloudflare dia manohana automatic deployment sy build status ao amin’ny GitHub [3].

**Fanamarihana:** Raha tsy hitan’i Cloudflare ilay `wrangler.toml`, jereo aloha fa `worker` no **Root directory**, fa tsy ny repository root.

## 6. Fanamboarana ny Android APK

### Fanomanana Android Studio

1. Apetraho Android Studio.
2. Sokafy amin’ny Android Studio ilay folder:

```text
Hound-online-relay/android
```

3. Rehefa mangataka SDK dia apetraho:
   - Android SDK Platform 35.
   - Android SDK Build-Tools.
   - Android SDK Platform-Tools.
4. Jereo fa Java/JDK 17 no ampiasain’ny Gradle:
   - Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle.
   - Safidio JDK 17.
5. Avelao hifarana ny Gradle sync.

### Build soso-kevitra: aza embed-na ny token

Ny pairing token dia toy ny password. Raha ampidirina amin’ny `-PhoundPairingToken=...`, dia tafiditra ao amin’ny APK ary mety ho hita rehefa reverse-engineer-na ilay APK.

Aleo URL ihany no embed-na, ary ny token atao input ao amin’ny app:

```bash
cd Hound-online-relay/android
./gradlew assembleServerDebug assembleClientDebug \
  -PhoundRelayUrl=https://hound-online-relay.<subdomain>.workers.dev
```

Ao Windows:

```bat
gradlew.bat assembleServerDebug assembleClientDebug ^
  -PhoundRelayUrl=https://hound-online-relay.<subdomain>.workers.dev
```

APK vokarina:

```text
android/app/build/outputs/apk/server/debug/app-server-debug.apk
android/app/build/outputs/apk/client/debug/app-client-debug.apk
```

### Build miaraka amin’ny token raikitra, raha tena ilaina

Raha tianao ho efa ao anaty APK ilay token:

```bash
./gradlew assembleServerDebug assembleClientDebug \
  -PhoundRelayUrl=https://hound-online-relay.<subdomain>.workers.dev \
  -PhoundPairingToken=YOUR16CHARACTERTOKEN
```

Ampiasao token vaovao sy private. Aza apetraka ao amin’ny GitHub command, README public, GitHub Actions log, na screenshot.

## 7. Fametrahana sy fampiasana ny APK

1. Alefaso any amin’ny Android phone ny `app-server-debug.apk`.
2. Alefaso amin’ny phone iray na emulator ny `app-client-debug.apk`.
3. Omeo alalana hametraka APK avy amin’ny source ivelany raha ilaina.
4. Ao amin’ny Server app:
   - Sokafy ny Server tab.
   - Safidio ny folder tianao zaraina.
   - Omeo storage/file access.
   - Omeo notification permission.
   - Avelao hiasa ny foreground service.
   - Jereo ny pairing token asehon’ilay app.
5. Ao amin’ny Client app:
   - Ampidiro ilay Worker URL raha mbola tsy voapetraka.
   - Ampidiro ilay pairing token avy amin’ny Server.
   - Andramo alaina ny fichier kely aloha.

Ny Server dia tsy maintsy mandeha sy tsy voakaton’ny Android battery optimization. Raha mijanona ilay Server app/service, dia tsy ho vita ny download lavitra.

## 8. Raha tsy mandeha

### `401 invalid_pairing_token`

- Hamarino fa 16 characters ilay token.
- Ampiasao uppercase A–Z sy digits 0–9 ihany.
- Ataovy mitovy tanteraka amin’ny Server sy Client.

### `404 not_found`

- Hamarino fa ny URL dia Worker URL, ohatra:
  `https://hound-online-relay.example.workers.dev`
- Aza manampy `/api/status` ao amin’ny Android relay URL raha ilay app no manampy ny API path.

### `503 manifest_not_ready`

Midika izany fa mbola tsy nandefa manifest ny Server. Alefaso ny Server, safidio ny folder, ary andraso segondra vitsivitsy.

### APK tsy vita build

Hamarino:

```bash
java -version
adb version
```

Ny projet dia natao ho an’ny JDK 17 sy Android SDK 35. Raha miseho hoe `SDK location not found`, apetraho Android SDK ary amboary ny `ANDROID_HOME` na sokafy amin’ny Android Studio ilay projet.

### Server online fa tsy hita ny fichier

- Hamarino ny folder voafidy.
- Hamarino storage/file permission.
- Hamarino notification sy foreground service.
- Aza avela hamono ilay app ny battery saver.
- Hamarino fa mitovy Worker URL sy token ny apps roa.

## 9. Fiarovana ilaina

- Ataovy **Private** ny GitHub repository.
- Aza atao public ny pairing token.
- Zarao amin’ny olona kendrena ihany ilay token.
- Rehefa heverinao fa tafaporitsaka ilay token, mamoròna token vaovao ary rebuild-na/ampidiro indray ny apps.
- Zarao amin’ny Server ihany ny folder ilaina.
- Aza expose-na mivantana amin’ny Internet ny HTTP server local an’ilay app.
- Ampiasao HTTPS Worker URL (`https://`), fa aza HTTP.
- Mariho fa ny token dia bearer secret: izay mahazo azy dia afaka mangataka ny manifest sy fichiers amin’ilay relay.

## 10. Workflow fampiasana isan’andro

Rehefa efa napetraka indray mandeha:

```bash
# Fanovana Worker
cd Hound-online-relay/worker
git pull
npx wrangler deploy
git add .
git commit -m "Update relay"
git push origin main
```

Na, raha efa mandeha ny Cloudflare Git integration, ampy ny:

```bash
git add worker/
git commit -m "Update relay"
git push origin main
```

Ho an’ny fanovana Android:

```bash
cd Hound-online-relay/android
./gradlew assembleServerDebug assembleClientDebug \
  -PhoundRelayUrl=https://hound-online-relay.<subdomain>.workers.dev
```

## References

[1]: https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github "GitHub: Adding locally hosted code to GitHub"

[2]: https://developers.cloudflare.com/durable-objects/get-started/ "Cloudflare: Durable Objects getting started"

[3]: https://developers.cloudflare.com/workers/ci-cd/builds/git-integration/github-integration/ "Cloudflare: GitHub integration for Workers"
