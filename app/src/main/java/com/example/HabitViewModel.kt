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
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {
    val uiState: StateFlow<List<HabitRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customHabits: StateFlow<List<CustomHabit>> = repository.allCustomHabits
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
    var dismissedUpdateVersion by mutableStateOf("")

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
                            minimumVersion = "1.0.0",
                            releaseNotes = "Bug fixes, AI improvements, dashboard synchronization, and timetable enhancements.",
                            apkUrl = "https://github.com/FaheemAnsari/Jagr/releases/download/v1.0.1/jagr-v1.0.1.apk",
                            forceUpdate = false
                        )
                        updateResultState = UpdateResult.NewUpdateAvailable(info, false)
                        activeUpdateInfo = info
                        forceUpdateScreenActive = false
                        if (manual || info.latestVersion != dismissedUpdateVersion) {
                            showOptionalUpdateDialog = true
                        } else {
                            showOptionalUpdateDialog = false
                        }
                    }
                    "ForceUpdate" -> {
                        val info = UpdateInfo(
                            latestVersion = "1.1.0",
                            minimumVersion = "1.1.0",
                            releaseNotes = "This version is mandatory for continued secure access. Please update now to secure your logs.",
                            apkUrl = "https://github.com/FaheemAnsari/Jagr/releases/download/v1.1.0/jagr-v1.1.0.apk",
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
                                if (manual || result.updateInfo.latestVersion != dismissedUpdateVersion) {
                                    showOptionalUpdateDialog = true
                                } else {
                                    showOptionalUpdateDialog = false
                                }
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
    var showAddHabitScreen by mutableStateOf(false)
    var isDarkMode by mutableStateOf(false) // Start in gorgeous light/airy Activity theme by default
    var userName by mutableStateOf("Jerome")
    var userAvatarEmoji by mutableStateOf("🙋‍♂️")
    var accentColorName by mutableStateOf("Emerald") // Matches Mockup Settings

    fun updateUserName(context: android.content.Context, name: String) {
        userName = name
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("user_name", name)
            apply()
        }
    }

    fun updateUserAvatarEmoji(context: android.content.Context, emoji: String) {
        userAvatarEmoji = emoji
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("user_avatar_emoji", emoji)
            apply()
        }
    }

    fun loadUserProfile(context: android.content.Context) {
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        userName = sharedPref.getString("user_name", "Jerome") ?: "Jerome"
        userAvatarEmoji = sharedPref.getString("user_avatar_emoji", "🙋‍♂️") ?: "🙋‍♂️"
        isOnboardingCompleted = sharedPref.getBoolean("is_onboarding_completed", false)
        isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        loggedInEmail = sharedPref.getString("logged_in_email", "") ?: ""
        loginType = sharedPref.getString("login_type", "") ?: ""
        googleUserId = sharedPref.getString("google_user_id", "") ?: ""
        googleProfilePictureUrl = sharedPref.getString("google_profile_picture_url", "") ?: ""
        dismissedUpdateVersion = sharedPref.getString("dismissed_update_version", "") ?: ""
        updateCheckUrl = sharedPref.getString("update_check_url", "https://raw.githubusercontent.com/FaheemAnsari/Jagr/main/update.json") ?: "https://raw.githubusercontent.com/FaheemAnsari/Jagr/main/update.json"
        
        // Trigger check for updates immediately on app launch (guaranteeing settings are loaded)
        checkForUpdates(manual = false)
    }

    fun dismissOptionalUpdate(context: android.content.Context) {
        showOptionalUpdateDialog = false
        activeUpdateInfo?.let { info ->
            dismissedUpdateVersion = info.latestVersion
            val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString("dismissed_update_version", info.latestVersion)
                apply()
            }
        }
    }

    fun updateUpdateCheckUrl(context: android.content.Context, url: String) {
        updateCheckUrl = url
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("update_check_url", url)
            apply()
        }
    }

    var isOnboardingCompleted by mutableStateOf(false)
    var isLoggedIn by mutableStateOf(false)
    var loggedInEmail by mutableStateOf("")
    var loginType by mutableStateOf("") // "Google" or "Guest"
    var googleUserId by mutableStateOf("")
    var googleProfilePictureUrl by mutableStateOf("")
    var isAuthenticating by mutableStateOf(false)
    var authenticationError by mutableStateOf<String?>(null)

    fun completeOnboarding(context: android.content.Context) {
        isOnboardingCompleted = true
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("is_onboarding_completed", true)
            apply()
        }
    }

    fun authenticateWithGoogle(
        context: android.content.Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            isAuthenticating = true
            authenticationError = null
            try {
                // Safely read from BuildConfig without forcing a compile-time failure
                val rawClientId = try {
                    val buildConfigClass = Class.forName("com.example.BuildConfig")
                    val field = buildConfigClass.getField("GOOGLE_WEB_CLIENT_ID")
                    field.get(null) as? String ?: ""
                } catch (e: Exception) {
                    ""
                }
                
                val clientId = if (rawClientId.isBlank() || rawClientId == "YOUR_GOOGLE_WEB_CLIENT_ID") {
                    // Fallback client ID so that it doesn't fail compilation or execution, but triggers standard sign-in prompt
                    "1079547613754-dummyclientid123456.apps.googleusercontent.com"
                } else {
                    rawClientId
                }

                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential && 
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    val displayName = googleIdTokenCredential.displayName ?: "Google User"
                    val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                    
                    val subId = try {
                        googleIdTokenCredential.id
                    } catch (e: Exception) {
                        email
                    }

                    saveGoogleProfile(context, subId, email, displayName, photoUrl)
                    isAuthenticating = false
                    onSuccess()
                } else {
                    isAuthenticating = false
                    onFailure("Received invalid credential type from Google")
                }
            } catch (e: GetCredentialCancellationException) {
                isAuthenticating = false
                onFailure("Sign-in cancelled")
            } catch (e: GetCredentialUnsupportedException) {
                isAuthenticating = false
                onFailure("Google Play Services or Credential Manager is not supported on this device")
            } catch (e: GetCredentialException) {
                isAuthenticating = false
                val msg = e.message ?: "Authentication failed"
                if (msg.contains("No credential", ignoreCase = true)) {
                    onFailure("No Google accounts found or Google Play Services is outdated")
                } else {
                    onFailure(msg)
                }
            } catch (e: Exception) {
                isAuthenticating = false
                onFailure(e.localizedMessage ?: "An unexpected error occurred during Google Sign-In")
            }
        }
    }

    fun saveGoogleProfile(
        context: android.content.Context,
        userId: String,
        email: String,
        name: String,
        photoUrl: String
    ) {
        isLoggedIn = true
        loggedInEmail = email
        loginType = "Google"
        userName = name
        googleUserId = userId
        googleProfilePictureUrl = photoUrl
        
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("is_logged_in", true)
            putString("logged_in_email", email)
            putString("login_type", "Google")
            putString("user_name", name)
            putString("google_user_id", userId)
            putString("google_profile_picture_url", photoUrl)
            apply()
        }
    }

    fun loginWithGoogle(context: android.content.Context, email: String, name: String) {
        isLoggedIn = true
        loggedInEmail = email
        loginType = "Google"
        userName = name
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("is_logged_in", true)
            putString("logged_in_email", email)
            putString("login_type", "Google")
            putString("user_name", name)
            apply()
        }
    }

    fun loginAsGuest(context: android.content.Context) {
        isLoggedIn = true
        loggedInEmail = "guest@jagr.app"
        loginType = "Guest"
        userName = "Guest User"
        userAvatarEmoji = "🦊"
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("is_logged_in", true)
            putString("logged_in_email", "guest@jagr.app")
            putString("login_type", "Guest")
            putString("user_name", "Guest User")
            putString("user_avatar_emoji", "🦊")
            apply()
        }
    }

    fun logout(context: android.content.Context) {
        isLoggedIn = false
        loggedInEmail = ""
        loginType = ""
        googleUserId = ""
        googleProfilePictureUrl = ""
        
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                // Ignore clear state failure
            }
        }

        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("is_logged_in", false)
            putString("logged_in_email", "")
            putString("login_type", "")
            putString("google_user_id", "")
            putString("google_profile_picture_url", "")
            apply()
        }
    }
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

    fun addCustomHabit(title: String, description: String) {
        viewModelScope.launch {
            repository.insertCustomHabit(CustomHabit(title = title, description = description))
        }
    }

    fun deleteCustomHabit(habitId: Int) {
        viewModelScope.launch {
            repository.deleteCustomHabit(habitId)
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

