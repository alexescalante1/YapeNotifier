# ── Stack traces legibles ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Hilt ──
-keep,allowobfuscation,allowshrinking class * extends dagger.hilt.android.internal.**
-keepclassmembers class * {
    @dagger.hilt.android.EarlyEntryPoint *;
}

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ── Glance (widget) ──
-keep class androidx.glance.** { *; }
-keep class com.example.yapenotifier.widget.** { *; }

# ── NotificationListenerService ──
-keep class com.example.yapenotifier.data.service.NotificationMonitorService { *; }

# ── KeepAliveService (foreground service) ──
-keep class com.example.yapenotifier.data.service.KeepAliveService { *; }

# ── BootReceiver ──
-keep class com.example.yapenotifier.receiver.BootReceiver { *; }

# ── DataStore Preferences ──
-keepclassmembers class androidx.datastore.preferences.** { *; }

# ── Kotlin coroutines ──
-dontwarn kotlinx.coroutines.**

# ── Compose ──
-dontwarn androidx.compose.**
