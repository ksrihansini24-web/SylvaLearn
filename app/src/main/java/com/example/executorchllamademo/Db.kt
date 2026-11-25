package com.example.executorchllamademo

import android.content.Context
import androidx.room.*

@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val examName: String,
    val examDate: String,
    val hoursPerDay: Float,
    val level: String,
    val weakSubjects: String,
    val language: String
)

@Entity(tableName = "progress")
data class Progress(
    @PrimaryKey val subject: String,
    val attempted: Int,
    val correct: Int
)

@Entity(tableName = "chat_history")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(p: Profile)

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(p: Progress)

    @Query("SELECT * FROM progress")
    suspend fun getAllProgress(): List<Progress>

    @Query("SELECT * FROM progress WHERE subject = :s")
    suspend fun getProgress(s: String): Progress?

    @Insert
    suspend fun insertMessage(msg: ChatMessage)

    @Query("SELECT * FROM chat_history ORDER BY id ASC")
    suspend fun getAllMessages(): List<ChatMessage>

    @Query("SELECT * FROM (SELECT * FROM chat_history ORDER BY id DESC LIMIT 6) ORDER BY id ASC")
    suspend fun getRecentMessages(): List<ChatMessage>

    @Query("DELETE FROM chat_history")
    suspend fun clearChatHistory()
}

@Database(entities = [Profile::class, Progress::class, ChatMessage::class], version = 6, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "sylva.db")
                .fallbackToDestructiveMigration()
                .build().also { inst = it }
        }
    }
}
