package com.rafiq.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MessageEntity::class, ResponseMemoryEntity::class, UserMemoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RafiqDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): ResponseMemoryDao
    abstract fun userMemoryDao(): UserMemoryDao

    companion object {
        @Volatile private var instance: RafiqDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_memory (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "category TEXT NOT NULL, " +
                            "content TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE messages ADD COLUMN emotion TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): RafiqDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RafiqDatabase::class.java,
                    "rafiq.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}