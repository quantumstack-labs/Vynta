package com.first_project.chronoai.data.local.dao

import androidx.room.*
import com.first_project.chronoai.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
    
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): HabitEntity?
}
