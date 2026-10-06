package com.rafiq.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert suspend fun insert(message: MessageEntity): Long

    @Query("SELECT * FROM messages ORDER BY id ASC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT 1")
    suspend fun lastMessage(): MessageEntity?

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun count(): Int

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}