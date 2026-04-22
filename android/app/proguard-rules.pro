# FanHub ProGuard rules
-keep class com.fanhub.app.data.model.** { *; }
-keepclassmembers class com.fanhub.app.data.model.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
