package com.example

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomHabitDao {
    @Query("SELECT * FROM custom_habits")
    fun getAllHabits(): Flow<List<CustomHabit>>

    @Insert
    suspend fun insertHabit(habit: CustomHabit)

    @Query("DELETE FROM custom_habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Int)
}
