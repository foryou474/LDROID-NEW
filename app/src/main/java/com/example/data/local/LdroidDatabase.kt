package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        FileOpHistoryEntity::class,
        WorkspaceSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LdroidDatabase : RoomDatabase() {
    abstract fun ldroidDao(): LdroidDao

    companion object {
        @Volatile
        private var INSTANCE: LdroidDatabase? = null

        fun getInstance(context: Context): LdroidDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LdroidDatabase::class.java,
                    "ldroid_workspace.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
