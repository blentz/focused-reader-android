# ProGuard / R8 rules for Focused Reader release builds.
#
# Verified against `./gradlew :app:bundleRelease` with R8 in full mode.

# ---- Project ----
# Keep ImportSource enum (DataStore reads it by name).
-keepclassmembers enum com.focusedreader.** { *; }

# ---- Kotlin coroutines ----
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.atomicfu.**

# ---- Jetpack Compose (BOM picks the right versions; we just keep stability) ----
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-dontwarn dagger.hilt.android.internal.**

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ---- Jsoup (uses reflection for Node subclass discovery) ----
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ---- DataStore Preferences ----
-dontwarn androidx.datastore.preferences.**

# ---- AndroidX / system ----
-dontwarn java.beans.**
-dontwarn java.awt.**

# ---- Strip release log spam (also covered by Task #8) ----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
