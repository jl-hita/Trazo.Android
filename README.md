# TRAZO Android

Native Kotlin + Jetpack Compose client for the TRAZO strength-training platform.

The API and Angular administration app live in jl-hita/Trazo.

## Current foundation
- Android app shell using Material 3.
- Retrofit authentication client for email/password, Google ID tokens and TOTP challenges.
- Session tokens encrypted with an AES-GCM key held by Android Keystore.
- GitHub Actions debug build on pull requests.

## Local build
Requires JDK 17, Android SDK 36 and Gradle 8.13. Run: gradle :app:assembleDebug

Set Gradle property googleWebClientId to the Google OAuth Web client ID. The API base URL is currently a deliberately invalid placeholder and must be replaced for each deployment environment.

Email verification and password recovery are being added to the backend; public registration must remain closed until verification is enforced server-side.
