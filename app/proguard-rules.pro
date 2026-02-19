# TrailRide Tracker ProGuard Rules

# Room entities
-keep class com.trailride.tracker.data.db.entity.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Health Connect
-keep class androidx.health.connect.** { *; }
