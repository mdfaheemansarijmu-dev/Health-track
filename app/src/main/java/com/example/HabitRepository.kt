package com.example

class HabitRepository(private val habitDao: HabitDao, private val customHabitDao: CustomHabitDao) {
    val allRecords = habitDao.getAllRecords()
    val allCustomHabits = customHabitDao.getAllHabits()
    suspend fun insert(record: HabitRecord) = habitDao.insertRecord(record)
    suspend fun delete(record: HabitRecord) = habitDao.deleteRecord(record)
    suspend fun deleteAll() = habitDao.deleteAllRecords()
    suspend fun insertCustomHabit(habit: CustomHabit) = customHabitDao.insertHabit(habit)
    suspend fun deleteCustomHabit(habitId: Int) = customHabitDao.deleteHabitById(habitId)
}
