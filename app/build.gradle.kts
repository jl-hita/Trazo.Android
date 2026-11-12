plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
val configuredApiUrl = providers.gradleProperty("apiBaseUrl")
val debugApiUrl = configuredApiUrl.orElse("http://10.0.2.2:5000/").get().let { if (it.endsWith("/")) it else "$it/" }
val releaseApiUrl = configuredApiUrl.orElse("https://api.example.invalid/").get().let { if (it.endsWith("/")) it else "$it/" }
android {
 namespace = "com.trazo.android"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.trazo.android"
  minSdk = 26
  targetSdk = 36
  versionCode = 1
  versionName = "0.1.0"
  val googleClientId = providers.gradleProperty("googleWebClientId").orNull ?: ""
  buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
  buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
 }
 buildTypes { debug { buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"") } }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.10.00"))
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.credentials:credentials:1.6.0")
 implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
 implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
 implementation("com.squareup.retrofit2:retrofit:2.11.0")
 implementation("com.squareup.retrofit2:converter-gson:2.11.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
