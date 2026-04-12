package com.florian.piste

import android.content.Context
import androidx.room.*

// Room entities
@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val color: String = "#3C3734",
    val photoPath: String? = null,
    val icon: String? = null
)

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val color: String = "#007AFF",
    val biggestAirMs: Long = 0,
    val airCount: Int = 0
)

@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(
        entity = TrackEntity::class,
        parentColumns = ["id"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trackId")]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val trackId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val altitude: Double? = null,
    val sortOrder: Int = 0
)

// DAOs
@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints ORDER BY timestamp DESC")
    suspend fun getAll(): List<WaypointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: WaypointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(waypoints: List<WaypointEntity>)

    @Update
    suspend fun update(waypoint: WaypointEntity)

    @Delete
    suspend fun delete(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getById(id: String): WaypointEntity?

    @Query("SELECT COUNT(*) FROM waypoints")
    suspend fun count(): Int

    @Query("DELETE FROM waypoints")
    suspend fun deleteAll()
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    suspend fun getAll(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Update
    suspend fun update(track: TrackEntity)

    @Delete
    suspend fun delete(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: String): TrackEntity?

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}

@Dao
interface TrackPointDao {
    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY sortOrder")
    suspend fun getForTrack(trackId: String): List<TrackPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deleteForTrack(trackId: String)
}

// Database
@Database(
    entities = [WaypointEntity::class, TrackEntity::class, TrackPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PisteDatabase : RoomDatabase() {
    abstract fun waypointDao(): WaypointDao
    abstract fun trackDao(): TrackDao
    abstract fun trackPointDao(): TrackPointDao

    companion object {
        @Volatile
        private var INSTANCE: PisteDatabase? = null

        fun getInstance(context: Context): PisteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PisteDatabase::class.java,
                    "piste_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

// Converters between domain models and Room entities
fun Waypoint.toEntity() = WaypointEntity(
    id = id, name = name, latitude = latitude, longitude = longitude,
    notes = notes, timestamp = timestamp, color = color,
    photoPath = photoPath, icon = icon
)

fun WaypointEntity.toDomain() = Waypoint(
    id = id, name = name, latitude = latitude, longitude = longitude,
    notes = notes, timestamp = timestamp, color = color,
    photoPath = photoPath, icon = icon
)

fun Track.toEntity() = TrackEntity(
    id = id, name = name, startTime = startTime, endTime = endTime,
    color = color, biggestAirMs = biggestAirMs, airCount = airCount
)

fun TrackEntity.toDomain(points: List<TrackPoint>) = Track(
    id = id, name = name, points = points, startTime = startTime,
    endTime = endTime, color = color, biggestAirMs = biggestAirMs,
    airCount = airCount
)

fun TrackPoint.toEntity(trackId: String, order: Int) = TrackPointEntity(
    trackId = trackId, latitude = latitude, longitude = longitude,
    timestamp = timestamp, altitude = altitude, sortOrder = order
)

fun TrackPointEntity.toDomain() = TrackPoint(
    latitude = latitude, longitude = longitude,
    timestamp = timestamp, altitude = altitude
)
