package com.example

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HabitRecord::class, CustomHabit::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun customHabitDao(): CustomHabitDao
}
