package com.first_project.chronoai.data.local.dao

import androidx.room.*
import com.first_project.chronoai.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deadline LIKE :datePrefix || '%' OR (deadline IS NULL AND :isToday = 1) ORDER BY priority DESC")
    fun getTasksForDate(datePrefix: String, isToday: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deadline LIKE :datePrefix || '%' OR (deadline IS NULL AND :isToday = 1) ORDER BY priority DESC LIMIT :limit")
    fun getTasksForDateWithLimit(datePrefix: String, isToday: Int, limit: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    suspend fun getAllTasksDirect(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Int): TaskEntity?

    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
}
