# Step 18: prep file for when minification gets enabled (likely Step 19/20
# release build). isMinifyEnabled is still false in app/build.gradle.kts —
# turning it on without testing rules first risks breaking Retrofit/Gson
# reflection at runtime with no error until you hit that code path.

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson — keep data classes used for JSON (de)serialization intact
-keepclassmembers class com.sudhanshu.tva.network.** { *; }
-keepclassmembers class com.sudhanshu.tva.data.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
