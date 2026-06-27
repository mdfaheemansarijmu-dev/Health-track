package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {
    val uiState: StateFlow<List<HabitRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Configuration & Themes
    var selectedTab by mutableStateOf(0)
    var isDarkMode by mutableStateOf(true) // Start in premium dark mode by default, matches Mockups 4 & 5
    var accentColorName by mutableStateOf("Emerald") // Matches Mockup Settings
    var isFingerprintEnabled by mutableStateOf(true)
    var isPinLockEnabled by mutableStateOf(true)
    var dailyReminderTime by mutableStateOf("9:00 PM")
    var reflectionReminderTime by mutableStateOf("10:00 PM")

    // Daily Reflection Form States
    var selectedMoodIndex by mutableStateOf(-1) // 0 to 6 for 😄, 😊, 😐, 😟, 😡, 😴, 😢
    var journalNotes by mutableStateOf("")

    // Form expanded states
    var masturbationExpanded by mutableStateOf(true)
    var pornExpanded by mutableStateOf(false)
    var thoughtsExpanded by mutableStateOf(false)
    var urgeExpanded by mutableStateOf(false)

    // Form answers & sub-selections
    var masturbationSlipped by mutableStateOf("Yes") // Default matching Screen 2
    var masturbationCount by mutableStateOf(1)
    var masturbationTriggers by mutableStateOf(listOf("Porn")) // Default selection
    var masturbationNotes by mutableStateOf("")

    var pornSlipped by mutableStateOf("No")
    var pornCount by mutableStateOf(1)
    var pornTriggers by mutableStateOf(emptyList<String>())
    var pornNotes by mutableStateOf("")

    var thoughtsSlipped by mutableStateOf("No")
    var thoughtsCount by mutableStateOf(1)
    var thoughtsTriggers by mutableStateOf(emptyList<String>())
    var thoughtsNotes by mutableStateOf("")

    var urgeSlipped by mutableStateOf("No")
    var urgeCount by mutableStateOf(1)
    var urgeTriggers by mutableStateOf(emptyList<String>())
    var urgeNotes by mutableStateOf("")

    fun addRecord(activityType: String, count: Int, duration: Int) {
        viewModelScope.launch {
            repository.insert(HabitRecord(activityType = activityType, count = count, durationMinutes = duration))
        }
    }

    fun deleteRecord(record: HabitRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun saveTodayLog() {
        viewModelScope.launch {
            // Save Masturbation record (count = 0 means clean/resisted, count > 0 means slipped)
            val mCount = if (masturbationSlipped == "Yes") masturbationCount else 0
            repository.insert(HabitRecord(
                activityType = "Masturbation",
                count = mCount,
                durationMinutes = if (masturbationNotes.isNotEmpty()) masturbationNotes.length else 0
            ))

            // Save Porn record
            val pCount = if (pornSlipped == "Yes") pornCount else 0
            repository.insert(HabitRecord(
                activityType = "Pornography",
                count = pCount,
                durationMinutes = if (pornNotes.isNotEmpty()) pornNotes.length else 0
            ))

            // Save Thoughts record
            val tCount = if (thoughtsSlipped == "Yes") thoughtsCount else 0
            repository.insert(HabitRecord(
                activityType = "Dirty Thoughts",
                count = tCount,
                durationMinutes = if (thoughtsNotes.isNotEmpty()) thoughtsNotes.length else 0
            ))

            // Save Urges record
            val uCount = if (urgeSlipped == "Yes") urgeCount else 0
            repository.insert(HabitRecord(
                activityType = "Urge",
                count = uCount,
                durationMinutes = if (urgeNotes.isNotEmpty()) urgeNotes.length else 0
            ))

            // Save Mood as a special record (index stored in count)
            if (selectedMoodIndex >= 0) {
                repository.insert(HabitRecord(
                    activityType = "Mood",
                    count = selectedMoodIndex,
                    durationMinutes = 0
                ))
            }

            // Save Journal Notes if present
            if (journalNotes.isNotEmpty()) {
                repository.insert(HabitRecord(
                    activityType = "JournalNote",
                    count = 0,
                    durationMinutes = 0
                ))
            }

            // Reset Form fields to defaults/clean
            masturbationSlipped = "No"
            masturbationCount = 1
            masturbationTriggers = emptyList()
            masturbationNotes = ""

            pornSlipped = "No"
            pornCount = 1
            pornTriggers = emptyList()
            pornNotes = ""

            thoughtsSlipped = "No"
            thoughtsCount = 1
            thoughtsTriggers = emptyList()
            thoughtsNotes = ""

            urgeSlipped = "No"
            urgeCount = 1
            urgeTriggers = emptyList()
            urgeNotes = ""

            selectedMoodIndex = -1
            journalNotes = ""
        }
    }
}

