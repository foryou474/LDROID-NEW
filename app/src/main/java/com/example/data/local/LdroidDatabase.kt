package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        FileOpHistoryEntity::class,
        WorkspaceSnapshotEntity::class,
        AiMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LdroidDatabase : RoomDatabase() {
    abstract fun ldroidDao(): LdroidDao

    companion object {
        @Volatile
        private var INSTANCE: LdroidDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_chat_messages` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `projectId` TEXT NOT NULL,
                        `isUser` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `timestampMs` INTEGER NOT NULL,
                        `isError` INTEGER NOT NULL,
                        `proposedChangesJson` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): LdroidDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LdroidDatabase::class.java,
                    "ldroid_workspace.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
