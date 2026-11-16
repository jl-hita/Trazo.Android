# Gson reads and writes the API contracts by reflection: keep their fields and constructors.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class com.trazo.android.data.model.** { *; }
-keep class com.trazo.android.auth.** { *; }
-keep class com.trazo.android.data.local.Pending** { *; }
-keep class com.trazo.android.data.local.Cached** { *; }
# Retrofit service interfaces are used through proxies.
-keep,allowobfuscation interface com.trazo.android.data.api.*Api
-keep,allowobfuscation interface com.trazo.android.auth.AuthApi
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# Gson TypeToken subclasses (new TypeToken<List<X>>() {}) need their generic superclass signature at runtime.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
# Types the app (de)serializes outside the packages above.
-keep class com.trazo.android.diagnostics.ErrorReportBody { *; }
# Push API bodies and the config answer: without this R8 renames/folds their fields and the app crashes (PushClientConfig.usable).
-keep class com.trazo.android.push.PushClientConfig { *; }
-keep class com.trazo.android.push.RegisterDeviceBody { *; }
-keep class com.trazo.android.push.UnregisterDeviceBody { *; }
