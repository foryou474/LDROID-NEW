package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val projectType: String,
    val rootPath: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val lastOpenedFile: String? = null
)

@Entity(tableName = "file_op_history")
data class FileOpHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val operation: String,
    val targetPath: String,
    val details: String,
    val timestampMs: Long
)

@Entity(tableName = "workspace_snapshots")
data class WorkspaceSnapshotEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val label: String,
    val fileCount: Int,
    val totalBytes: Long,
    val createdAtMs: Long
)

@Entity(tableName = "ai_chat_messages")
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val isUser: Boolean,
    val text: String,
    val timestampMs: Long,
    val isError: Boolean = false,
    val proposedChangesJson: String = "[]"
)
