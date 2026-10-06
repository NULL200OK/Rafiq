package com.rafiq.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "") val emotion: String = ""
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_RAFIQ = "rafiq"
    }
}