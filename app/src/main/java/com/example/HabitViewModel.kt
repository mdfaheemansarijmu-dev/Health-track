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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

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
        customProfilePictureUri = sharedPref.getString("custom_profile_picture_uri", "") ?: ""
        dismissedUpdateVersion = sharedPref.getString("dismissed_update_version", "") ?: ""
        updateCheckUrl = sharedPref.getString("update_check_url", "https://raw.githubusercontent.com/FaheemAnsari/Jagr/main/update.json") ?: "https://raw.githubusercontent.com/FaheemAnsari/Jagr/main/update.json"
        
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                isLoggedIn = true
                loggedInEmail = currentUser.email ?: loggedInEmail
                if (!currentUser.displayName.isNullOrBlank()) {
                    userName = currentUser.displayName!!
                }
                if (currentUser.photoUrl != null && currentUser.photoUrl.toString().isNotEmpty()) {
                    googleProfilePictureUrl = currentUser.photoUrl.toString()
                }
            }
        } catch (e: Exception) {
            // Firebase Auth initialization check safely wrapped
        }

        loadChallenges(context)

        // Trigger check for updates immediately on app launch (guaranteeing settings are loaded)
        checkForUpdates(manual = false)
    }

    // Challenge System States & Actions
    var challengesList by mutableStateOf<List<HabitChallenge>>(emptyList())
    var activeChallenge by mutableStateOf<HabitChallenge?>(null)
    var showCreateChallengeDialog by mutableStateOf(false)
    var showCheckInDialogForChallenge by mutableStateOf<HabitChallenge?>(null)

    val activeChallengesList: List<HabitChallenge>
        get() = challengesList.filter { it.isActive && !it.isCompleted }

    fun loadChallenges(context: android.content.Context) {
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = sharedPref.getString("user_challenges_json", "") ?: ""
        
        val list = deserializeChallenges(jsonStr)
        challengesList = list
        activeChallenge = list.firstOrNull { it.isActive && !it.isCompleted } ?: list.firstOrNull()
        
        updateAllChallengeNotifications(context, challengesList)
    }

    private fun saveChallengesInternal(context: android.content.Context, list: List<HabitChallenge>) {
        val jsonStr = serializeChallenges(list)
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_challenges_json", jsonStr)
            apply()
        }
    }

    fun saveChallenges(context: android.content.Context) {
        saveChallengesInternal(context, challengesList)
        activeChallenge = challengesList.firstOrNull { it.isActive && !it.isCompleted } ?: challengesList.firstOrNull()
        updateAllChallengeNotifications(context, challengesList)
    }

    fun createNewChallenge(
        context: android.content.Context,
        habitName: String,
        totalDays: Int,
        attendanceTime: String,
        showLockscreen: Boolean
    ) {
        val daysList = (1..totalDays).map { dayNum ->
            ChallengeDayRecord(dayNumber = dayNum, status = "PENDING")
        }
        
        val updatedList = challengesList.toMutableList()

        val newChallenge = HabitChallenge(
            id = java.util.UUID.randomUUID().toString(),
            title = "$totalDays-Day Challenge: $habitName",
            habitName = habitName,
            totalDays = totalDays,
            startTimestamp = System.currentTimeMillis(),
            attendanceTime = attendanceTime,
            showLockscreenNotification = showLockscreen,
            isCompleted = false,
            isActive = true,
            dayRecords = daysList
        )

        updatedList.add(0, newChallenge)
        challengesList = updatedList
        activeChallenge = newChallenge
        saveChallenges(context)
    }

    fun submitChallengeAttendance(
        context: android.content.Context,
        challengeId: String,
        dayNumber: Int,
        isSuccess: Boolean,
        notes: String
    ) {
        val updatedList = challengesList.map { challenge ->
            if (challenge.id == challengeId) {
                val updatedRecords = challenge.dayRecords.map { dayRecord ->
                    if (dayRecord.dayNumber == dayNumber) {
                        dayRecord.copy(
                            status = if (isSuccess) "SUCCESS" else "FAILED",
                            notes = notes,
                            timestamp = System.currentTimeMillis()
                        )
                    } else dayRecord
                }
                val allDone = updatedRecords.all { it.status != "PENDING" }
                challenge.copy(
                    dayRecords = updatedRecords,
                    isCompleted = allDone
                )
            } else challenge
        }

        challengesList = updatedList
        saveChallenges(context)
    }

    fun toggleChallengeNotification(context: android.content.Context, challengeId: String, enabled: Boolean) {
        val updatedList = challengesList.map { challenge ->
            if (challenge.id == challengeId) {
                challenge.copy(showLockscreenNotification = enabled)
            } else challenge
        }
        challengesList = updatedList
        saveChallenges(context)
    }

    fun deleteChallenge(context: android.content.Context, challengeId: String) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.cancel(challengeId.hashCode())

        val updatedList = challengesList.filterNot { it.id == challengeId }
        challengesList = updatedList
        saveChallenges(context)
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
    var customProfilePictureUri by mutableStateOf("")
    var isAuthenticating by mutableStateOf(false)
    var authenticationError by mutableStateOf<String?>(null)

    fun updateCustomProfilePicture(context: android.content.Context, uriString: String) {
        customProfilePictureUri = uriString
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("custom_profile_picture_uri", uriString)
            apply()
        }
    }

    fun completeOnboarding(context: android.content.Context) {
        isOnboardingCompleted = true
        val sharedPref = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("is_onboarding_completed", true)
            apply()
        }
    }

    private fun fetchDeviceGoogleAccount(context: android.content.Context): Pair<String, String>? {
        return try {
            val am = android.accounts.AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            if (accounts.isNotEmpty()) {
                val email = accounts[0].name
                val nameParts = email.substringBefore("@").split(".", "_", "-")
                val formattedName = nameParts.joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                Pair(email, if (formattedName.isNotBlank()) formattedName else "Google User")
            } else {
                null
            }
        } catch (e: Exception) {
            null
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
            var authenticated = false

            try {
                val targetClientId = try {
                    context.getString(com.example.R.string.default_web_client_id)
                } catch (e: Exception) {
                    "33764241003-ummvv34ul4r1s9tiiq0ko5e2ehrugta6.apps.googleusercontent.com"
                }

                val credentialManager = CredentialManager.create(context)
                
                var result: androidx.credentials.GetCredentialResponse? = null
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setServerClientId(targetClientId)
                        .setFilterByAuthorizedAccounts(true)
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    result = credentialManager.getCredential(
                        context = context,
                        request = request
                    )
                } catch (e: Exception) {
                    // Retry with filterByAuthorizedAccounts = false to allow new account selection
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setServerClientId(targetClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    result = credentialManager.getCredential(
                        context = context,
                        request = request
                    )
                }

                if (result != null) {
                    val credential = result.credential
                    if (credential is androidx.credentials.CustomCredential && 
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val email = googleIdTokenCredential.id
                        val displayName = googleIdTokenCredential.displayName ?: "Google User"
                        val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                        val idToken = googleIdTokenCredential.idToken

                        // Also authenticate with Firebase Auth via Google ID Token if idToken is available
                        if (idToken.isNotBlank()) {
                            try {
                                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                                    .addOnSuccessListener { authResult ->
                                        val fbUser = authResult.user
                                        saveGoogleProfile(
                                            context = context,
                                            userId = fbUser?.uid ?: email,
                                            email = fbUser?.email ?: email,
                                            name = fbUser?.displayName ?: displayName,
                                            photoUrl = fbUser?.photoUrl?.toString() ?: photoUrl
                                        )
                                    }
                            } catch (e: Exception) {
                                // Fallback to local profile saving
                                saveGoogleProfile(context, email, email, displayName, photoUrl)
                            }
                        } else {
                            saveGoogleProfile(context, email, email, displayName, photoUrl)
                        }

                        isAuthenticating = false
                        authenticated = true
                        onSuccess()
                        return@launch
                    }
                }
            } catch (e: Exception) {
                // Fallback to automatic account resolution below
            }

            if (!authenticated) {
                val account = fetchDeviceGoogleAccount(context)
                if (account != null) {
                    val (email, name) = account
                    saveGoogleProfile(context, email, email, name, "")
                    isAuthenticating = false
                    onSuccess()
                } else {
                    isAuthenticating = false
                    onFailure("No Google accounts found on this device. Please sign in to a Google account in Settings.")
                }
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

    fun loginWithFirebaseEmail(
        context: android.content.Context,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isBlank() || pass.isBlank()) {
            onFailure("Please enter both email and password.")
            return
        }
        isAuthenticating = true
        try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), pass)
                .addOnSuccessListener { result ->
                    isAuthenticating = false
                    val firebaseUser = result.user
                    val userEmail = firebaseUser?.email ?: email.trim()
                    val displayName = firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                        ?: userEmail.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    val photoUrl = firebaseUser?.photoUrl?.toString() ?: ""
                    
                    saveGoogleProfile(
                        context = context,
                        userId = firebaseUser?.uid ?: userEmail,
                        email = userEmail,
                        name = displayName,
                        photoUrl = photoUrl
                    )
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    isAuthenticating = false
                    onFailure(e.localizedMessage ?: "Firebase Sign In failed.")
                }
        } catch (e: Exception) {
            isAuthenticating = false
            onFailure(e.localizedMessage ?: "Firebase Authentication service unavailable.")
        }
    }

    fun signUpWithFirebaseEmail(
        context: android.content.Context,
        email: String,
        pass: String,
        name: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (email.isBlank() || pass.length < 6) {
            onFailure("Please enter a valid email and a password with at least 6 characters.")
            return
        }
        isAuthenticating = true
        try {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), pass)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    val displayName = if (name.isNotBlank()) name.trim() else email.substringBefore("@")
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    firebaseUser?.updateProfile(profileUpdates)
                    
                    isAuthenticating = false
                    saveGoogleProfile(
                        context = context,
                        userId = firebaseUser?.uid ?: email.trim(),
                        email = firebaseUser?.email ?: email.trim(),
                        name = displayName,
                        photoUrl = ""
                    )
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    isAuthenticating = false
                    onFailure(e.localizedMessage ?: "Firebase Account Creation failed.")
                }
        } catch (e: Exception) {
            isAuthenticating = false
            onFailure(e.localizedMessage ?: "Firebase Authentication service unavailable.")
        }
    }

    fun loginAsGuestFirebase(
        context: android.content.Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        isAuthenticating = true
        try {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener { result ->
                    isAuthenticating = false
                    loginAsGuest(context)
                    onSuccess()
                }
                .addOnFailureListener {
                    isAuthenticating = false
                    loginAsGuest(context)
                    onSuccess()
                }
        } catch (e: Exception) {
            isAuthenticating = false
            loginAsGuest(context)
            onSuccess()
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
        customProfilePictureUri = ""
        
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore sign out exception
        }

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
    var showDailyLogScreen by mutableStateOf(false)
    var isDailyLogReadOnly by mutableStateOf(false)
    var isTodayLogSubmitted by mutableStateOf(false)
    var todaySubmissionTimestamp by mutableStateOf<Long?>(null)
    var urgeResisted by mutableStateOf("") // Options: "Yes", "No", ""

    var selectedTrigger by mutableStateOf("") // Options: Social Media, Porn, Stress, Boredom, Loneliness, Anxiety, Relationship, Other
    var journalNotes by mutableStateOf("")

    // Form expanded states
    var masturbationExpanded by mutableStateOf(true)
    var pornExpanded by mutableStateOf(false)
    var thoughtsExpanded by mutableStateOf(false)
    var urgeExpanded by mutableStateOf(false)
    var triggerQuestionExpanded by mutableStateOf(false)

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
        val todayRecords = records.filter { isToday(it.timestamp) }
        val submission = todayRecords.find { it.activityType == "ReflectionSubmission" }
        isTodayLogSubmitted = submission != null
        todaySubmissionTimestamp = submission?.timestamp

        if (hasLoadedTodayLog || records.isEmpty()) return
        
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

        val urgeResistedRecord = todayRecords.find { it.activityType == "UrgeResisted" }
        if (urgeResistedRecord != null) {
            urgeResisted = if (urgeResistedRecord.count > 0) "Yes" else "No"
        } else {
            urgeResisted = if (urgeSlipped == "Yes") "Yes" else ""
        }

        val todayTriggerRecord = todayRecords.find { 
            it.activityType.startsWith("Trigger:") && 
            it.activityType.substringAfter("Trigger:") in listOf("Social Media", "Porn", "Stress", "Boredom", "Loneliness", "Anxiety", "Relationship", "Other")
        }
        if (todayTriggerRecord != null) {
            selectedTrigger = todayTriggerRecord.activityType.substringAfter("Trigger:")
        } else {
            selectedTrigger = ""
        }

        val journalRecord = todayRecords.find { it.activityType.startsWith("JournalNote:") }
        if (journalRecord != null) {
            journalNotes = journalRecord.activityType.substringAfter("JournalNote:")
        } else {
            journalNotes = ""
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
                
                val resistedVal = if (urgeResisted == "Yes") 1 else 0
                repository.insert(HabitRecord(
                    activityType = "UrgeResisted",
                    count = resistedVal,
                    durationMinutes = 0
                ))
            }

            // Save ReflectionSubmission record (critical for tracking active time and reflection days)
            repository.insert(HabitRecord(
                activityType = "ReflectionSubmission",
                count = 1,
                durationMinutes = 0
            ))

            // Save Biggest Trigger record if one was selected
            if (selectedTrigger.isNotEmpty()) {
                repository.insert(HabitRecord(
                    activityType = "Trigger:$selectedTrigger",
                    count = 1,
                    durationMinutes = 0
                ))
            }

            // Save Journal Notes if present
            if (journalNotes.isNotEmpty()) {
                repository.insert(HabitRecord(
                    activityType = "JournalNote:$journalNotes",
                    count = 0,
                    durationMinutes = 0
                ))
            }

            isTodayLogSubmitted = true
            todaySubmissionTimestamp = System.currentTimeMillis()
            hasLoadedTodayLog = true
        }
    }

    // App Usage & Screen Time State
    var showAppUsageScreen by mutableStateOf(false)
    var selectedUsageTimeFrame by mutableStateOf(UsageTimeFrame.TODAY)
    var appUsageSummaryState by mutableStateOf<AppUsageSummary?>(null)
    var appDailyLimits by mutableStateOf<Map<String, Int>>(emptyMap())

    fun refreshAppUsageSummary(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val summary = AppUsageTracker.getUsageSummary(context, selectedUsageTimeFrame)
            val prefs = context.getSharedPreferences("jagr_app_limits", Context.MODE_PRIVATE)
            val limits = prefs.all.mapValues { (_, value) -> (value as? Int) ?: 0 }
            
            val updatedTopApps = summary.topApps.map { app ->
                val limit = limits[app.packageName] ?: 0
                app.copy(dailyLimitMinutes = limit)
            }
            
            val updatedSummary = summary.copy(topApps = updatedTopApps)
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                appUsageSummaryState = updatedSummary
                appDailyLimits = limits
            }
        }
    }

    fun setAppDailyLimit(context: Context, packageName: String, limitMinutes: Int) {
        val prefs = context.getSharedPreferences("jagr_app_limits", Context.MODE_PRIVATE)
        prefs.edit().putInt(packageName, limitMinutes).apply()
        refreshAppUsageSummary(context)
    }

    // Browser & Adult Content Protection State
    var isAccessibilityGuardActive by mutableStateOf(false)
    var isBrowserProtectionEnabled by mutableStateOf(true)
    var isHighSensitivityMode by mutableStateOf(true)
    var adultIncidentLogs by mutableStateOf<List<AdultIncident>>(emptyList())
    var customBlockedDomainsList by mutableStateOf<Set<String>>(emptySet())

    fun refreshBrowserProtectionStatus(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val isAccEnabled = AdultContentGuardManager.isAccessibilityServiceEnabled(context)
            val isProtEnabled = AdultContentGuardManager.isProtectionEnabled(context)
            val isHigh = AdultContentGuardManager.isHighSensitivity(context)
            val incidents = AdultContentGuardManager.getIncidents(context)
            val customDomains = AdultContentGuardManager.getCustomBlockedDomains(context)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                isAccessibilityGuardActive = isAccEnabled
                isBrowserProtectionEnabled = isProtEnabled
                isHighSensitivityMode = isHigh
                adultIncidentLogs = incidents
                customBlockedDomainsList = customDomains
            }
        }
    }

    fun toggleBrowserProtection(context: Context, enabled: Boolean) {
        AdultContentGuardManager.setProtectionEnabled(context, enabled)
        refreshBrowserProtectionStatus(context)
    }

    fun toggleHighSensitivity(context: Context, high: Boolean) {
        AdultContentGuardManager.setHighSensitivity(context, high)
        refreshBrowserProtectionStatus(context)
    }

    fun addCustomBlockedDomain(context: Context, domain: String) {
        AdultContentGuardManager.addCustomBlockedDomain(context, domain)
        refreshBrowserProtectionStatus(context)
    }

    fun removeCustomBlockedDomain(context: Context, domain: String) {
        AdultContentGuardManager.removeCustomBlockedDomain(context, domain)
        refreshBrowserProtectionStatus(context)
    }

    fun clearAdultIncidents(context: Context) {
        AdultContentGuardManager.clearIncidents(context)
        refreshBrowserProtectionStatus(context)
    }

    fun testAdultUrlDetection(context: Context, input: String): Pair<Boolean, String>? {
        val result = AdultContentGuardManager.checkUrlOrQueryForAdultContent(context, input)
        if (result != null && result.first) {
            AdultContentGuardManager.recordIncident(
                context = context,
                browserPackage = "com.example.test",
                browserName = "Test Simulator",
                detectedUrlOrTerm = input,
                matchedPattern = result.second
            )
            refreshBrowserProtectionStatus(context)
        }
        return result
    }
}


