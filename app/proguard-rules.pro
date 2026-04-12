# Gson — keep data classes serialized with Gson
-keepattributes Signature
-keep class com.florian.piste.Waypoint { *; }
-keep class com.florian.piste.Track { *; }
-keep class com.florian.piste.TrackPoint { *; }
-keep class com.florian.piste.GpxData { *; }
-keep class com.florian.piste.TrackStatistics { *; }
-keep class com.florian.piste.PersonalBests { *; }

# Enums used in settings / Gson
-keep enum com.florian.piste.DistanceUnit { *; }
-keep enum com.florian.piste.MapStyle { *; }

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Play Services Location
-keep class com.google.android.gms.location.** { *; }

# Coil — keep image loading internals
-dontwarn io.coil.**
