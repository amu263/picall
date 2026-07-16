# Keep Picall specific classes
-keep class com.picall.app.** { *; }
-keepclassmembers class com.picall.app.data.model.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
