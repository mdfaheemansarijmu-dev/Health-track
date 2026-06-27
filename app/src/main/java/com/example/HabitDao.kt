package com.example

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HabitRecord>>

    @Insert
    suspend fun insertRecord(record: HabitRecord)

    @Delete
    suspend fun deleteRecord(record: HabitRecord)

    @Query("DELETE FROM habit_records")
    suspend fun deleteAllRecords()
}
