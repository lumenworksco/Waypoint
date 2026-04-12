# Gson
-keepattributes Signature
-keep class com.florian.piste.Waypoint { *; }
-keep class com.florian.piste.Track { *; }
-keep class com.florian.piste.TrackPoint { *; }
-keep class com.florian.piste.GpxData { *; }
-keep class com.florian.piste.TrackStatistics { *; }

# Enums used in settings
-keep enum com.florian.piste.DistanceUnit { *; }
-keep enum com.florian.piste.MapStyle { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Play Services Location
-keep class com.google.android.gms.location.** { *; }
