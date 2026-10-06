package com.rafiq.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_memory")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val content: String,
    val createdAt: Long
)

@Dao
interface UserMemoryDao {
    @Insert suspend fun insert(memory: UserMemoryEntity)

    @Query("SELECT * FROM user_memory ORDER BY id DESC")
    fun observeAll(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memory ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<UserMemoryEntity>

    @Query("DELETE FROM user_memory WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM user_memory")
    suspend fun clearAll()

    @Query("DELETE FROM user_memory WHERE id NOT IN (SELECT id FROM user_memory ORDER BY id DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}