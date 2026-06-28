package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_habits")
data class CustomHabit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = ""
)
