package com.first_project.chronoai.data.local.dao

import androidx.room.*
import com.first_project.chronoai.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insertTask(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
}
