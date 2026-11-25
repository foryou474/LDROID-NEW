package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LdroidDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMs DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("UPDATE projects SET updatedAtMs = :updatedAtMs, lastOpenedFile = :lastFile WHERE id = :id")
    suspend fun touchProject(id: String, updatedAtMs: Long, lastFile: String?)

    @Query("SELECT * FROM file_op_history WHERE projectId = :projectId ORDER BY timestampMs DESC LIMIT 50")
    fun observeHistory(projectId: String): Flow<List<FileOpHistoryEntity>>

    @Insert
    suspend fun insertHistory(entry: FileOpHistoryEntity)

    @Query("DELETE FROM file_op_history WHERE projectId = :projectId")
    suspend fun deleteHistoryForProject(projectId: String)

    @Query("SELECT * FROM workspace_snapshots WHERE projectId = :projectId ORDER BY createdAtMs DESC")
    fun observeSnapshots(projectId: String): Flow<List<WorkspaceSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: WorkspaceSnapshotEntity)
}
