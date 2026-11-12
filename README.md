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

The debug build defaults to http://10.0.2.2:5000/ (the host computer's localhost from the Android emulator). Start the API on the development computer at port 5000, or override the URL when building:

gradle --no-daemon :app:assembleDebug -PapiBaseUrl=https://YOUR_TEST_API_HOST/

For a physical device, use a reachable LAN address or HTTPS host. Debug builds allow cleartext HTTP for local development only. Release builds require HTTPS. The URL must end with a slash.

## Google Sign-In configuration

Create a Web application OAuth client in Google Cloud Console and pass its client ID:

gradle --no-daemon :app:assembleDebug -PapiBaseUrl=https://YOUR_TEST_API_HOST/ -PgoogleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com

Configure the same value on the API as Authentication__Google__ClientId. Also create an Android OAuth client for package com.trazo.android with the SHA-1 fingerprint of the signing certificate used to install the app. Never embed a Google client secret in Android.

## Build

Requires JDK 17, Android SDK 36 and Gradle 8.13. Run gradle --no-daemon :app:assembleDebug.

No API credentials, Google client secrets, SMTP credentials or signing secrets are committed.
