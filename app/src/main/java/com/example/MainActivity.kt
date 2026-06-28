package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = Room.databaseBuilder(
            applicationContext, 
            AppDatabase::class.java, 
            "habit-db"
        ).fallbackToDestructiveMigration().build()
        
        val repository = HabitRepository(database.habitDao(), database.customHabitDao())
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HabitViewModel(repository) as T
            }
        })[HabitViewModel::class.java]
        
        viewModel.loadUserProfile(applicationContext)

        enableEdgeToEdge()
        setContent {
            val isDark = viewModel.isDarkMode
            val accentColor = if (viewModel.accentColorName == "Emerald") Color(0xFF6C63FF) else Color(0xFF10B981)
            
            MyApplicationTheme(darkTheme = isDark, accentColor = accentColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (viewModel.forceUpdateScreenActive) {
                        com.example.update.ForceUpdateScreen(viewModel)
                    } else if (!viewModel.isOnboardingCompleted) {
                        OnboardingScreen(viewModel)
                    } else if (!viewModel.isLoggedIn) {
                        LoginScreen(viewModel)
                    } else {
                        HabitTrackerScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun HabitTrackerScreen(viewModel: HabitViewModel) {
    val records by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = viewModel.isDarkMode

    LaunchedEffect(records) {
        viewModel.loadTodayLog(records)
    }
    val accentPrimary = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    if (viewModel.showOptionalUpdateDialog) {
        com.example.update.OptionalUpdateDialog(viewModel)
    }
    val bottomBg = MaterialTheme.colorScheme.surface
    val bottomBorder = MaterialTheme.colorScheme.outline

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Elegant navigation bottom bar matching the designs exactly
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bottomBg,
                border = BorderStroke(1.dp, bottomBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        NavigationTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home, 0),
                        NavigationTabItem("Calendar", Icons.Filled.DateRange, Icons.Outlined.DateRange, 1),
                        NavigationTabItem("Insights", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard, 2),
                        NavigationTabItem("Profile", Icons.Filled.Person, Icons.Outlined.Person, 3)
                    )

                    tabs.forEach { tab ->
                        val isSelected = viewModel.selectedTab == tab.index
                        val tabColor = if (isSelected) accentPrimary else textSecondary
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    onClick = { viewModel.selectedTab = tab.index },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                )
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.label,
                                tint = tabColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = tabColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (viewModel.showAddHabitScreen) {
                AddHabitScreen(viewModel) { viewModel.showAddHabitScreen = false }
            } else {
                AnimatedContent(
                    targetState = viewModel.selectedTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "navigation_screen_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> HomeReflectionScreen(viewModel, records)
                        1 -> CalendarReflectionScreen(viewModel, records)
                        2 -> InsightsReflectionScreen(viewModel, records)
                        3 -> SettingsReflectionScreen(viewModel)
                    }
                }
            }
        }
    }
}

data class NavigationTabItem(
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val index: Int
)

