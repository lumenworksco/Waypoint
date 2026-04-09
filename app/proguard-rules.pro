# Gson
-keepattributes Signature
-keep class com.florian.waypoint.Waypoint { *; }
-keep class com.florian.waypoint.Track { *; }
-keep class com.florian.waypoint.TrackPoint { *; }
-keep class com.florian.waypoint.GpxData { *; }

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Play Services Location
-keep class com.google.android.gms.location.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
