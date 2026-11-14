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
