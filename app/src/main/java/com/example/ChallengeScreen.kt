package com.example

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ChallengeScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkMode
    
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val cardBg = MaterialTheme.colorScheme.surface

    val activeChallenges = viewModel.activeChallengesList
    val allChallenges = viewModel.challengesList

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // HEADER ROW
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Habit Challenges 🏆",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set custom day goals, run multiple challenges concurrently, and check in nightly.",
                        fontSize = 13.sp,
                        color = textSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { viewModel.showCreateChallengeDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Challenge",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Goal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }

        // ACTIVE CHALLENGES SECTION (SUPPORTING MULTIPLE ACTIVE CHALLENGES)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeChallenges.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Challenges (${activeChallenges.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Running ${activeChallenges.size} Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    activeChallenges.forEach { challenge ->
                        ActiveChallengeHeroCard(
                            challenge = challenge,
                            viewModel = viewModel,
                            onCheckInClick = { viewModel.showCheckInDialogForChallenge = challenge }
                        )
                    }
                } else {
                    // Empty active challenge state card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(accentPrimary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "No active challenge",
                                    tint = accentPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Active Challenges",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start a 3, 5, 10, 30, 60, 90, or 200-day challenge! You can run multiple challenges at the same time.",
                                fontSize = 13.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.showCreateChallengeDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentPrimary)
                            ) {
                                Text("Create First Challenge 🚀", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // REAL NOTIFICATION SYSTEM & PUSH CONTROLS CARD
        item {
            RealNotificationSystemCard(
                activeChallengesCount = activeChallenges.size,
                activeChallenges = activeChallenges
            )
        }

        // PAST / ALL CHALLENGES HISTORY SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Challenge History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (allChallenges.isEmpty()) {
                    Text(
                        text = "No challenge history logged yet.",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        allChallenges.forEach { challenge ->
                            PastChallengeItemCard(challenge = challenge, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    // CREATE CHALLENGE DIALOG
    if (viewModel.showCreateChallengeDialog) {
        CreateChallengeDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showCreateChallengeDialog = false }
        )
    }

    // CHECK-IN ATTENDANCE DIALOG
    viewModel.showCheckInDialogForChallenge?.let { challenge ->
        ChallengeCheckInDialog(
            challenge = challenge,
            viewModel = viewModel,
            onDismiss = { viewModel.showCheckInDialogForChallenge = null }
        )
    }
}

@Composable
fun ActiveChallengeHeroCard(
    challenge: HabitChallenge,
    viewModel: HabitViewModel,
    onCheckInClick: () -> Unit
) {
    val context = LocalContext.current
    val cardBg = MaterialTheme.colorScheme.surface
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary

    val currentDay = challenge.getCurrentDayNumber()
    val progress = challenge.calculateProgress()
    val progressPercent = (progress * 100).toInt()
    val successCount = challenge.getSuccessCount()
    
    // Check status of today's day record
    val todayRecord = challenge.dayRecords.firstOrNull { it.dayNumber == currentDay }
    val todayStatus = todayRecord?.status ?: "PENDING"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, accentPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & Duration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACTIVE CHALLENGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = challenge.habitName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${challenge.totalDays} Days",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Progress Bar Section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day $currentDay of ${challenge.totalDays}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "$progressPercent% Complete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = accentPrimary,
                    trackColor = accentPrimary.copy(alpha = 0.15f)
                )
            }

            // Attendance Check-in Action Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, borderStrokeColor)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Clock",
                                tint = accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nightly Attendance: ${challenge.attendanceTime}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }

                        // Today status pill
                        val (pillBg, pillText, pillLabel) = when (todayStatus) {
                            "SUCCESS" -> Triple(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981), "Passed ✅")
                            "FAILED" -> Triple(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444), "Slipped ❌")
                            else -> Triple(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B), "Pending ⏰")
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = pillBg
                        ) {
                            Text(
                                text = pillLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onCheckInClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToReg,
                            contentDescription = "Check in",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (todayStatus == "PENDING") "Submit Nightly Attendance" else "Update Today's Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Days Grid / Day-by-Day Timeline
            Column {
                Text(
                    text = "Daily Challenge Timeline ($successCount/${challenge.totalDays} Passed)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(challenge.dayRecords) { dayRecord ->
                        val isCurrentDay = dayRecord.dayNumber == currentDay
                        val boxBorderColor = if (isCurrentDay) accentPrimary else borderStrokeColor
                        val (bgColor, icon, iconTint) = when (dayRecord.status) {
                            "SUCCESS" -> Triple(Color(0xFF10B981).copy(alpha = 0.15f), Icons.Default.CheckCircle, Color(0xFF10B981))
                            "FAILED" -> Triple(Color(0xFFEF4444).copy(alpha = 0.15f), Icons.Default.Cancel, Color(0xFFEF4444))
                            else -> Triple(MaterialTheme.colorScheme.background, Icons.Outlined.Circle, textSecondary)
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(if (isCurrentDay) 2.dp else 1.dp, boxBorderColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "D${dayRecord.dayNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrentDay) FontWeight.Black else FontWeight.Bold,
                                    color = if (isCurrentDay) accentPrimary else textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Day ${dayRecord.dayNumber} status",
                                    tint = iconTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Toggle lockscreen notification persistence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = "Lockscreen reminder",
                        tint = accentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Lock Screen & Notification Panel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Keep challenge visible on your device screen",
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                }

                Switch(
                    checked = challenge.showLockscreenNotification,
                    onCheckedChange = { enabled ->
                        viewModel.toggleChallengeNotification(context, challenge.id, enabled)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentPrimary)
                )
            }
        }
    }
}

@Composable
fun RealNotificationSystemCard(
    activeChallengesCount: Int,
    activeChallenges: List<HabitChallenge>
) {
    val context = LocalContext.current
    val cardBg = MaterialTheme.colorScheme.surface
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                android.widget.Toast.makeText(context, "Notification permission granted! Nightly check-in alarms are active.", android.widget.Toast.LENGTH_LONG).show()
                updateAllChallengeNotifications(context, activeChallenges)
            } else {
                android.widget.Toast.makeText(context, "Permission denied. Nightly push notifications cannot be displayed.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasNotificationPermission) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFFEF4444).copy(alpha = 0.15f)
                            )
                    ) {
                        Icon(
                            imageVector = if (hasNotificationPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Notification Status",
                            tint = if (hasNotificationPermission) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CHECK-IN NOTIFICATION SYSTEM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (hasNotificationPermission) "System Alarms & Push Notifications Active" else "Permission Required for Push Reminders",
                            fontSize = 11.sp,
                            color = if (hasNotificationPermission) textSecondary else Color(0xFFEF4444)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasNotificationPermission) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (hasNotificationPermission) "ACTIVE 🟢" else "ACTION NEEDED ⚠️",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasNotificationPermission) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grant notification permission so the app can alert you at your attendance time.",
                            fontSize = 11.sp,
                            color = textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Grant Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (activeChallenges.isNotEmpty()) {
                val timesList = activeChallenges.joinToString(", ") { "${it.habitName} @ ${it.attendanceTime}" }
                Text(
                    text = "Scheduled Alarms: $timesList",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentPrimary
                )
            } else {
                Text(
                    text = "No active challenge alarms scheduled. Create a challenge above to start nightly attendance notifications.",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
fun PastChallengeItemCard(challenge: HabitChallenge, viewModel: HabitViewModel) {
    val context = LocalContext.current
    val cardBg = MaterialTheme.colorScheme.surface
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.habitName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${challenge.totalDays} Days Goal • ${challenge.getSuccessCount()} Passed • ${challenge.getFailedCount()} Slipped",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            IconButton(
                onClick = { viewModel.deleteChallenge(context, challenge.id) }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun CreateChallengeDialog(
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkMode
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surface

    var habitName by remember { mutableStateOf("") }
    var selectedPresetDays by remember { mutableStateOf(3) }
    var customDaysText by remember { mutableStateOf("") }
    var attendanceTime by remember { mutableStateOf("10:00 PM") }
    var pinToLockscreen by remember { mutableStateOf(true) }

    val presetDaysList = listOf(3, 5, 10, 30, 60, 90, 200)

    val habitSuggestions = listOf(
        "Not watching YouTube & social media",
        "No Pornography & Masturbation",
        "No Social Media after 10 PM",
        "No Alcohol & Smoking",
        "30 Minutes Daily Exercise",
        "Reading 20 Pages Daily"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create New Challenge 🏆",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )

                // Habit Selection Field
                Column {
                    Text(
                        text = "Which habit do you want to challenge?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = habitName,
                        onValueChange = { habitName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = { Text("e.g. Not watching YouTube & social media") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick suggestions chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(habitSuggestions) { sugg ->
                            SuggestionChip(
                                onClick = { habitName = sugg },
                                label = { Text(sugg, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Days Duration Selection
                Column {
                    Text(
                        text = "Challenge Duration (Days)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetDaysList) { days ->
                            val isSelected = selectedPresetDays == days && customDaysText.isEmpty()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPresetDays = days
                                    customDaysText = ""
                                },
                                label = { Text("$days Days", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Days Input
                    OutlinedTextField(
                        value = customDaysText,
                        onValueChange = { customDaysText = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = { Text("Or type custom days (e.g. 14, 100, 365)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // Nightly Attendance Time Selection
                Column {
                    Text(
                        text = "Nightly Attendance Check-in Time",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val times = listOf("9:00 PM", "10:00 PM", "10:30 PM", "11:00 PM")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        times.forEach { t ->
                            val isSelected = attendanceTime == t
                            FilterChip(
                                selected = isSelected,
                                onClick = { attendanceTime = t },
                                label = { Text(t, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Lockscreen Reminder Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pin to Lock Screen & Notifications",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Keep challenge visible as sticky reminder",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                    Switch(
                        checked = pinToLockscreen,
                        onCheckedChange = { pinToLockscreen = it }
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalDays = customDaysText.toIntOrNull() ?: selectedPresetDays
                            val cleanHabit = habitName.ifBlank { "Custom Habit Challenge" }
                            viewModel.createNewChallenge(
                                context = context,
                                habitName = cleanHabit,
                                totalDays = finalDays.coerceAtLeast(1),
                                attendanceTime = attendanceTime,
                                showLockscreen = pinToLockscreen
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentPrimary)
                    ) {
                        Text("Start Challenge 🚀", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeCheckInDialog(
    challenge: HabitChallenge,
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surface

    val currentDay = challenge.getCurrentDayNumber()
    var selectedSuccess by remember { mutableStateOf(true) }
    var notesText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = cardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = "Night Check-in",
                        tint = accentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nightly Attendance (${challenge.attendanceTime})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                }

                Text(
                    text = "Check-in for Day $currentDay of ${challenge.totalDays}: '${challenge.habitName}'",
                    fontSize = 13.sp,
                    color = textSecondary
                )

                Text(
                    text = "Did you follow your rule today for '${challenge.habitName}'?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                // Selection Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // YES Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSuccess = true }
                            .border(
                                width = if (selectedSuccess) 2.dp else 1.dp,
                                color = if (selectedSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedSuccess) Color(0xFF10B981).copy(alpha = 0.15f) else cardBg
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✅ YES", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Followed Rule", fontSize = 11.sp, color = textSecondary)
                        }
                    }

                    // NO Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSuccess = false }
                            .border(
                                width = if (!selectedSuccess) 2.dp else 1.dp,
                                color = if (!selectedSuccess) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!selectedSuccess) Color(0xFFEF4444).copy(alpha = 0.15f) else cardBg
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("❌ NO", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Slipped Today", fontSize = 11.sp, color = textSecondary)
                        }
                    }
                }

                // Reflection Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Optional reflection notes...") },
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.submitChallengeAttendance(
                                context = context,
                                challengeId = challenge.id,
                                dayNumber = currentDay,
                                isSuccess = selectedSuccess,
                                notes = notesText
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    ) {
                        Text("Submit Check-in", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
