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
import com.example.update.UpdateInfo
import com.example.update.UpdateResult

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {
    val uiState: StateFlow<List<HabitRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Update System States
    val currentVersion = "1.0.0"
    var updateCheckUrl by mutableStateOf("https://raw.githubusercontent.com/FaheemAnsari/Jagr/main/update.json")
    var isMockUpdateMode by mutableStateOf(false)
    var mockUpdateType by mutableStateOf("UpToDate") // "UpToDate", "OptionalUpdate", "ForceUpdate"
    
    var updateResultState by mutableStateOf<UpdateResult?>(null)
    var isCheckingForUpdates by mutableStateOf(false)
    var lastCheckedTimeStr by mutableStateOf("Never")
    var activeUpdateInfo by mutableStateOf<UpdateInfo?>(null)
    var forceUpdateScreenActive by mutableStateOf(false)
    var showOptionalUpdateDialog by mutableStateOf(false)

    init {
        // Automatically check for updates on app launch
        checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            isCheckingForUpdates = true
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            lastCheckedTimeStr = formatter.format(java.util.Date())
            
            if (isMockUpdateMode) {
                // Simulate checking latency
                kotlinx.coroutines.delay(1000)
                when (mockUpdateType) {
                    "UpToDate" -> {
                        updateResultState = UpdateResult.UpToDate
                        activeUpdateInfo = null
                        forceUpdateScreenActive = false
                        showOptionalUpdateDialog = false
                    }
                    "OptionalUpdate" -> {
                        val info = UpdateInfo(
                            latestVersion = "1.0.1",
                            minimumSupportedVersion = "1.0.0",
                            updateTitle = "New Update Available",
                            updateMessage = "Bug fixes, AI improvements, dashboard synchronization, and timetable enhancements.",
                            downloadUrl = "https://github.com/FaheemAnsari/Jagr/releases/download/v1.0.1/jagr-v1.0.1.apk",
                            forceUpdate = false
                        )
                        updateResultState = UpdateResult.NewUpdateAvailable(info, false)
                        activeUpdateInfo = info
                        forceUpdateScreenActive = false
                        showOptionalUpdateDialog = true
                    }
                    "ForceUpdate" -> {
                        val info = UpdateInfo(
                            latestVersion = "1.1.0",
                            minimumSupportedVersion = "1.1.0",
                            updateTitle = "New Update Available",
                            updateMessage = "This version is mandatory for continued secure access. Please update now to secure your logs.",
                            downloadUrl = "https://github.com/FaheemAnsari/Jagr/releases/download/v1.1.0/jagr-v1.1.0.apk",
                            forceUpdate = true
                        )
                        updateResultState = UpdateResult.NewUpdateAvailable(info, true)
                        activeUpdateInfo = info
                        forceUpdateScreenActive = true
                        showOptionalUpdateDialog = false
                    }
                }
            } else {
                // Real HTTP Call
                try {
                    val service = com.example.update.HttpUpdateService()
                    val result = service.checkForUpdates(updateCheckUrl, currentVersion)
                    updateResultState = result
                    when (result) {
                        is UpdateResult.NewUpdateAvailable -> {
                            activeUpdateInfo = result.updateInfo
                            if (result.mustForceUpdate) {
                                forceUpdateScreenActive = true
                                showOptionalUpdateDialog = false
                            } else {
                                forceUpdateScreenActive = false
                                showOptionalUpdateDialog = true
                            }
                        }
                        else -> {
                            activeUpdateInfo = null
                            forceUpdateScreenActive = false
                            showOptionalUpdateDialog = false
                        }
                    }
                } catch (e: Exception) {
                    updateResultState = UpdateResult.Error(e.message ?: "Failed to contact update server")
                    activeUpdateInfo = null
                    forceUpdateScreenActive = false
                    showOptionalUpdateDialog = false
                }
            }
            isCheckingForUpdates = false
        }
    }

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
    var masturbationSlipped by mutableStateOf("") // Default unselected
    var masturbationCount by mutableStateOf(1)
    var masturbationTriggers by mutableStateOf(emptyList<String>())
    var masturbationNotes by mutableStateOf("")

    var pornSlipped by mutableStateOf("") // Default unselected
    var pornCount by mutableStateOf(1)
    var pornTriggers by mutableStateOf(emptyList<String>())
    var pornNotes by mutableStateOf("")

    var thoughtsSlipped by mutableStateOf("") // Default unselected
    var thoughtsCount by mutableStateOf(1)
    var thoughtsTriggers by mutableStateOf(emptyList<String>())
    var thoughtsNotes by mutableStateOf("")

    var urgeSlipped by mutableStateOf("") // Default unselected
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

    private fun isToday(timestamp: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val cal2 = java.util.Calendar.getInstance()
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    var hasLoadedTodayLog = false

    fun loadTodayLog(records: List<HabitRecord>) {
        if (hasLoadedTodayLog || records.isEmpty()) return
        
        val todayRecords = records.filter { isToday(it.timestamp) }
        if (todayRecords.isEmpty()) {
            hasLoadedTodayLog = true
            return
        }

        // Restore values from saved records
        val masturbationRecord = todayRecords.find { it.activityType == "Masturbation" }
        if (masturbationRecord != null) {
            masturbationSlipped = if (masturbationRecord.count > 0) "Yes" else "No"
            masturbationCount = if (masturbationRecord.count > 0) masturbationRecord.count else 1
        }

        val pornRecord = todayRecords.find { it.activityType == "Pornography" }
        if (pornRecord != null) {
            pornSlipped = if (pornRecord.count > 0) "Yes" else "No"
            pornCount = if (pornRecord.count > 0) pornRecord.count else 1
        }

        val thoughtsRecord = todayRecords.find { it.activityType == "Dirty Thoughts" }
        if (thoughtsRecord != null) {
            thoughtsSlipped = if (thoughtsRecord.count > 0) "Yes" else "No"
            thoughtsCount = if (thoughtsRecord.count > 0) thoughtsRecord.count else 1
        }

        val urgeRecord = todayRecords.find { it.activityType == "Urge" }
        if (urgeRecord != null) {
            urgeSlipped = if (urgeRecord.count > 0) "Yes" else "No"
            urgeCount = if (urgeRecord.count > 0) urgeRecord.count else 1
        }

        val moodRecord = todayRecords.find { it.activityType == "Mood" }
        if (moodRecord != null) {
            selectedMoodIndex = moodRecord.count
        }

        val todayTriggers = todayRecords
            .filter { it.activityType.startsWith("Trigger:") }
            .map { it.activityType.substringAfter("Trigger:") }

        if (todayTriggers.isNotEmpty()) {
            if (masturbationSlipped == "Yes") {
                masturbationTriggers = todayTriggers
            }
            if (pornSlipped == "Yes") {
                pornTriggers = todayTriggers
            }
            if (thoughtsSlipped == "Yes") {
                thoughtsTriggers = todayTriggers
            }
            if (urgeSlipped == "Yes") {
                urgeTriggers = todayTriggers
            }
        }

        hasLoadedTodayLog = true
    }

    fun saveTodayLog() {
        viewModelScope.launch {
            // Delete all records of today first to avoid duplicates
            val todayRecords = uiState.value.filter { isToday(it.timestamp) }
            todayRecords.forEach { record ->
                repository.delete(record)
            }

            // Save Masturbation record (count = 0 means clean/resisted, count > 0 means slipped)
            val mCount = if (masturbationSlipped == "Yes") masturbationCount else 0
            repository.insert(HabitRecord(
                activityType = "Masturbation",
                count = mCount,
                durationMinutes = if (masturbationNotes.isNotEmpty()) masturbationNotes.length else 0
            ))
            if (masturbationSlipped == "Yes") {
                masturbationTriggers.forEach { trigger ->
                    repository.insert(HabitRecord(
                        activityType = "Trigger:$trigger",
                        count = 1,
                        durationMinutes = 0
                    ))
                }
            }

            // Save Porn record
            val pCount = if (pornSlipped == "Yes") pornCount else 0
            repository.insert(HabitRecord(
                activityType = "Pornography",
                count = pCount,
                durationMinutes = if (pornNotes.isNotEmpty()) pornNotes.length else 0
            ))
            if (pornSlipped == "Yes") {
                pornTriggers.forEach { trigger ->
                    repository.insert(HabitRecord(
                        activityType = "Trigger:$trigger",
                        count = 1,
                        durationMinutes = 0
                    ))
                }
            }

            // Save Thoughts record
            val tCount = if (thoughtsSlipped == "Yes") thoughtsCount else 0
            repository.insert(HabitRecord(
                activityType = "Dirty Thoughts",
                count = tCount,
                durationMinutes = if (thoughtsNotes.isNotEmpty()) thoughtsNotes.length else 0
            ))
            if (thoughtsSlipped == "Yes") {
                thoughtsTriggers.forEach { trigger ->
                    repository.insert(HabitRecord(
                        activityType = "Trigger:$trigger",
                        count = 1,
                        durationMinutes = 0
                    ))
                }
            }

            // Save Urges record
            val uCount = if (urgeSlipped == "Yes") urgeCount else 0
            repository.insert(HabitRecord(
                activityType = "Urge",
                count = uCount,
                durationMinutes = if (urgeNotes.isNotEmpty()) urgeNotes.length else 0
            ))
            if (urgeSlipped == "Yes") {
                urgeTriggers.forEach { trigger ->
                    repository.insert(HabitRecord(
                        activityType = "Trigger:$trigger",
                        count = 1,
                        durationMinutes = 0
                    ))
                }
            }

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

            hasLoadedTodayLog = true
        }
    }
}

