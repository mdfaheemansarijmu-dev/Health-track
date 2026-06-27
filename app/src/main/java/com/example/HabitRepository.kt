package com.example

class HabitRepository(private val habitDao: HabitDao) {
    val allRecords = habitDao.getAllRecords()
    suspend fun insert(record: HabitRecord) = habitDao.insertRecord(record)
    suspend fun delete(record: HabitRecord) = habitDao.deleteRecord(record)
    suspend fun deleteAll() = habitDao.deleteAllRecords()
}
