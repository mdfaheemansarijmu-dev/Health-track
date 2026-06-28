package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.window.DialogProperties
import com.example.HabitViewModel

@Composable
fun OptionalUpdateDialog(viewModel: HabitViewModel) {
    val info = viewModel.activeUpdateInfo ?: return
    val context = LocalContext.current
    val isDark = viewModel.isDarkMode

    val dialogBg = if (isDark) Color(0xFF131C2E) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderStrokeColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val accentPrimary = if (viewModel.accentColorName == "Emerald") Color(0xFF10B981) else Color(0xFF2DD4BF)

    Dialog(
        onDismissRequest = { viewModel.dismissOptionalUpdate(context) },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rocket Icon & Header
                Text(
                    text = "🚀 New Update Available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Custom message if present
                Text(
                    text = "Version ${info.latestVersion} is available!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Version display cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Version Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = dialogBg.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Current", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                            Text(text = viewModel.currentVersion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }

                    // Latest Version Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = dialogBg.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Latest", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                            Text(text = info.latestVersion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // What's New Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Later Button
                    OutlinedButton(
                        onClick = { viewModel.dismissOptionalUpdate(context) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, borderStrokeColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Later", fontWeight = FontWeight.Bold)
                    }

                    // Update Now Button
                    Button(
                        onClick = {
                            viewModel.dismissOptionalUpdate(context)
                            openDownloadUrl(context, info.apkUrl)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Update Now", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ForceUpdateScreen(viewModel: HabitViewModel) {
    val info = viewModel.activeUpdateInfo ?: return
    val context = LocalContext.current
    val isDark = viewModel.isDarkMode

    val background = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val accentPrimary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderStrokeColor, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium cloud download visual indicator
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = accentPrimary.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "System Update Required",
                            tint = accentPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Critical Update Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A mandatory newer version of the app is available. You must install the latest version to continue using Jagr.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Version display cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Version Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = background)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Current Version", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                            Text(text = viewModel.currentVersion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }

                    // Latest Version Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = background)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Latest Version", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                            Text(text = info.latestVersion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // What's New Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Release Notes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = info.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Only Update Now button (non-dismissible)
                Button(
                    onClick = { openDownloadUrl(context, info.apkUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

fun openDownloadUrl(context: Context, downloadUrl: String) {
    try {
        if (downloadUrl.isEmpty()) {
            Toast.makeText(context, "No download link available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        Toast.makeText(context, "Opening update download link...", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open update link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
