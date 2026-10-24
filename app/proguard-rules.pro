# ProGuard rules for TirePressure
-keepattributes *Annotation*,InnerClasses,Signature

# Strip all Android Logging in Release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Android 15 / 16 Edge-to-Edge compliance:
# Strip legacy deprecated Window bar color setters and API 28 SHORT_EDGES cutout mode
# invoked by AndroidX backward-compatibility shims so Play Console does not flag them.
-assumenosideeffects class android.view.Window {
    public void setStatusBarColor(int);
    public void setNavigationBarColor(int);
}
-assumenosideeffects class * {
    void adjustLayoutInDisplayCutoutMode(android.view.Window);
}

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}

# Keep Data Models, Entities, and Enums
-keep class com.tirepressure.app.domain.model.** { *; }
-keep class com.tirepressure.app.core.model.** { *; }
-keep class com.tirepressure.app.data.local.entity.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
