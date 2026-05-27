package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val type: String, // "text", "voice", "image"
    val voicePath: String? = null,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val soundOption: Int = 0, // 0: Cyber Buzz, 1: Digital Beat, 2: Space Chime
    val label: String = "Alarm"
)

@Entity(tableName = "game_scores")
data class GameScore(
    @PrimaryKey val gameId: String, // "racing", "fruit", "archery", "cricket"
    val highScore: Int
)

@Dao
interface AppDao {
    // --- NOTES ---
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    // --- ALARMS ---
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    // --- GAME SCORES ---
    @Query("SELECT * FROM game_scores WHERE gameId = :gameId")
    suspend fun getScoreForGame(gameId: String): GameScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: GameScore)
}

@Database(entities = [Note::class, Alarm::class, GameScore::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val appDao: AppDao
}
