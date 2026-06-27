package com.example

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HabitRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}
