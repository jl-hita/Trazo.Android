# TRAZO Android

Native Kotlin + Jetpack Compose client for the TRAZO strength-training platform. The API and Angular administration app live in jl-hita/Trazo.

## Authentication

- Email/password registration and login.
- Email verification and password recovery deep links.
- Google Sign-In using Credential Manager and a Google Web OAuth client ID.
- TOTP two-factor enrollment, challenge verification, recovery codes and disable flow.
- Access and refresh tokens encrypted with Android Keystore.
- Refresh-token rotation on app startup, authenticated current-user lookup and server-side logout.

## API endpoint configuration

The API endpoint is configurable without source changes. The debug default is `http://10.0.2.2:5001/` (Android emulator to the development computer).

Configure scheme, host and port separately:

```bash
gradle --no-daemon :app:assembleDebug \
  -PapiScheme=https \
  -PapiHost=jlhitap.duckdns.org \
  -PapiPort=443
```

For a non-standard port, set e.g. `-PapiPort=8443`. For local/private LAN testing over HTTP, use `-PapiScheme=http -PapiHost=192.168.1.20 -PapiPort=5001`. Cleartext HTTP is allowed only in debug builds. Release builds require HTTPS.

You can also configure a path prefix using `-PapiBasePath=/trazo-api/`, if the reverse proxy routes the API below a path. The path must be supported by the server's proxy/path-base configuration.

Alternatively pass the complete URL with `-PapiBaseUrl=https://jlhitap.duckdns.org:443/`; this takes precedence over scheme/host/port. Include the trailing slash.

Google Sign-In can be configured at the same time:

```bash
gradle --no-daemon :app:assembleDebug \
  -PapiScheme=https \
  -PapiHost=jlhitap.duckdns.org \
  -PapiPort=443 \
  -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Never use emulator-only `10.0.2.2` on a physical device.

## Google Sign-In configuration

Create a Web application OAuth client and configure its ID as `Authentication__Google__ClientId` on the API and as `googleWebClientId` in the Android build. Also create an Android OAuth client for package `com.trazo.android` with the SHA-1 fingerprint of the certificate used to sign the installed APK. Never embed a Google client secret in Android.

## Build

Requires JDK 17, Android SDK 36 and Gradle 8.13. Run `gradle --no-daemon :app:assembleDebug`.

No API credentials, Google client secrets, SMTP credentials or signing secrets are committed.
