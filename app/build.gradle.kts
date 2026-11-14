import java.util.Properties

plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
val explicitApiBaseUrl = providers.gradleProperty("apiBaseUrl").orNull?.let { if (it.endsWith("/")) it else "$it/" }
val configuredApiHost = providers.gradleProperty("apiHost").orNull
val apiScheme = providers.gradleProperty("apiScheme").orElse("http").get()
val apiHost = configuredApiHost ?: "10.0.2.2"
val apiPort = providers.gradleProperty("apiPort").orElse("5001").get()
val apiBasePath = providers.gradleProperty("apiBasePath").orElse("/").get().let {
 if (it.startsWith("/")) it else "/$it"
}.let { if (it.endsWith("/")) it else "$it/" }
val composedApiUrl = "$apiScheme://$apiHost:$apiPort$apiBasePath"
val debugApiUrl = explicitApiBaseUrl ?: composedApiUrl
val hasExplicitReleaseEndpoint = configuredApiHost != null ||
 providers.gradleProperty("apiPort").isPresent ||
 providers.gradleProperty("apiScheme").isPresent

// Release signing and build numbers come from local.properties, Gradle properties or the environment: never from the repo.
val localProps = Properties().apply {
 val file = rootProject.file("local.properties")
 if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(name: String, env: String): String? =
 localProps.getProperty(name) ?: providers.gradleProperty(name).orNull ?: System.getenv(env)
val signingKeystorePath = secret("trazoSigningKeystorePath", "TRAZO_SIGNING_KEYSTORE_PATH")
val signingKeystorePassword = secret("trazoSigningKeystorePassword", "TRAZO_SIGNING_KEYSTORE_PASSWORD")
val signingKeyAlias = secret("trazoSigningKeyAlias", "TRAZO_SIGNING_KEY_ALIAS")
val signingKeyPassword = secret("trazoSigningKeyPassword", "TRAZO_SIGNING_KEY_PASSWORD")
val hasReleaseSigning = !signingKeystorePath.isNullOrBlank() && rootProject.file(signingKeystorePath).exists() &&
 !signingKeystorePassword.isNullOrBlank() && !signingKeyAlias.isNullOrBlank() && !signingKeyPassword.isNullOrBlank()

// versionCode = major*1_000_000 + minor*10_000 + patch*100 + buildNumber, so it only ever grows with versionName.
val appVersionName = "0.2.0"
val buildNumber = (providers.gradleProperty("buildNumber").orNull ?: System.getenv("TRAZO_BUILD_NUMBER") ?: "0").toInt()
require(buildNumber in 0..99) { "buildNumber debe estar entre 0 y 99." }
val appVersionCode = appVersionName.split(".").map { it.toInt() }.let { (ma, mi, pa) -> ma * 1_000_000 + mi * 10_000 + pa * 100 } + buildNumber
// Subscription product used by the (inactive) Play Billing flow. Change it here or with -PbillingProductId=...
val billingProductId = providers.gradleProperty("billingProductId").orElse("trazo_premium").get()
// FLAG_SECURE on the routine screens; can only be switched off in debug builds with -PsecureScreens=false.
val secureScreensInDebug = providers.gradleProperty("secureScreens").orElse("true").get().toBoolean()
val releaseApiUrl = explicitApiBaseUrl ?: if (hasExplicitReleaseEndpoint) composedApiUrl else "https://api.example.invalid/"
android {
 namespace = "com.trazo.android"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.trazo.android"
  minSdk = 26
  targetSdk = 36
  versionCode = appVersionCode
  versionName = appVersionName
  val googleClientId = providers.gradleProperty("googleWebClientId").orNull ?: ""
  buildConfigField("String", "API_BASE_URL", "\"$releaseApiUrl\"")
  buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
  buildConfigField("String", "BILLING_PRODUCT_ID", "\"$billingProductId\"")
  buildConfigField("boolean", "SECURE_SCREENS", "true")
 }
 signingConfigs {
  if (hasReleaseSigning) create("release") {
   storeFile = rootProject.file(signingKeystorePath!!)
   storePassword = signingKeystorePassword
   keyAlias = signingKeyAlias
   keyPassword = signingKeyPassword
  }
 }
 buildTypes {
  debug {
   buildConfigField("String", "API_BASE_URL", "\"$debugApiUrl\"")
   buildConfigField("boolean", "SECURE_SCREENS", "$secureScreensInDebug")
  }
  release {
   isMinifyEnabled = true
   isShrinkResources = true
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
   if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
  }
 }
 testOptions { unitTests.isReturnDefaultValues = true }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
// Kotlin must target the same JVM as Java regardless of the JDK running Gradle (Android Studio ships 21).
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.10.00"))
 implementation("androidx.activity:activity-compose:1.11.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-core")
 implementation("androidx.credentials:credentials:1.6.0")
 implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
 implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
 implementation("com.squareup.retrofit2:retrofit:2.11.0")
 implementation("com.squareup.retrofit2:converter-gson:2.11.0")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 implementation("io.coil-kt:coil-compose:2.7.0")
 implementation("androidx.navigation:navigation-compose:2.9.6")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 implementation("com.android.billingclient:billing-ktx:9.1.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
 testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