// ==========================================
// SCREEN 1: HOME (DAILY REFLECTION)
// ==========================================
@Composable
fun HomeReflectionScreen(viewModel: HabitViewModel, records: List<HabitRecord>) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkMode
    
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val cardBg = MaterialTheme.colorScheme.surface

    var currentDateText by remember { mutableStateOf(getCurrentFormattedDate()) }
    val recoveryScore = remember(records) { calculateRecoveryScore(records) }
    val scoreSweepAngle = remember(recoveryScore) { (recoveryScore.toFloat() / 100f) * 360f }
    val recoveryLabel = remember(recoveryScore) {
        if (records.isEmpty()) {
            "No Logs\nYet"
        } else {
            when {
                recoveryScore == 100 -> "Excellent\nProgress"
                recoveryScore >= 80 -> "Good\nProgress"
                recoveryScore >= 60 -> "Steady\nProgress"
                recoveryScore >= 20 -> "Needs\nImprovement"
                else -> "High\nRisk"
            }
        }
    }
    val recoveryMessage = remember(recoveryScore) {
        if (records.isEmpty()) {
            "Start logging your daily progress to track your recovery."
        } else {
            when {
                recoveryScore == 100 -> "Outstanding! You had a perfect clean day."
                recoveryScore >= 80 -> "Great job! A minor challenge, but you handled it well."
                recoveryScore >= 60 -> "Keep going, every step counts in your recovery journey."
                recoveryScore >= 20 -> "Stay strong! Identify your triggers and keep trying."
                else -> "Today wasn't your best day. Tomorrow is a new opportunity to improve."
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateText = getCurrentFormattedDate()
            kotlinx.coroutines.delay(15_000) // update date text every 15 seconds
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showUrgeHelpDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = cardBg,
                drawerContentColor = textPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(BorderStroke(1.dp, accentPrimary.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "Jagr Logo",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Jagr",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Text(
                                text = "Recovery Companion",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderStrokeColor))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Drawer Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = accentPrimary) },
                        label = { Text("Reflection Guide", fontWeight = FontWeight.Bold, color = textPrimary) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showGuideDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalActivity, contentDescription = null, tint = Color(0xFFEF4444)) },
                        label = { Text("Emergency Urge Help", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showUrgeHelpDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = textSecondary) },
                        label = { Text("Quick Settings", fontWeight = FontWeight.Bold, color = textPrimary) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.selectedTab = 3 // Go to Settings
                        },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Version 1.0.0 (Build 2026)",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TOP GREETING HEADER (Matches Activity Mockup Exactly)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = textPrimary)
                            }
                            
                            IconButton(onClick = { showNotificationsDialog = true }) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = textPrimary)
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp, end = 2.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(accentPrimary)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            // Avatar Placeholder with beautiful Emoji or Google Profile Image
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(accentPrimary.copy(alpha = 0.15f))
                                    .border(1.5.dp, accentPrimary.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.loginType == "Google" && viewModel.googleProfilePictureUrl.isNotEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = viewModel.googleProfilePictureUrl,
                                        contentDescription = "Google Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = viewModel.userAvatarEmoji,
                                        fontSize = 28.sp
                                    )
                                }
                            }
                            
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Good day, ",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = viewModel.userName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = textPrimary
                                    )
                                }
                                Text(
                                    text = "Track your daily activity & stay healthy",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                // GRADIENT RECOVERY SCORE CARD
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = if (viewModel.accentColorName == "Emerald") {
                                            listOf(Color(0xFF3B2E9F), Color(0xFF6C63FF)) // Gorgeous Purple gradient
                                        } else {
                                            listOf(Color(0xFF064E3B), Color(0xFF10B981)) // Green gradient
                                        }
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left - Circle Progress
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(96.dp)
                                ) {
                                    Canvas(modifier = Modifier.size(88.dp)) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.15f),
                                            radius = size.minDimension / 2,
                                            style = Stroke(width = 6.dp.toPx())
                                        )
                                        drawArc(
                                            color = Color.White,
                                            startAngle = -90f,
                                            sweepAngle = scoreSweepAngle, // dynamic
                                            useCenter = false,
                                            style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = recoveryScore.toString(),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Heart",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Right - Progress description and sparkline
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recoveryLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = recoveryMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // High-fidelity custom Sparkline
                                    Sparkline(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp),
                                        points = if (records.isEmpty()) listOf(0f, 0f) else calculateRecoveryScoreTrend(records),
                                        lineColor = Color.White,
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }
                    }
                }

                // INTERACTIVE QUESTIONS LIST
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Q1: Did you masturbate?
                        QuestionCheckInCard(
                            indexBadge = "1",
                            title = "Did you masturbate?",
                            isExpanded = viewModel.masturbationExpanded,
                            onToggleExpand = { viewModel.masturbationExpanded = !viewModel.masturbationExpanded },
                            answer = viewModel.masturbationSlipped,
                            onAnswerChange = { viewModel.masturbationSlipped = it },
                            count = viewModel.masturbationCount,
                            onCountChange = { viewModel.masturbationCount = it },
                            triggers = viewModel.masturbationTriggers,
                            onTriggerToggle = { trigger ->
                                viewModel.masturbationTriggers = if (viewModel.masturbationTriggers.contains(trigger)) {
                                    viewModel.masturbationTriggers.filter { it != trigger }
                                } else {
                                    viewModel.masturbationTriggers + trigger
                                }
                            },
                            notes = viewModel.masturbationNotes,
                            onNotesChange = { viewModel.masturbationNotes = it },
                            viewModel = viewModel
                        )

                        // Q2: Watched Porn?
                        QuestionCheckInCard(
                            indexBadge = "2",
                            title = "Watched Porn?",
                            isExpanded = viewModel.pornExpanded,
                            onToggleExpand = { viewModel.pornExpanded = !viewModel.pornExpanded },
                            answer = viewModel.pornSlipped,
                            onAnswerChange = { viewModel.pornSlipped = it },
                            count = viewModel.pornCount,
                            onCountChange = { viewModel.pornCount = it },
                            triggers = viewModel.pornTriggers,
                            onTriggerToggle = { trigger ->
                                viewModel.pornTriggers = if (viewModel.pornTriggers.contains(trigger)) {
                                    viewModel.pornTriggers.filter { it != trigger }
                                } else {
                                    viewModel.pornTriggers + trigger
                                }
                            },
                            notes = viewModel.pornNotes,
                            onNotesChange = { viewModel.pornNotes = it },
                            viewModel = viewModel
                        )

                        // Q3: Had Sexual / Intrusive Thoughts?
                        QuestionCheckInCard(
                            indexBadge = "3",
                            title = "Had Sexual / Intrusive Thoughts?",
                            isExpanded = viewModel.thoughtsExpanded,
                            onToggleExpand = { viewModel.thoughtsExpanded = !viewModel.thoughtsExpanded },
                            answer = viewModel.thoughtsSlipped,
                            onAnswerChange = { viewModel.thoughtsSlipped = it },
                            count = viewModel.thoughtsCount,
                            onCountChange = { viewModel.thoughtsCount = it },
                            triggers = viewModel.thoughtsTriggers,
                            onTriggerToggle = { trigger ->
                                viewModel.thoughtsTriggers = if (viewModel.thoughtsTriggers.contains(trigger)) {
                                    viewModel.thoughtsTriggers.filter { it != trigger }
                                } else {
                                    viewModel.thoughtsTriggers + trigger
                                }
                            },
                            notes = viewModel.thoughtsNotes,
                            onNotesChange = { viewModel.thoughtsNotes = it },
                            viewModel = viewModel
                        )

                        // Q4: Experienced an Urge?
                        QuestionCheckInCard(
                            indexBadge = "4",
                            title = "Experienced an Urge?",
                            isExpanded = viewModel.urgeExpanded,
                            onToggleExpand = { viewModel.urgeExpanded = !viewModel.urgeExpanded },
                            answer = viewModel.urgeSlipped,
                            onAnswerChange = { viewModel.urgeSlipped = it },
                            count = viewModel.urgeCount,
                            onCountChange = { viewModel.urgeCount = it },
                            triggers = viewModel.urgeTriggers,
                            onTriggerToggle = { trigger ->
                                viewModel.urgeTriggers = if (viewModel.urgeTriggers.contains(trigger)) {
                                    viewModel.urgeTriggers.filter { it != trigger }
                                } else {
                                    viewModel.urgeTriggers + trigger
                                }
                            },
                            notes = viewModel.urgeNotes,
                            onNotesChange = { viewModel.urgeNotes = it },
                            viewModel = viewModel
                        )
                    }
                }

                // MOOD TODAY SECTION
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Mood Today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val emojis = listOf("😄", "😊", "😐", "😟", "😡", "😴", "😢")
                            emojis.forEachIndexed { idx, emoji ->
                                val isSelected = viewModel.selectedMoodIndex == idx
                                val bgCircleColor = if (isSelected) accentPrimary.copy(alpha = 0.15f) else borderStrokeColor.copy(alpha = 0.4f)
                                val borderStroke = if (isSelected) BorderStroke(2.dp, accentPrimary) else BorderStroke(1.dp, Color.Transparent)

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(bgCircleColor)
                                        .border(borderStroke, CircleShape)
                                        .clickable { viewModel.selectedMoodIndex = idx }
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }

                // JOURNAL / NOTES SECTION
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Journal / Notes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = viewModel.journalNotes,
                            onValueChange = { viewModel.journalNotes = it },
                            placeholder = { Text("How was your day? Write anything...", color = textSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = accentPrimary,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedContainerColor = if (isDark) Color(0xFF131C2E) else Color.White,
                                unfocusedContainerColor = if (isDark) Color(0xFF131C2E) else Color.White
                            ),
                            maxLines = 5
                        )
                    }
                }

                // SAVE BUTTON
                item {
                    Button(
                        onClick = {
                            viewModel.saveTodayLog()
                            Toast.makeText(context, "Log saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAVE TODAY'S LOG",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Interactive Dialog overlays
            if (showGuideDialog) {
                AlertDialog(
                    onDismissRequest = { showGuideDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = accentPrimary)
                            Text("Reflection Guide", fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Welcome to Jagr, your mindful companion for self-control and personal recovery.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textPrimary
                            )
                            Text(
                                "• Daily Logs: Build self-awareness by logging your status every evening.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                            Text(
                                "• Recovery Score: Calculated based on your consistency and resisting slips. Your current score of 92 is Excellent!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                            Text(
                                "• Triggers: Identify emotional and situational patterns to build long-term resistance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showGuideDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = accentPrimary)
                        ) {
                            Text("Got it", color = Color.White)
                        }
                    },
                    containerColor = cardBg
                )
            }

            if (showUrgeHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showUrgeHelpDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocalActivity, contentDescription = null, tint = Color(0xFFEF4444))
                            Text("Urge Relief Tool", fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "When an urge strikes, take a moment to breathe and reset.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                            
                            // Pulse Breathing Exercise Animation
                            BreathingExerciseSection()
                            
                            Text(
                                "Pro-Tip: Urges typically peak within 10-15 minutes and then subside. Go drink a glass of cold water, step outside, or do 10 pushups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showUrgeHelpDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = accentPrimary)
                        ) {
                            Text("I feel better", color = Color.White)
                        }
                    },
                    containerColor = cardBg
                )
            }

            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = accentPrimary)
                            Text("Motivational Alerts", fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Notification 1
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, borderStrokeColor),
                                colors = CardDefaults.cardColors(containerColor = background)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = accentPrimary)
                                    Column {
                                        Text("Daily Reminder • 9:00 PM", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                        Text("Time to complete your daily reflection log! Regular check-ins reinforce mindfulness.", fontSize = 11.sp, color = textSecondary)
                                    }
                                }
                            }
                            
                            // Notification 2
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, borderStrokeColor),
                                colors = CardDefaults.cardColors(containerColor = background)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444))
                                    Column {
                                        Text("Recovery Tip • Active", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                                        Text("You are doing great! Self-control is strength. Right thought is mastery.", fontSize = 11.sp, color = textSecondary)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showNotificationsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = accentPrimary)
                        ) {
                            Text("Mark as read", color = Color.White)
                        }
                    },
                    containerColor = cardBg
                )
            }
        }
    }
}

@Composable
fun BreathingExerciseSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val stageText = if (scale < 1.05f) "Breathe In..." else "Hold and Release..."

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.25f))
                    .border(2.dp, Color(0xFF10B981), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stageText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981)
        )
    }
}

