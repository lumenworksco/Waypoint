# Gson
-keepattributes Signature
-keep class com.florian.waypoint.Waypoint { *; }
-keep class com.florian.waypoint.Track { *; }
-keep class com.florian.waypoint.TrackPoint { *; }
-keep class com.florian.waypoint.GpxData { *; }
-keep class com.florian.waypoint.TrackStatistics { *; }

# Enums used in settings
-keep enum com.florian.waypoint.DistanceUnit { *; }
-keep enum com.florian.waypoint.MapStyle { *; }
-keep enum com.florian.waypoint.CoordFormat { *; }

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Play Services Location
-keep class com.google.android.gms.location.** { *; }
