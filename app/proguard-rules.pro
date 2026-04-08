# Gson
-keepattributes Signature
-keep class com.florian.waypoint.Waypoint { *; }

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Play Services Location
-keep class com.google.android.gms.location.** { *; }
