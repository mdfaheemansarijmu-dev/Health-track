package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_records")
data class HabitRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val count: Int = 0,
    val durationMinutes: Int = 0
)
