package com.rafiq.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "response_memory")
data class ResponseMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val answer: String,
    val createdAt: Long
)

@Dao
interface ResponseMemoryDao {
    @Insert suspend fun insert(entry: ResponseMemoryEntity)

    @Query("SELECT * FROM response_memory ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ResponseMemoryEntity>

    @Query("DELETE FROM response_memory WHERE id NOT IN (SELECT id FROM response_memory ORDER BY id DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}