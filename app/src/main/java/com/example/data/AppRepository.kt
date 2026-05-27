package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val dao = db.appDao

    val allNotes: Flow<List<Note>> = dao.getAllNotes()
    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()

    suspend fun insertNote(note: Note) = dao.insertNote(note)
    suspend fun updateNote(note: Note) = dao.updateNote(note)
    suspend fun deleteNote(note: Note) = dao.deleteNote(note)
    suspend fun getNoteById(id: Int) = dao.getNoteById(id)

    suspend fun insertAlarm(alarm: Alarm) = dao.insertAlarm(alarm)
    suspend fun updateAlarm(alarm: Alarm) = dao.updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: Alarm) = dao.deleteAlarm(alarm)

    suspend fun getHighScore(gameId: String): Int {
        return dao.getScoreForGame(gameId)?.highScore ?: 0
    }

    suspend fun updateHighScore(gameId: String, score: Int) {
        val current = getHighScore(gameId)
        if (score > current) {
            dao.insertScore(GameScore(gameId, score))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "multitask_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
