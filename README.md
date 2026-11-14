# TRAZO Android

Native Kotlin + Jetpack Compose client for the TRAZO strength-training platform. The ASP.NET Core API and Angular administration application live in [jl-hita/Trazo](https://github.com/jl-hita/Trazo).

## Current capabilities

- Email/password registration and login.
- Email verification and password-recovery deep links.
- Google Sign-In using Credential Manager and a Google Web OAuth client ID.
- TOTP two-factor enrollment, challenge verification, recovery codes and disable flow.
- Access and refresh tokens protected with Android Keystore.
- Refresh-token rotation, authenticated current-user lookup and server-side logout.
- Training plans, weekly schedule and workout sessions.
- Set logging, rest timer, exercise substitution and exercise catalog.
- Workout history, local drafts and pending workout records.
- Account and subscription screens.
- Manual plan-review requests and conversations.

## Requirements

- JDK 17
- Android SDK 36
- Gradle 8.13 (use the Gradle wrapper where available)
- A reachable TRAZO API for authenticated and server-backed flows

## API endpoint configuration

The API endpoint is configurable without source changes. The debug default is `http://10.0.2.2:5001/` (Android emulator to the development computer).

Configure scheme, host and port separately:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PapiScheme=https \
  -PapiHost=jlhitap.duckdns.org \
  -PapiPort=443
```

For a non-standard port, set e.g. `-PapiPort=8443`. For local/private LAN testing over HTTP, use `-PapiScheme=http -PapiHost=192.168.1.20 -PapiPort=5001`. Cleartext HTTP is allowed only in debug builds. Release builds require HTTPS.

You can also configure a path prefix using `-PapiBasePath=/trazo-api/`, if the reverse proxy routes the API below a path. The path must be supported by the server's proxy/path-base configuration.

Alternatively pass the complete URL with `-PapiBaseUrl=https://jlhitap.duckdns.org:443/`; this takes precedence over scheme/host/port. Include the trailing slash.

**Release builds must be configured with the real HTTPS API endpoint.** Do not publish a build using the placeholder/default endpoint.

## Google Sign-In configuration

Create a Web application OAuth client and configure its ID as `Authentication__Google__ClientId` on the API and as `googleWebClientId` in the Android build:

```bash
./gradlew --no-daemon :app:assembleDebug \
  -PapiScheme=https \
  -PapiHost=jlhitap.duckdns.org \
  -PapiPort=443 \
  -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Also create an Android OAuth client for package `com.trazo.android` with the SHA-1 fingerprint of the certificate used to sign the installed APK. Never embed a Google client secret in Android.

## Authentication and local data

Access and refresh tokens are encrypted using Android Keystore-backed storage. The app also keeps local workout data, drafts and pending records so an interrupted or offline workout is not immediately lost.

**Important:** local workout/cache files are currently stored in the app's private storage as JSON and are not encrypted at rest by the same mechanism as the tokens. Do not interpret token encryption as encryption of all locally stored user data. Review this limitation before using the app with sensitive personal data.

Pending workout records are retried when the relevant app flows request synchronization. This is not yet a guaranteed WorkManager-based background synchronization service; users should allow the app to reconnect and synchronize while it is active.

## Build and tests

Build the debug APK:

```bash
./gradlew --no-daemon :app:assembleDebug
```

Run local unit tests:

```bash
./gradlew --no-daemon :app:testDebugUnitTest
```

GitHub Actions runs the unit-test task and assembles the debug application. These checks do not constitute a full instrumented end-to-end test on a physical device.

## Release and secrets

See [docs/release.md](docs/release.md) and [docs/configuration.md](docs/configuration.md) for signing, release configuration, API settings, emulator/physical-device setup and deep links.

No API credentials, Google client secrets, SMTP credentials or signing secrets should be committed. Keep signing material and private credentials in local secure storage or the CI secret store.
