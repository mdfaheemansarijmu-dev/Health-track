package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppUsageScreen(
    viewModel: HabitViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    val background = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val accentPrimary = Color(0xFF3B82F6)
    val borderStrokeColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: App Usage, 1: Browser & Adult Guard
    var limitDialogApp by remember { mutableStateOf<AppUsageInfo?>(null) }
    var showAddDomainDialog by remember { mutableStateOf(false) }
    var newCustomDomainText by remember { mutableStateOf("") }
    var testSimulatorUrl by remember { mutableStateOf("") }
    var testSimulatorResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel.selectedUsageTimeFrame) {
        viewModel.refreshAppUsageSummary(context)
        viewModel.refreshBrowserProtectionStatus(context)
    }

    val summary = viewModel.appUsageSummaryState ?: remember {
        AppUsageTracker.getUsageSummary(context, viewModel.selectedUsageTimeFrame)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onBack != null) {
                                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textPrimary)
                                }
                            }
                            Text(
                                text = "Device & Browser Tracking",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "Monitor device apps, screen time & protect against adult content",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.refreshAppUsageSummary(context)
                            viewModel.refreshBrowserProtectionStatus(context)
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentPrimary.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = accentPrimary)
                    }
                }
            }

            // Tab Selector: App Screen Time vs Browser Adult Site Guard
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tab 0
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = 0 },
                        color = if (selectedTab == 0) accentPrimary else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.White else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "App Screen Time",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedTab == 0) Color.White else textSecondary
                            )
                        }
                    }

                    // Tab 1
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedTab = 1
                                viewModel.refreshBrowserProtectionStatus(context)
                            },
                        color = if (selectedTab == 1) Color(0xFF8B5CF6) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Adult Site Guard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedTab == 1) Color.White else textSecondary
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // ==================== APP USAGE TAB ====================
                // Usage Access Permission Banner
                if (!summary.hasUsagePermission) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Usage Access Permission Required",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "Grant permission in system Settings to display real-time screen time and exact app statistics.",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { AppUsageTracker.openUsageAccessSettings(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                ) {
                                    Text("Grant Usage Access in Settings ⚙️", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Screen Time Summary Hero Card
                item {
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
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Timeframe Pills Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (summary.hasUsagePermission) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (summary.hasUsagePermission) "LIVE TRACKING 🟢" else "PERMISSION NEEDED ⚠️",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (summary.hasUsagePermission) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    UsageTimeFrame.values().forEach { tf ->
                                        val isSelected = viewModel.selectedUsageTimeFrame == tf
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    viewModel.selectedUsageTimeFrame = tf
                                                    viewModel.refreshAppUsageSummary(context)
                                                },
                                            color = if (isSelected) accentPrimary else background,
                                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
                                        ) {
                                            Text(
                                                text = tf.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else textSecondary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Total Screen Time Big Stat
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Total Screen Time",
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = summary.getFormattedTotalScreenTime(),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary
                                )
                                Text(
                                    text = "${summary.topApps.size} apps tracked during this timeframe",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                // Categories Breakdown
                if (summary.categories.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Category Breakdown",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )

                                summary.categories.forEach { cat ->
                                    CategoryUsageRow(cat, isDark, textPrimary, textSecondary)
                                }
                            }
                        }
                    }
                }

                // App List Header
                item {
                    Text(
                        text = "Tracked Applications",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (summary.topApps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = textSecondary, modifier = Modifier.size(36.dp))
                                Text("No app usage recorded yet", color = textPrimary, fontWeight = FontWeight.Bold)
                                Text("Use apps on your device to start tracking screen time stats.", color = textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(summary.topApps) { app ->
                        AppUsageItemCard(
                            app = app,
                            totalScreenTimeMillis = summary.totalScreenTimeMillis,
                            isDark = isDark,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentPrimary = accentPrimary,
                            borderStrokeColor = borderStrokeColor,
                            onSetLimit = { limitDialogApp = app }
                        )
                    }
                }
            } else {
                // ==================== BROWSER & ADULT GUARD TAB ====================
                // Accessibility Service Banner / Toggle
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.5.dp,
                                if (viewModel.isAccessibilityGuardActive) Color(0xFF10B981) else Color(0xFF8B5CF6),
                                RoundedCornerShape(20.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.isAccessibilityGuardActive) {
                                if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
                            } else {
                                if (isDark) Color(0xFF3B0764) else Color(0xFFF5F3FF)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isAccessibilityGuardActive) Icons.Default.CheckCircle else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (viewModel.isAccessibilityGuardActive) Color(0xFF10B981) else Color(0xFF8B5CF6),
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (viewModel.isAccessibilityGuardActive) "Live Browser Guard Active 🛡️" else "Accessibility Guard Required",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (viewModel.isAccessibilityGuardActive) {
                                            if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                        } else {
                                            if (isDark) Color(0xFFE9D5FF) else Color(0xFF581C87)
                                        }
                                    )
                                    Text(
                                        text = if (viewModel.isAccessibilityGuardActive) {
                                            "Actively inspecting browser address bars (Chrome, Firefox, Edge, Opera, Samsung) for adult content & triggers."
                                        } else {
                                            "Enable Jagr in Accessibility Settings to monitor URLs in browsers and shield against adult websites in real-time."
                                        },
                                        fontSize = 12.sp,
                                        color = if (viewModel.isAccessibilityGuardActive) {
                                            if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                        } else {
                                            if (isDark) Color(0xFFD8B4FE) else Color(0xFF6B21A8)
                                        }
                                    )
                                }
                            }

                            if (!viewModel.isAccessibilityGuardActive) {
                                Button(
                                    onClick = { AdultContentGuardManager.openAccessibilitySettings(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                ) {
                                    Text("Enable Accessibility Guard in Settings ⚙️", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Android Sandboxing & Architecture Notice
                item {
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
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = accentPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Android Privacy & Sandboxing Architecture",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Modern Android protects browser databases (Chrome, Firefox) with sandboxing. Apps cannot read raw browser files in background. Instead, Jagr uses the standard Accessibility Service to monitor active address bars and block adult domains on-screen in real time.",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Guard Settings Controls
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Guard Configuration",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )

                            // Master Shield Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Adult Content Shield", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
                                    Text("Detect and log adult domains & URLs", fontSize = 11.sp, color = textSecondary)
                                }
                                Switch(
                                    checked = viewModel.isBrowserProtectionEnabled,
                                    onCheckedChange = { viewModel.toggleBrowserProtection(context, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8B5CF6))
                                )
                            }

                            // High Sensitivity Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("High Sensitivity Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textPrimary)
                                    Text("Also scan search terms and explicit URL query parameters", fontSize = 11.sp, color = textSecondary)
                                }
                                Switch(
                                    checked = viewModel.isHighSensitivityMode,
                                    onCheckedChange = { viewModel.toggleHighSensitivity(context, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8B5CF6))
                                )
                            }
                        }
                    }
                }

                // Real-time Test Simulator
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF8B5CF6))
                                Text(
                                    text = "URL & Content Detection Tester",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                            Text(
                                text = "Test a sample domain or search term to verify how Jagr detects and logs adult triggers.",
                                fontSize = 11.sp,
                                color = textSecondary
                            )

                            OutlinedTextField(
                                value = testSimulatorUrl,
                                onValueChange = {
                                    testSimulatorUrl = it
                                    testSimulatorResult = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. test-adult-site.com or pornhub.com", fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    val match = viewModel.testAdultUrlDetection(context, testSimulatorUrl)
                                    testSimulatorResult = if (match != null && match.first) {
                                        "🚨 MATCH DETECTED: ${match.second}"
                                    } else {
                                        "✅ SAFE: No adult triggers detected for this input."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                enabled = testSimulatorUrl.isNotBlank()
                            ) {
                                Text("Run Detector Test 🔍", fontWeight = FontWeight.Bold)
                            }

                            if (testSimulatorResult != null) {
                                val isFlagged = testSimulatorResult!!.contains("MATCH DETECTED")
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isFlagged) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = testSimulatorResult!!,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isFlagged) Color(0xFFEF4444) else Color(0xFF10B981),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Blocked Domains Management
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Custom Blocked Domains", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textPrimary)
                                    Text("Add personal trigger websites to block list", fontSize = 11.sp, color = textSecondary)
                                }
                                IconButton(
                                    onClick = { showAddDomainDialog = true },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Domain", tint = Color(0xFF8B5CF6))
                                }
                            }

                            if (viewModel.customBlockedDomainsList.isEmpty()) {
                                Text(
                                    text = "No custom domains added. Core database of 40+ major adult platforms is active.",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    viewModel.customBlockedDomainsList.forEach { domain ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(background)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(domain, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                                            IconButton(
                                                onClick = { viewModel.removeCustomBlockedDomain(context, domain) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Detected Incidents & History Log
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Incidents Log (${viewModel.adultIncidentLogs.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        if (viewModel.adultIncidentLogs.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearAdultIncidents(context) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Log", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (viewModel.adultIncidentLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                                Text("Clean Record! No adult site visits detected", color = textPrimary, fontWeight = FontWeight.Bold)
                                Text("When adult sites or explicit search queries are detected in any browser, they will appear here.", color = textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(viewModel.adultIncidentLogs) { incident ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = incident.browserName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textPrimary
                                        )
                                    }
                                    Text(
                                        text = incident.getFormattedTime(),
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }

                                Text(
                                    text = incident.detectedUrlOrTerm,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEF4444)
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${incident.matchedPattern} • ${incident.actionTaken}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Custom Domain Dialog
        if (showAddDomainDialog) {
            AlertDialog(
                onDismissRequest = { showAddDomainDialog = false },
                title = { Text("Add Custom Blocked Domain", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter a website domain to flag and protect against:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = newCustomDomainText,
                            onValueChange = { newCustomDomainText = it },
                            placeholder = { Text("e.g. distracting-site.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCustomDomainText.isNotBlank()) {
                                viewModel.addCustomBlockedDomain(context, newCustomDomainText)
                                newCustomDomainText = ""
                                showAddDomainDialog = false
                            }
                        }
                    ) {
                        Text("Add Domain")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDomainDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Daily Limit Dialog for App Usage
        if (limitDialogApp != null) {
            val app = limitDialogApp!!
            var limitMinutesInput by remember { mutableStateOf(if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes.toString() else "30") }

            AlertDialog(
                onDismissRequest = { limitDialogApp = null },
                title = { Text("Set Daily Limit for ${app.appName}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Set daily max screen time limit (in minutes):", fontSize = 13.sp)
                        OutlinedTextField(
                            value = limitMinutesInput,
                            onValueChange = { limitMinutesInput = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text("Minutes, e.g. 45") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 60, 120).forEach { mins ->
                                Button(
                                    onClick = { limitMinutesInput = mins.toString() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("${mins}m", fontSize = 11.sp, color = textPrimary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val mins = limitMinutesInput.toIntOrNull() ?: 0
                            viewModel.setAppDailyLimit(context, app.packageName, mins)
                            limitDialogApp = null
                        }
                    ) {
                        Text("Save Limit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.setAppDailyLimit(context, app.packageName, 0)
                        limitDialogApp = null
                    }) {
                        Text("Remove Limit", color = Color(0xFFEF4444))
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryUsageRow(
    category: UsageCategorySummary,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val catColor = getCategoryColor(category.category)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = getCategoryIcon(category.category),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${category.category} (${category.appCount} apps)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )
            }
            Text(
                text = category.getFormattedDuration(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }
        LinearProgressIndicator(
            progress = { (category.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = catColor,
            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun AppUsageItemCard(
    app: AppUsageInfo,
    totalScreenTimeMillis: Long,
    isDark: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentPrimary: Color,
    borderStrokeColor: Color,
    onSetLimit: () -> Unit
) {
    val usageRatio = if (totalScreenTimeMillis > 0) (app.totalTimeInForegroundMillis.toFloat() / totalScreenTimeMillis.toFloat()) else 0f
    val limitMinutes = app.dailyLimitMinutes
    val usedMinutes = (app.totalTimeInForegroundMillis / 1000 / 60).toInt()
    val isExceeded = limitMinutes in 1..usedMinutes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isExceeded) Color(0xFFEF4444) else borderStrokeColor,
                RoundedCornerShape(16.dp)
            )
            .clickable { onSetLimit() },
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppIconImage(drawable = app.icon, fallbackCategory = app.category)
                    Column {
                        Text(
                            text = app.appName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = app.category,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = app.getFormattedDuration(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isExceeded) Color(0xFFEF4444) else textPrimary
                    )

                    if (limitMinutes > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isExceeded) Color(0xFFEF4444).copy(alpha = 0.15f) else accentPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isExceeded) "Limit Exceeded ($limitMinutes m)" else "Limit: $limitMinutes m",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExceeded) Color(0xFFEF4444) else accentPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { usageRatio.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isExceeded) Color(0xFFEF4444) else getCategoryColor(app.category),
                trackColor = borderStrokeColor.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun AppIconImage(drawable: Drawable?, fallbackCategory: String) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            drawableToBitmap(drawable)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            return
        }
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(getCategoryColor(fallbackCategory).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(fallbackCategory),
            contentDescription = null,
            tint = getCategoryColor(fallbackCategory),
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    try {
        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtMost(128),
            drawable.intrinsicHeight.coerceAtMost(128),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    } catch (e: Exception) {
        return null
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Social Media" -> Icons.Default.Share
        "Entertainment" -> Icons.Default.Movie
        "Productivity & Tools" -> Icons.Default.Work
        "Games" -> Icons.Default.Games
        else -> Icons.Default.Category
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Social Media" -> Color(0xFFEC4899) // Pink
        "Entertainment" -> Color(0xFF8B5CF6) // Purple
        "Productivity & Tools" -> Color(0xFF3B82F6) // Blue
        "Games" -> Color(0xFF10B981) // Emerald Green
        else -> Color(0xFFF59E0B) // Amber
    }
}
