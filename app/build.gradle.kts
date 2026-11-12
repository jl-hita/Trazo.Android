plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.trazo.android"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.trazo.android"
  minSdk = 26
  targetSdk = 36
  versionCode = 1
  versionName = "0.1.0"
  val apiBaseUrl = providers.gradleProperty("apiBaseUrl").orElse("https://api.example.invalid/").get().let { if (it.endsWith("/")) it else "$it/" }
  val googleClientId = providers.gradleProperty("googleWebClientId").orNull ?: ""
  buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
  buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
 }
 buildTypes {
  debug { buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5000/\"") }
 }
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
 implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
 implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
 implementation("com.squareup.retrofit2:retrofit:2.11.0")
 implementation("com.squareup.retrofit2:converter-gson:2.11.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