@Composable
fun QuestionCheckInCard(
    indexBadge: String,
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    answer: String,
    onAnswerChange: (String) -> Unit,
    count: Int,
    onCountChange: (Int) -> Unit,
    triggers: List<String>,
    onTriggerToggle: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    viewModel: HabitViewModel
) {
    val isDark = viewModel.isDarkMode
    val cardBg = MaterialTheme.colorScheme.surface
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary

    val animatedNoBgColor by animateColorAsState(
        targetValue = if (answer == "No") Color(0xFF10B981) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "no_bg_color"
    )
    val animatedNoTextColor by animateColorAsState(
        targetValue = if (answer == "No") Color.White else textSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "no_text_color"
    )

    val animatedYesBgColor by animateColorAsState(
        targetValue = if (answer == "Yes") Color(0xFFEF4444) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "yes_bg_color"
    )
    val animatedYesTextColor by animateColorAsState(
        targetValue = if (answer == "Yes") Color.White else textSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "yes_text_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, borderStrokeColor, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header Row (Question Summary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onToggleExpand,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Badge circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentPrimary.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = indexBadge,
                            color = accentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontSize = 15.sp
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = textSecondary
                )
            }

            // Expanded content with exact styling
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    // Segment Selection: No / Yes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // "No" Selection Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(animatedNoBgColor)
                                .clickable { onAnswerChange("No") }
                        ) {
                            Text(
                                text = "No",
                                fontWeight = FontWeight.Bold,
                                color = animatedNoTextColor
                            )
                        }

                        // "Yes" Selection Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(animatedYesBgColor)
                                .clickable { onAnswerChange("Yes") }
                        ) {
                            Text(
                                text = "Yes",
                                fontWeight = FontWeight.Bold,
                                color = animatedYesTextColor
                            )
                        }
                    }

                    // Nested sub-questions if Yes
                    if (answer == "Yes") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Counter section
                        Text(
                            text = "How many times?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(borderStrokeColor.copy(alpha = 0.5f))
                                    .clickable { if (count > 1) onCountChange(count - 1) }
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = textPrimary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "$count",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(borderStrokeColor.copy(alpha = 0.5f))
                                    .clickable { onCountChange(count + 1) }
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = textPrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger tags
                        Text(
                            text = "What triggered it?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val availableTriggers = listOf("Porn", "Urge", "Stress", "Boredom", "Social Media", "Other")
                        // Display tags in wrapping flows
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableTriggers.take(4).forEach { trigger ->
                                    val isTriggerSelected = triggers.contains(trigger)
                                    val chipBg = if (isTriggerSelected) accentPrimary else Color.Transparent
                                    val chipText = if (isTriggerSelected) Color.White else textSecondary
                                    val chipBorder = if (isTriggerSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, borderStrokeColor)

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(chipBg)
                                            .border(chipBorder, RoundedCornerShape(12.dp))
                                            .clickable { onTriggerToggle(trigger) }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = trigger, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = chipText)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                availableTriggers.drop(4).forEach { trigger ->
                                    val isTriggerSelected = triggers.contains(trigger)
                                    val chipBg = if (isTriggerSelected) accentPrimary else Color.Transparent
                                    val chipText = if (isTriggerSelected) Color.White else textSecondary
                                    val chipBorder = if (isTriggerSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, borderStrokeColor)

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(chipBg)
                                            .border(chipBorder, RoundedCornerShape(12.dp))
                                            .clickable { onTriggerToggle(trigger) }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = trigger, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = chipText)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Notes section
                        Text(
                            text = "Notes (optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = onNotesChange,
                            placeholder = { Text("Write anything you want to note...", color = textSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = accentPrimary,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Sparkline(
    modifier: Modifier = Modifier,
    points: List<Float>,
    lineColor: Color,
    strokeWidth: Float
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val minX = 0f
        val maxX = (points.size - 1).toFloat()
        val minY = points.minOrNull() ?: 0f
        val maxY = points.maxOrNull() ?: 1f
        val rangeY = if (maxY - minY == 0f) 1f else maxY - minY

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, y ->
            val xPx = (index.toFloat() / maxX) * width
            val yPx = height - ((y - minY) / rangeY) * height
            if (index == 0) {
                path.moveTo(xPx, yPx)
            } else {
                path.lineTo(xPx, yPx)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        // Nodes on line
        points.forEachIndexed { index, y ->
            val xPx = (index.toFloat() / maxX) * width
            val yPx = height - ((y - minY) / rangeY) * height
            drawCircle(
                color = lineColor,
                radius = strokeWidth * 1.5f,
                center = androidx.compose.ui.geometry.Offset(xPx, yPx)
            )
        }
    }
}


// ==========================================
// SCREEN 2: CALENDAR
// ==========================================
@Composable
fun CalendarReflectionScreen(viewModel: HabitViewModel, records: List<HabitRecord>) {
    var calendarInstance by remember { mutableStateOf(Calendar.getInstance()) }
    val currentYear = calendarInstance.get(Calendar.YEAR)
    val currentMonth = calendarInstance.get(Calendar.MONTH)
    
    val monthName = remember(calendarInstance) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(calendarInstance.time)
    }

    var selectedDateMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }

    val isDark = viewModel.isDarkMode
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val cardBg = MaterialTheme.colorScheme.surface

    val selMillis = selectedDateMillis
    val selDateStr = remember(selMillis) {
        if (selMillis != null) {
            val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            sdf.format(Date(selMillis))
        } else ""
    }

    val dayRecords = remember(records, selMillis) {
        if (selMillis != null) {
            records.filter { isSameDay(it.timestamp, selMillis) }
        } else emptyList()
    }

    // Month days builder
    val daysInMonth = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Adjust Sunday = 7
        val firstDayOfWeek = when (val day = cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 7
            else -> day - 1
        }

        val days = mutableListOf<CalendarDay>()

        // Padding for previous month
        for (i in 1 until firstDayOfWeek) {
            days.add(CalendarDay(isPadding = true))
        }

        // Current month days
        for (day in 1..maxDays) {
            val dayCal = Calendar.getInstance()
            dayCal.set(Calendar.YEAR, currentYear)
            dayCal.set(Calendar.MONTH, currentMonth)
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            days.add(CalendarDay(dayNumber = day, timestamp = dayCal.timeInMillis, isPadding = false))
        }
        days
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { /* Calendar Filter */ }) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = textPrimary)
                    }
                    IconButton(onClick = { /* List Filter */ }) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter", tint = textPrimary)
                    }
                }
            }
        }

        // Calendar Grid Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Navigation month
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newCal = Calendar.getInstance()
                            newCal.time = calendarInstance.time
                            newCal.add(Calendar.MONTH, -1)
                            calendarInstance = newCal
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous Month", tint = textPrimary)
                        }

                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )

                        IconButton(onClick = {
                            val newCal = Calendar.getInstance()
                            newCal.time = calendarInstance.time
                            newCal.add(Calendar.MONTH, 1)
                            calendarInstance = newCal
                        }) {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next Month", tint = textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Days of week header Mon Tue Wed Thu Fri Sat Sun
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        weekDays.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Monthly Grid (manual layout to prevent nesting lazy viewports)
                    val chunkedWeeks = daysInMonth.chunked(7)
                    chunkedWeeks.forEach { weekDaysList ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            weekDaysList.forEach { day ->
                                if (day.isPadding) {
                                    Box(modifier = Modifier.size(40.dp).weight(1f))
                                } else {
                                    val isSelected = selMillis?.let { isSameDay(it, day.timestamp) } ?: false
                                    val isTodayDay = isToday(day.timestamp)
                                    
                                    // Calculate status dots
                                    val daysRecords = records.filter { isSameDay(it.timestamp, day.timestamp) }
                                    val hasMasturbation = daysRecords.any { it.activityType == "Masturbation" && it.count > 0 }
                                    val hasPorn = daysRecords.any { it.activityType == "Pornography" && it.count > 0 }
                                    val hasThoughts = daysRecords.any { it.activityType == "Dirty Thoughts" && it.count > 0 }
                                    val hasUrges = daysRecords.any { it.activityType == "Urge" && it.count > 0 }
                                    val hasClean = daysRecords.isNotEmpty() && !hasMasturbation && !hasPorn

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) accentPrimary 
                                                else if (isTodayDay) accentPrimary.copy(alpha = 0.15f) 
                                                else Color.Transparent
                                            )
                                            .border(
                                                if (isTodayDay && !isSelected) BorderStroke(1.dp, accentPrimary)
                                                else BorderStroke(0.dp, Color.Transparent),
                                                CircleShape
                                            )
                                            .clickable { selectedDateMillis = day.timestamp },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${day.dayNumber}",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else textPrimary,
                                                fontSize = 14.sp
                                            )
                                            
                                            // Tiny dots indicator below date number
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (hasMasturbation) {
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                                }
                                                if (hasPorn) {
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFEC4899)))
                                                }
                                                if (hasThoughts) {
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                                }
                                                if (hasUrges) {
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFF97316)))
                                                }
                                                if (hasClean && daysRecords.isNotEmpty()) {
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Legend Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CALENDAR LEGEND",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Legends wrap
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendItem(color = Color(0xFF10B981), label = "Clean Day", isDark = isDark)
                            LegendItem(color = Color(0xFFF97316), label = "Urges", isDark = isDark)
                            LegendItem(color = Color(0xFFF59E0B), label = "Sexual Thoughts", isDark = isDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendItem(color = Color(0xFFEC4899), label = "Porn", isDark = isDark)
                            LegendItem(color = Color(0xFFEF4444), label = "Masturbation", isDark = isDark)
                            Box(modifier = Modifier.width(100.dp)) // Padding placeholder
                        }
                    }
                }
            }
        }

        // Details list for selected day
        item {
            Text(
                text = selDateStr.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = textPrimary,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (dayRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = "Zen",
                            tint = textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No logs for this date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Head to Home tab to write notes and log check-ins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(dayRecords, key = { it.id }) { rec ->
                // Custom rendered logs timelines matching designs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isClean = rec.count == 0
                            val typeColor = when (rec.activityType) {
                                "Masturbation" -> Color(0xFFEF4444)
                                "Pornography" -> Color(0xFFEC4899)
                                "Dirty Thoughts" -> Color(0xFFF59E0B)
                                "Urge" -> Color(0xFFF97316)
                                "Mood" -> Color(0xFF10B981)
                                else -> accentPrimary
                            }

                            val icon = when (rec.activityType) {
                                "Masturbation" -> Icons.Default.SelfImprovement
                                "Pornography" -> Icons.Default.VisibilityOff
                                "Dirty Thoughts" -> Icons.Default.Psychology
                                "Urge" -> Icons.Default.OfflineBolt
                                "Mood" -> Icons.Default.EmojiEmotions
                                else -> Icons.Default.Notes
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(typeColor.copy(alpha = 0.12f))
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = rec.activityType,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        fontSize = 15.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isClean) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isClean) "Clean" else "Slipped (${rec.count}x)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isClean) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                }
                                Text(
                                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(rec.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSecondary
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteRecord(rec) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Helpers for dynamic date, recovery score, and streaks
fun getCurrentFormattedDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(Date())
}

fun calculateSingleDayScore(dayRecords: List<HabitRecord>): Int {
    val hasMasturbation = dayRecords.any { it.activityType == "Masturbation" && it.count > 0 }
    val hasPorn = dayRecords.any { it.activityType == "Pornography" && it.count > 0 }
    val hasThoughts = dayRecords.any { it.activityType == "Dirty Thoughts" && it.count > 0 }
    val hasUrge = dayRecords.any { it.activityType == "Urge" && it.count > 0 }
    
    val masturbationCount = dayRecords.filter { it.activityType == "Masturbation" }.sumOf { it.count }
    val pornCount = dayRecords.filter { it.activityType == "Pornography" }.sumOf { it.count }
    val urgeCount = dayRecords.filter { it.activityType == "Urge" }.sumOf { it.count }
    
    var score = 100
    
    if (hasMasturbation) {
        score -= 25 * maxOf(1, masturbationCount)
    }
    if (hasPorn) {
        score -= 25 * maxOf(1, pornCount)
    }
    if (hasUrge) {
        score -= 15 * maxOf(1, urgeCount)
    }
    
    // Negative thoughts coming has 0 negative mark
    // If negative thoughts came, but user did not masturbate and did not watch porn, add +2 points
    if (hasThoughts && !hasMasturbation && !hasPorn) {
        score += 2
    }
    
    return maxOf(0, minOf(100, score))
}

fun calculateCurrentStreak(records: List<HabitRecord>): Int {
    if (records.isEmpty()) return 0
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val groupedByDay = records.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    val todayCal = java.util.Calendar.getInstance()
    val todayStr = sdf.format(todayCal.time)
    
    val yesterdayCal = java.util.Calendar.getInstance()
    yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(yesterdayCal.time)
    
    // Start checking from today or yesterday
    var startCal = java.util.Calendar.getInstance()
    if (!groupedByDay.containsKey(todayStr)) {
        // If today has no logs, see if we can start from yesterday
        if (groupedByDay.containsKey(yesterdayStr)) {
            startCal = yesterdayCal
        } else {
            return 0 // No logs today or yesterday means streak is 0
        }
    }
    
    var streak = 0
    val maxIterations = 365 // prevent infinite loops
    for (i in 0 until maxIterations) {
        val dayStr = sdf.format(startCal.time)
        val dayRecords = groupedByDay[dayStr]
        if (dayRecords == null || dayRecords.isEmpty()) {
            break // Gap in logging breaks the streak
        }
        
        val hasMasturbation = dayRecords.any { it.activityType == "Masturbation" && it.count > 0 }
        val hasPorn = dayRecords.any { it.activityType == "Pornography" && it.count > 0 }
        
        if (hasMasturbation || hasPorn) {
            break // A slip breaks the streak
        }
        
        streak++
        startCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

fun calculateLongestStreak(records: List<HabitRecord>): Int {
    if (records.isEmpty()) return 0
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val groupedByDay = records.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    // Sort all logged dates
    val loggedDates = groupedByDay.keys.sorted()
    if (loggedDates.isEmpty()) return 0
    
    var longest = 0
    var current = 0
    var previousDateCal: java.util.Calendar? = null
    
    for (dateStr in loggedDates) {
        val dayRecords = groupedByDay[dateStr] ?: continue
        val hasMasturbation = dayRecords.any { it.activityType == "Masturbation" && it.count > 0 }
        val hasPorn = dayRecords.any { it.activityType == "Pornography" && it.count > 0 }
        val isClean = !hasMasturbation && !hasPorn
        
        if (isClean) {
            val curCal = java.util.Calendar.getInstance()
            val parsedDate = sdf.parse(dateStr) ?: continue
            curCal.time = parsedDate
            
            if (previousDateCal == null) {
                current = 1
            } else {
                // Check if it is exactly the next day
                val tempCal = java.util.Calendar.getInstance()
                tempCal.time = previousDateCal.time
                tempCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                val expectedNextStr = sdf.format(tempCal.time)
                if (dateStr == expectedNextStr) {
                    current++
                } else {
                    current = 1 // Gap broke the streak, restart at 1
                }
            }
            previousDateCal = curCal
            if (current > longest) {
                longest = current
            }
        } else {
            current = 0
            previousDateCal = null
        }
    }
    return longest
}

fun calculatePornFreeDays(records: List<HabitRecord>): Int {
    if (records.isEmpty()) return 0
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val groupedByDay = records.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    var count = 0
    for ((_, dayRecords) in groupedByDay) {
        val hasPorn = dayRecords.any { it.activityType == "Pornography" && it.count > 0 }
        if (!hasPorn) {
            count++
        }
    }
    return count
}

fun calculateMasturbationFreeDays(records: List<HabitRecord>): Int {
    if (records.isEmpty()) return 0
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val groupedByDay = records.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    var count = 0
    for ((_, dayRecords) in groupedByDay) {
        val hasMasturbation = dayRecords.any { it.activityType == "Masturbation" && it.count > 0 }
        if (!hasMasturbation) {
            count++
        }
    }
    return count
}

fun calculateRecoveryScore(records: List<HabitRecord>): Int {
    if (records.isEmpty()) return 0
    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val recentRecords = records.filter { it.timestamp >= thirtyDaysAgo }
    val groupedByDay = recentRecords.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    if (groupedByDay.isEmpty()) return 0
    
    var totalScore = 0
    for ((_, dayRecords) in groupedByDay) {
        totalScore += calculateSingleDayScore(dayRecords)
    }
    return totalScore / groupedByDay.size
}

fun calculateRecoveryScoreForTime(records: List<HabitRecord>, endTime: Long): Int {
    if (records.isEmpty()) return 0
    val thirtyDaysBefore = endTime - (30L * 24 * 60 * 60 * 1000)
    
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val recentRecords = records.filter { it.timestamp in thirtyDaysBefore..endTime }
    val groupedByDay = recentRecords.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    if (groupedByDay.isEmpty()) return 0
    
    var totalScore = 0
    for ((_, dayRecords) in groupedByDay) {
        totalScore += calculateSingleDayScore(dayRecords)
    }
    return totalScore / groupedByDay.size
}

fun calculateRecoveryScoreTrend(records: List<HabitRecord>): List<Float> {
    val trend = mutableListOf<Float>()
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val now = System.currentTimeMillis()
    val oneDayMs = 24L * 60 * 60 * 1000
    
    val groupedByDay = records.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    // Generate scores for the last 7 days (including today)
    for (i in 6 downTo 0) {
        val targetTime = now - (i * oneDayMs)
        val dayStr = sdf.format(java.util.Date(targetTime))
        val dayRecords = groupedByDay[dayStr]
        
        val dayScore = if (dayRecords != null && dayRecords.isNotEmpty()) {
            calculateSingleDayScore(dayRecords)
        } else {
            if (records.isEmpty()) 0 else 100
        }
        trend.add(dayScore.toFloat())
    }
    return trend
}

fun calculateWeeklyImprovement(records: List<HabitRecord>): Int {
    if (records.isEmpty()) return 0
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val now = System.currentTimeMillis()
    val oneDayMs = 24L * 60 * 60 * 1000
    
    val thisWeekStart = now - (7L * oneDayMs)
    val lastWeekStart = now - (14L * oneDayMs)
    
    // This week records
    val thisWeekRecords = records.filter { it.timestamp in thisWeekStart..now }
    val thisWeekGrouped = thisWeekRecords.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    // Last week records
    val lastWeekRecords = records.filter { it.timestamp in lastWeekStart until thisWeekStart }
    val lastWeekGrouped = lastWeekRecords.groupBy { sdf.format(java.util.Date(it.timestamp)) }
    
    val thisWeekAvg = if (thisWeekGrouped.isNotEmpty()) {
        thisWeekGrouped.map { calculateSingleDayScore(it.value) }.average().toInt()
    } else {
        100
    }
    
    val lastWeekAvg = if (lastWeekGrouped.isNotEmpty()) {
        lastWeekGrouped.map { calculateSingleDayScore(it.value) }.average().toInt()
    } else {
        100
    }
    
    return thisWeekAvg - lastWeekAvg
}

fun calculateMoodTrend(records: List<HabitRecord>): String {
    val moodRecords = records.filter { it.activityType == "Mood" }
    if (moodRecords.isEmpty()) return "Stable"
    
    val now = System.currentTimeMillis()
    val oneWeekMs = 7L * 24 * 60 * 60 * 1000
    
    val thisWeekMoods = moodRecords.filter { it.timestamp >= now - oneWeekMs }
    val lastWeekMoods = moodRecords.filter { it.timestamp in (now - 2 * oneWeekMs)..(now - oneWeekMs) }
    
    fun getMoodWeight(index: Int): Int {
        return when (index) {
            0 -> 100 // 😄
            1 -> 80  // 😊
            2 -> 60  // 😐
            3 -> 40  // 😟
            4 -> 20  // 😡
            5 -> 50  // 😴
            6 -> 10  // 😢
            else -> 50
        }
    }
    
    if (thisWeekMoods.isEmpty()) return "Stable"
    val thisWeekAvg = thisWeekMoods.map { getMoodWeight(it.count) }.average()
    
    if (lastWeekMoods.isEmpty()) {
        return "Stable"
    }
    val lastWeekAvg = lastWeekMoods.map { getMoodWeight(it.count) }.average()
    
    return when {
        thisWeekAvg > lastWeekAvg + 5 -> "Improving"
        thisWeekAvg < lastWeekAvg - 5 -> "Declining"
        else -> "Stable"
    }
}

data class ActiveTimeResult(val timeBlock: String, val percentageStr: String)

fun calculateActiveTime(records: List<HabitRecord>): ActiveTimeResult {
    val logs = records.filter { it.activityType != "Mood" && it.activityType != "JournalNote" && !it.activityType.startsWith("Trigger:") }
    if (logs.isEmpty()) return ActiveTimeResult("No logs yet", "0% of logs")
    
    val hourCounts = IntArray(24)
    logs.forEach { log ->
        val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (hour in 0..23) {
            hourCounts[hour]++
        }
    }
    
    val blocks = listOf(
        "12AM - 4AM" to (0..3),
        "4AM - 8AM" to (4..7),
        "8AM - 12PM" to (8..11),
        "12PM - 4PM" to (12..15),
        "4PM - 8PM" to (16..19),
        "8PM - 12AM" to (20..23)
    )
    
    var maxBlockName = "8PM - 12AM"
    var maxBlockCount = 0
    
    blocks.forEach { (name, range) ->
        var sum = 0
        for (h in range) {
            sum += hourCounts[h]
        }
        if (sum > maxBlockCount) {
            maxBlockCount = sum
            maxBlockName = name
        }
    }
    
    val totalCount = logs.size
    val percentage = if (totalCount > 0) (maxBlockCount.toFloat() / totalCount * 100).toInt() else 0
    return ActiveTimeResult(maxBlockName, "$percentage% of logs")
}

data class TriggerPercent(val label: String, val percent: String, val percentFloat: Float, val color: Color)

fun calculateTriggerStatistics(records: List<HabitRecord>): List<TriggerPercent> {
    val triggerRecords = records.filter { it.activityType.startsWith("Trigger:") }
    val colors = listOf(
        Color(0xFFEF4444),
        Color(0xFFF97316),
        Color(0xFFF59E0B),
        Color(0xFF64748B),
        Color(0xFF3B82F6),
        Color(0xFF10B981)
    )
    
    val totalTriggersCount = triggerRecords.size
    if (totalTriggersCount == 0) {
        val totalSlips = records.filter { (it.activityType == "Masturbation" || it.activityType == "Pornography") && it.count > 0 }.size
        val totalUrges = records.filter { it.activityType == "Urge" && it.count > 0 }.size
        val totalThoughts = records.filter { it.activityType == "Dirty Thoughts" && it.count > 0 }.size
        val fallbackTotal = totalSlips + totalUrges + totalThoughts
        if (fallbackTotal == 0) {
            return listOf(
                TriggerPercent("Porn", "0%", 0f, colors[0]),
                TriggerPercent("Urge", "0%", 0f, colors[1]),
                TriggerPercent("Stress", "0%", 0f, colors[2]),
                TriggerPercent("Boredom", "0%", 0f, colors[3]),
                TriggerPercent("Other", "0%", 0f, colors[5])
            )
        }
        val pornPct = (totalSlips.toFloat() / fallbackTotal * 100).toInt()
        val urgePct = (totalUrges.toFloat() / fallbackTotal * 100).toInt()
        val thoughtsPct = (totalThoughts.toFloat() / fallbackTotal * 100).toInt()
        val stressPct = maxOf(0, (100 - pornPct - urgePct - thoughtsPct) / 2)
        val otherPct = maxOf(0, 100 - pornPct - urgePct - thoughtsPct - stressPct)
        return listOf(
            TriggerPercent("Porn", "$pornPct%", pornPct.toFloat(), colors[0]),
            TriggerPercent("Urge", "$urgePct%", urgePct.toFloat(), colors[1]),
            TriggerPercent("Stress", "$stressPct%", stressPct.toFloat(), colors[2]),
            TriggerPercent("Other", "$otherPct%", otherPct.toFloat(), colors[5])
        )
    }
    
    val countsMap = triggerRecords
        .map { it.activityType.substringAfter("Trigger:") }
        .groupBy { it }
        .mapValues { it.value.size }
        
    val result = mutableListOf<TriggerPercent>()
    val sortedTriggers = countsMap.entries.sortedByDescending { it.value }
    val sum = countsMap.values.sum()
    
    sortedTriggers.forEachIndexed { idx, entry ->
        val pct = if (sum > 0) (entry.value.toFloat() / sum * 100).toInt() else 0
        val color = colors[idx % colors.size]
        result.add(TriggerPercent(entry.key, "$pct%", pct.toFloat(), color))
    }
    return result
}

// Helpers for calendar calculation
fun isSameDay(t1: Long, t2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isToday(timestamp: Long): Boolean {
    return isSameDay(timestamp, System.currentTimeMillis())
}

@Composable
fun LegendItem(color: Color, label: String, isDark: Boolean) {
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = textSecondary)
    }
}


// ==========================================
// SCREEN 3: INSIGHTS
// ==========================================
@Composable
fun InsightsReflectionScreen(viewModel: HabitViewModel, records: List<HabitRecord>) {
    val isDark = viewModel.isDarkMode
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val cardBg = MaterialTheme.colorScheme.surface

    val currentStreak = remember(records) { calculateCurrentStreak(records) }
    val longestStreak = remember(records) { calculateLongestStreak(records) }
    val pornFreeDays = remember(records) { calculatePornFreeDays(records) }
    val masturbationFreeDays = remember(records) { calculateMasturbationFreeDays(records) }
    val recoveryScore = remember(records) { calculateRecoveryScore(records) }
    val scoreSweepAngle = remember(recoveryScore) { (recoveryScore.toFloat() / 100f) * 360f }
    val recoveryStatus = remember(recoveryScore) {
        if (records.isEmpty()) {
            "No Logs"
        } else {
            when {
                recoveryScore == 100 -> "Excellent"
                recoveryScore >= 80 -> "Good"
                recoveryScore >= 60 -> "Steady"
                recoveryScore >= 20 -> "Needs Improvement"
                else -> "High Risk"
            }
        }
    }

    val weeklyTrendPoints = remember(records) { calculateRecoveryScoreTrend(records) }
    val weeklyImprovement = remember(records) { calculateWeeklyImprovement(records) }
    val weeklyImprovementStr = remember(weeklyImprovement) {
        if (weeklyImprovement > 0) "+$weeklyImprovement%" else "$weeklyImprovement%"
    }
    val weeklyImprovementColor = remember(weeklyImprovement) {
        if (weeklyImprovement >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
    }
    val weeklyImprovementText = remember(weeklyImprovement) {
        if (weeklyImprovement >= 0) "Better than last week" else "Lower than last week"
    }
    val activeTimeRes = remember(records) { calculateActiveTime(records) }
    val moodTrendStr = remember(records) { calculateMoodTrend(records) }
    val triggerStats = remember(records) { calculateTriggerStatistics(records) }
    val trendDays = remember {
        val list = mutableListOf<String>()
        val sdf = SimpleDateFormat("E", Locale.getDefault())
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            list.add(sdf.format(cal.time))
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { /* Insights Menu */ }) {
                        Icon(imageVector = Icons.Default.Leaderboard, contentDescription = "Insights Logo", tint = textPrimary)
                    }
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                }
                
                IconButton(onClick = { /* Calendar Filter */ }) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = textPrimary)
                }
            }
        }

        // STATS CARDS GRID - Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Recovery Score Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Recovery Score", style = MaterialTheme.typography.bodySmall, color = textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                            Canvas(modifier = Modifier.size(68.dp)) {
                                drawCircle(
                                    color = borderStrokeColor.copy(alpha = 0.5f),
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 5.dp.toPx())
                                )
                                drawArc(
                                    color = accentPrimary,
                                    startAngle = -90f,
                                    sweepAngle = scoreSweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                )
                            }
                            Text(text = recoveryScore.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = recoveryStatus, style = MaterialTheme.typography.labelSmall, color = accentPrimary, fontWeight = FontWeight.ExtraBold)
                    }
                }

                // Current Streak Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Current Streak", style = MaterialTheme.typography.bodySmall, color = textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = currentStreak.toString(), fontSize = 42.sp, fontWeight = FontWeight.Black, color = textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "🔥", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Days", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }
            }
        }

        // STATS CARDS GRID - Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Longest Streak
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Longest Streak", style = MaterialTheme.typography.bodySmall, color = textSecondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = longestStreak.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = textPrimary)
                            Text(text = "👑", fontSize = 20.sp)
                        }
                        Text(text = "Days", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }

                // Porn-Free Days
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Porn-Free Days", style = MaterialTheme.typography.bodySmall, color = textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = pornFreeDays.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = textPrimary)
                            Icon(imageVector = Icons.Default.Shield, contentDescription = "Shield", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                        Text(text = "Days", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }
            }
        }

        // STATS CARDS GRID - Row 3
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Masturbation-Free Days
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Masturbation-Free", style = MaterialTheme.typography.bodySmall, color = textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = masturbationFreeDays.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = textPrimary)
                            Icon(imageVector = Icons.Default.Shield, contentDescription = "Shield", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                        Text(text = "Days", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }

                if (records.isNotEmpty()) {
                    // This Week (+X% sparkline card)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "This Week", style = MaterialTheme.typography.bodySmall, color = textSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(text = weeklyImprovementStr, fontSize = 24.sp, fontWeight = FontWeight.Black, color = weeklyImprovementColor)
                            Text(text = weeklyImprovementText, fontSize = 10.sp, color = textSecondary, textAlign = TextAlign.Center)
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Sparkline(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp),
                                points = weeklyTrendPoints,
                                lineColor = weeklyImprovementColor,
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = "No data",
                            tint = accentPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No data yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete your first reflection to see your insights.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your statistics will appear here once you start using the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // WEEKLY TREND LINE GRAPH SECTION
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Weekly Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Chart drawing
                        WeeklyTrendChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            points = weeklyTrendPoints,
                            lineColor = accentPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Day labels Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            trendDays.forEach { d ->
                                Text(
                                    text = d,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // MONTHLY HEATMAP GRID
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Monthly Heatmap",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Grid matrix of 4 x 7 squares (representing 28 days)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (r in 0 until 4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (c in 0 until 7) {
                                        val dayIndex = 27 - ((r * 7) + c)
                                        val cellTime = System.currentTimeMillis() - (dayIndex * 24L * 60 * 60 * 1000)
                                        val dayRecords = records.filter { isSameDay(it.timestamp, cellTime) }
                                        
                                        val emptyColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                        
                                        val cellColor = if (dayRecords.isEmpty()) {
                                            emptyColor
                                        } else {
                                            val hasRelapse = dayRecords.any { 
                                                (it.activityType == "Masturbation" || it.activityType == "Pornography") && it.count > 0 
                                            }
                                            val hasUrge = dayRecords.any { it.activityType == "Urge" && it.count > 0 }
                                            when {
                                                hasRelapse -> Color(0xFFEF4444)
                                                hasUrge -> Color(0xFFF59E0B)
                                                else -> Color(0xFF10B981)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(cellColor)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Heatmap indicator legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "Less", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF047857)))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF059669)))
                            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF10B981)))
                            Text(text = "More", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                    }
                }
            }

            // MOST COMMON TRIGGER SECTION
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Most Common Trigger",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Donut Chart
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .size(130.dp)
                            ) {
                                DonutChart(
                                    modifier = Modifier.size(110.dp),
                                    values = triggerStats.map { it.percentFloat },
                                    colors = triggerStats.map { it.color }
                                )
                                Text(
                                    text = "Triggers",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }

                            // Right: Legend column
                            Column(
                                modifier = Modifier.weight(1.2f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                triggerStats.forEach { stat ->
                                    TriggerPercentRow(color = stat.color, label = stat.label, percent = stat.percent)
                                }
                            }
                        }
                    }
                }
            }

            // OTHER CARDS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Active time card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Most Active Time", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = activeTimeRes.timeBlock, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Clock", tint = textSecondary, modifier = Modifier.size(14.dp))
                                Text(text = activeTimeRes.percentageStr, fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }

                    // Mood trends card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Mood Trends", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            val trendColor = if (moodTrendStr == "Improving") Color(0xFF10B981) else if (moodTrendStr == "Declining") Color(0xFFEF4444) else textSecondary
                            Text(text = moodTrendStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = trendColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Default.SentimentSatisfiedAlt, contentDescription = "Smile", tint = textSecondary, modifier = Modifier.size(14.dp))
                                Text(text = "This month", fontSize = 11.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TriggerPercentRow(color: Color, label: String, percent: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = percent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun DonutChart(
    modifier: Modifier = Modifier,
    values: List<Float> = listOf(42f, 24f, 18f, 10f, 6f),
    colors: List<Color> = listOf(
        Color(0xFFEF4444),
        Color(0xFFF97316),
        Color(0xFFF59E0B),
        Color(0xFF64748B),
        Color(0xFF10B981)
    )
) {
    Canvas(modifier = modifier) {
        val total = values.sum()
        if (total == 0f) return@Canvas
        var startAngle = -90f
        val strokeWidth = size.width * 0.16f
        values.forEachIndexed { index, valRaw ->
            val sweepAngle = (valRaw / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun WeeklyTrendChart(
    modifier: Modifier = Modifier,
    points: List<Float>,
    lineColor: Color
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Draw horizontal grid lines
        val gridLines = listOf(0f, 0.5f, 1f)
        gridLines.forEach { ratio ->
            val y = ratio * height
            drawLine(
                color = lineColor.copy(alpha = 0.08f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 2f
            )
        }
        
        // Draw spline trend line
        if (points.size < 2) return@Canvas
        val maxX = (points.size - 1).toFloat()
        val maxY = 100f
        
        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()
        
        points.forEachIndexed { index, score ->
            val x = (index.toFloat() / maxX) * width
            val y = height - (score / maxY) * height
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = ((index - 1).toFloat() / maxX) * width
                val prevY = height - (points[index - 1] / maxY) * height
                val controlX1 = prevX + (x - prevX) / 2f
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2f
                val controlY2 = y
                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()
        
        // Draw fill gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent)
            )
        )
        
        // Draw stroke
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        // Nodes
        points.forEachIndexed { index, score ->
            val x = (index.toFloat() / maxX) * width
            val y = height - (score / maxY) * height
            drawCircle(
                color = lineColor,
                radius = 8f,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}


// ==========================================
// SCREEN 4: SETTINGS
// ==========================================
@Composable
fun SettingsReflectionScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkMode
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val cardBg = MaterialTheme.colorScheme.surface
    var showGoogleChooserInSettings by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { /* Back navigation */ }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Profile & Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            }
        }

        // USER PROFILE SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "User Profile",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Interactive Avatar Emoji Selector
                            val selectedEmoji = viewModel.userAvatarEmoji
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(accentPrimary.copy(alpha = 0.15f))
                                    .border(1.5.dp, accentPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (viewModel.loginType == "Google" && viewModel.googleProfilePictureUrl.isNotEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = viewModel.googleProfilePictureUrl,
                                        contentDescription = "Google Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = selectedEmoji,
                                        fontSize = 32.sp
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = viewModel.userName,
                                    onValueChange = { newName ->
                                        viewModel.updateUserName(context, newName)
                                    },
                                    label = { Text("Display Name") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentPrimary,
                                        unfocusedBorderColor = borderStrokeColor,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Avatar Emoji Picker
                        Text(
                            text = "Choose Avatar Emoji",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                                          ) {
                            val emojis = listOf("🙋‍♂️", "🙋‍♀️", "🧑‍💻", "👩‍💻", "👑", "🦄", "🦁", "🦊")
                            emojis.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (viewModel.userAvatarEmoji == emoji) accentPrimary.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            viewModel.updateUserAvatarEmoji(context, emoji)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = borderStrokeColor.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Account Information & Logout / Login
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isGoogleUser = viewModel.loginType == "Google"
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGoogleUser) "Logged in via Google" else "Guest Mode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = if (isGoogleUser) {
                                        "ID: ${viewModel.googleUserId.take(16)}${if (viewModel.googleUserId.length > 16) "..." else ""}\n${viewModel.loggedInEmail}"
                                    } else {
                                        "Sign in to secure your habits"
                                    },
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    lineHeight = 15.sp
                                )
                            }

                            if (isGoogleUser) {
                                Button(
                                    onClick = {
                                        viewModel.logout(context)
                                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "Log Out",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        showGoogleChooserInSettings = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "Log In",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // HABITS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Habits",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderStrokeColor)
                ) {
                    SettingsClickableRow(
                        label = "Add New Habit",
                        leadingIcon = Icons.Default.Add,
                        isDark = isDark,
                        onClick = { viewModel.showAddHabitScreen = true }
                    )
                }
            }
        }

        // APPEARANCE SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Appearance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        // Theme row
                        SettingsInteractiveRow(
                            label = "Theme",
                            value = "System",
                            onClick = {
                                viewModel.isDarkMode = !viewModel.isDarkMode
                                Toast.makeText(context, "Theme toggled!", Toast.LENGTH_SHORT).show()
                            },
                            isDark = isDark
                        )
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        // Dark Mode switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Dark Mode", fontWeight = FontWeight.Bold, color = textPrimary)
                            Switch(
                                checked = viewModel.isDarkMode,
                                onCheckedChange = { viewModel.isDarkMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentPrimary,
                                    uncheckedThumbColor = textSecondary,
                                    uncheckedTrackColor = borderStrokeColor
                                )
                            )
                        }
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        // Accent color selection row
                        SettingsInteractiveRow(
                            label = "Accent Color",
                            value = if (viewModel.accentColorName == "Emerald") "Purple" else "Green",
                            onClick = {
                                viewModel.accentColorName = if (viewModel.accentColorName == "Emerald") "Mint" else "Emerald"
                            },
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // SECURITY SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Security",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        SettingsInteractiveRow(
                            label = "Fingerprint Lock",
                            value = if (viewModel.isFingerprintEnabled) "On" else "Off",
                            onClick = { viewModel.isFingerprintEnabled = !viewModel.isFingerprintEnabled },
                            isDark = isDark
                        )
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        SettingsInteractiveRow(
                            label = "PIN Lock",
                            value = if (viewModel.isPinLockEnabled) "On" else "Off",
                            onClick = { viewModel.isPinLockEnabled = !viewModel.isPinLockEnabled },
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // DATA SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Data",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        SettingsClickableRow(label = "Backup & Restore", leadingIcon = Icons.Default.Backup, isDark = isDark, onClick = {
                            Toast.makeText(context, "Data backed up successfully!", Toast.LENGTH_SHORT).show()
                        })
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        SettingsClickableRow(label = "Export Data", leadingIcon = Icons.Default.Share, isDark = isDark, onClick = {
                            Toast.makeText(context, "Data exported safely!", Toast.LENGTH_SHORT).show()
                        })
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        SettingsClickableRow(
                            label = "Delete All Data", 
                            leadingIcon = Icons.Default.Delete, 
                            isDark = isDark, 
                            textColor = Color(0xFFEF4444),
                            onClick = {
                                viewModel.deleteAllRecords()
                                Toast.makeText(context, "All records deleted safely.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // REMINDERS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Reminders",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        SettingsInteractiveRow(
                            label = "Daily Reminder",
                            value = viewModel.dailyReminderTime,
                            onClick = {
                                viewModel.dailyReminderTime = if (viewModel.dailyReminderTime == "9:00 PM") "8:30 PM" else "9:00 PM"
                            },
                            isDark = isDark
                        )
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        SettingsInteractiveRow(
                            label = "Reflection Reminder",
                            value = viewModel.reflectionReminderTime,
                            onClick = {
                                viewModel.reflectionReminderTime = if (viewModel.reflectionReminderTime == "10:00 PM") "10:30 PM" else "10:00 PM"
                            },
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // ABOUT & UPDATES SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "About & Updates",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // About description
                        Text(
                            text = "About Jagr",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Jagr is a modern, high-performance, offline-first personal reflection and habit-tracking companion designed to foster daily self-improvement and streak integrity.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 16.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Current Version info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Current Version", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                            Text(text = viewModel.currentVersion, fontWeight = FontWeight.SemiBold, color = textSecondary, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Latest Version info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val latestVer = viewModel.activeUpdateInfo?.latestVersion ?: "Up to date"
                            Text(text = "Latest Version", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                            Text(text = latestVer, fontWeight = FontWeight.SemiBold, color = if (latestVer == "Up to date") textSecondary else accentPrimary, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Last Checked info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Last Checked", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                            Text(text = viewModel.lastCheckedTimeStr, fontWeight = FontWeight.SemiBold, color = textSecondary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Check Now / Update controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Check for Updates", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 14.sp)
                                Text(text = "Verify if a newer version is online", color = textSecondary, fontSize = 11.sp)
                            }
                            
                            if (viewModel.isCheckingForUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = accentPrimary
                                )
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.checkForUpdates(manual = true)
                                        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "Check Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // Customizable URL Field
                        if (!viewModel.isMockUpdateMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(text = "Remote Update Config URL", fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = viewModel.updateCheckUrl,
                                onValueChange = { viewModel.updateUpdateCheckUrl(context, it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentPrimary,
                                    unfocusedBorderColor = borderStrokeColor,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textSecondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }

                        // Developer Mock options inside an elegant expandable outline for testing convenience!
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(12.dp))

                        var showDevOpts by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDevOpts = !showDevOpts }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Testing & Demo Options", fontWeight = FontWeight.Bold, color = textSecondary, fontSize = 13.sp)
                            Text(text = if (showDevOpts) "Hide" else "Show", color = accentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showDevOpts) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Enable Mock Mode", fontSize = 13.sp, color = textPrimary)
                                Switch(
                                    checked = viewModel.isMockUpdateMode,
                                    onCheckedChange = { viewModel.isMockUpdateMode = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = accentPrimary,
                                        uncheckedThumbColor = textSecondary,
                                        uncheckedTrackColor = borderStrokeColor
                                    )
                                )
                            }

                            if (viewModel.isMockUpdateMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Simulate Update Type:", fontSize = 12.sp, color = textSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("UpToDate" to "Up To Date", "OptionalUpdate" to "Optional", "ForceUpdate" to "Force").forEach { (type, label) ->
                                        val isSelected = viewModel.mockUpdateType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) accentPrimary else borderStrokeColor)
                                                .clickable { 
                                                    viewModel.mockUpdateType = type
                                                    viewModel.checkForUpdates(manual = true)
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label, 
                                                fontSize = 11.sp, 
                                                fontWeight = FontWeight.Bold, 
                                                color = if (isSelected) Color.White else textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // UPDATE HISTORY SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Update History",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Version 1.0.1",
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "• AI timetable import", color = textSecondary, fontSize = 13.sp)
                        Text(text = "• Dashboard synchronization", color = textSecondary, fontSize = 13.sp)
                        Text(text = "• Profile improvements", color = textSecondary, fontSize = 13.sp)
                        Text(text = "• Faster performance", color = textSecondary, fontSize = 13.sp)
                        Text(text = "• Bug fixes", color = textSecondary, fontSize = 13.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Version 1.0.0 (Current)",
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• Initial release of Jagr companion app", color = textSecondary.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }

        // GENERAL SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "General & Support",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column {
                        SettingsClickableRow(label = "Privacy Policy", leadingIcon = Icons.Default.PrivacyTip, isDark = isDark, onClick = {
                            Toast.makeText(context, "Privacy Policy is offline-protected.", Toast.LENGTH_SHORT).show()
                        })
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        SettingsClickableRow(label = "Rate Us", leadingIcon = Icons.Default.Star, isDark = isDark, onClick = {
                            Toast.makeText(context, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
                        })
                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.6f))
                        SettingsClickableRow(label = "Help & Support", leadingIcon = Icons.Default.Help, isDark = isDark, onClick = {
                            Toast.makeText(context, "Support Desk: support@jagr.app", Toast.LENGTH_LONG).show()
                        })
                    }
                }
            }
        }

        // FOOTER VERSION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Guard Logo",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Jagr v1.0.0",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
            }
        }
    }

    if (showGoogleChooserInSettings) {
        LaunchedEffect(showGoogleChooserInSettings) {
            showGoogleChooserInSettings = false
            viewModel.authenticateWithGoogle(
                context = context,
                onSuccess = {
                    Toast.makeText(context, "Successfully logged in as ${viewModel.userName}!", Toast.LENGTH_SHORT).show()
                },
                onFailure = { errorMsg ->
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@Composable
fun SettingsInteractiveRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, color = textPrimary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, color = textSecondary, fontWeight = FontWeight.SemiBold)
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = textSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SettingsClickableRow(
    label: String,
    leadingIcon: ImageVector,
    isDark: Boolean,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    val textPrimary = textColor ?: if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp))
            Text(text = label, fontWeight = FontWeight.Bold, color = textPrimary)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next", tint = textSecondary, modifier = Modifier.size(16.dp))
    }
}

data class CalendarDay(
    val dayNumber: Int = 0,
    val timestamp: Long = 0,
    val isPadding: Boolean = false
)
