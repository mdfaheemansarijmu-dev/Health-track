package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(viewModel: HabitViewModel, onHabitAdded: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDark = viewModel.isDarkMode
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val borderStrokeColor = MaterialTheme.colorScheme.outline
    val cardBg = MaterialTheme.colorScheme.surface

    val hasUnsavedChanges = title.isNotBlank() || description.isNotBlank()
    val isSaveEnabled = title.trim().isNotEmpty()

    // 2. Support Android's system Back button/gesture
    BackHandler(enabled = true) {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onHabitAdded()
        }
    }

    // 4. Discard Changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "Discard Changes?",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes. Do you want to discard them?",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onHabitAdded() // Close the page without saving anything
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false }
                ) {
                    Text("Continue Editing")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp)
    ) {
        // 1. Top Bar with Back button in the top-left corner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = {
                    if (hasUnsavedChanges) {
                        showDiscardDialog = true
                    } else {
                        onHabitAdded()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Add New Habit",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Habit title input field
        Text(
            text = "Habit Title *",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Enter habit title...", color = textSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedBorderColor = accentPrimary,
                unfocusedBorderColor = borderStrokeColor,
                focusedContainerColor = if (isDark) Color(0xFF131C2E) else Color.White,
                unfocusedContainerColor = if (isDark) Color(0xFF131C2E) else Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Habit description input field
        Text(
            text = "Description",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Enter description (optional)...", color = textSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        // Save Habit Button
        Button(
            onClick = {
                if (isSaveEnabled) {
                    viewModel.addCustomHabit(title.trim(), description.trim())
                    onHabitAdded()
                }
            },
            enabled = isSaveEnabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentPrimary,
                disabledContainerColor = accentPrimary.copy(alpha = 0.3f),
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Save Habit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
