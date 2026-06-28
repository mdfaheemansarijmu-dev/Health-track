package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val isDark = viewModel.isDarkMode
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary

    val pages = listOf(
        OnboardingPageData(
            title = "Meet Jagr",
            subtitle = "Your mindful companion for positive habits, physical activity tracking, and conscious daily reflection.",
            illustration = { StepTrackerAnimation(accentPrimary) }
        ),
        OnboardingPageData(
            title = "Track Habits & Milestones",
            subtitle = "Visualize your progress, daily streaks, step count, and healthy lifestyle choices in elegant responsive charts.",
            illustration = { AnalyticsAnimation(accentPrimary) }
        ),
        OnboardingPageData(
            title = "Reflect on What Matters",
            subtitle = "Securely record your feelings, avoid relapses, build long-term wellness, and live with clarity.",
            illustration = { ShieldReflectionAnimation(accentPrimary) }
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR with Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding(context)
                    }
                ) {
                    Text(
                        text = "Skip",
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // ILLUSTRATION & CONTENT (Animated crossfade for smoothness)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val page = pages[currentPage]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .height(240.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        page.illustration()
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = page.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = page.subtitle,
                        fontSize = 15.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // BOTTOM CONTROL ROW (Page Indicators & Next/Get Started)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) accentPrimary
                                    else textSecondary.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Action Button
                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) {
                            currentPage++
                        } else {
                            viewModel.completeOnboarding(context)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (currentPage == pages.lastIndex) "Get Started" else "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val illustration: @Composable () -> Unit
)

// ANIMATED CUSTOM GRAPHICS
@Composable
fun StepTrackerAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "step_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(scaleFactor)
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            // Draw standard modern gauge backing
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(10f, 10f),
                size = Size(size.width - 20f, size.height - 20f),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )
            // Draw active arc green
            drawArc(
                color = Color(0xFF10B981),
                startAngle = 135f,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = Offset(10f, 10f),
                size = Size(size.width - 20f, size.height - 20f),
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )
        }
        
        // Rotating purple orbital point representation
        Box(
            modifier = Modifier
                .size(160.dp)
                .rotate(rotation),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        // Centered Shoe and activity data representation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "7,235",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Steps",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnalyticsAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "analytics_animation")
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier
            .size(180.dp)
            .padding(12.dp)
        ) {
            val width = size.width
            val height = size.height

            // Grid lines backing
            for (i in 1..4) {
                val y = height * (i / 5f)
                drawLine(
                    color = color.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 3f
                )
            }

            // Draw orange bars
            val barWidth = 14f
            val spacing = 18f
            val startX = 20f
            val barHeights = listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.9f, 0.7f, 0.5f, 0.3f)

            barHeights.forEachIndexed { index, ratio ->
                val x = startX + index * (barWidth + spacing)
                val barH = height * ratio * 0.7f
                drawRoundRect(
                    color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                    topLeft = Offset(x, height - barH),
                    size = Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }

            // Beautiful curving sine wave for activity pulse
            val path = Path()
            path.moveTo(0f, height * 0.4f)
            for (x in 0..width.toInt()) {
                val angle = (x.toFloat() / width) * 2f * Math.PI + (pulseOffset / 100f) * 2f * Math.PI
                val y = height * 0.4f + Math.sin(angle).toFloat() * 18f
                path.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun ShieldReflectionAnimation(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_animation")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        // Glowing Background Circles
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(waveScale)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(waveScale * 0.9f)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
        )

        // Shield Card
        Card(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.Center)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

// LOGIN SCREEN IMPLEMENTATION
@Composable
fun LoginScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showGoogleChooser by remember { mutableStateOf(false) }

    val isDark = viewModel.isDarkMode
    val background = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val accentPrimary = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surface
    val borderStrokeColor = MaterialTheme.colorScheme.outline

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)
            ) {
                // Circular Glowing Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accentPrimary, Color(0xFF10B981))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterVintage,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Welcome to Jagr",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your daily companion for positive habits, physical tracking, & mindful reflection.",
                    fontSize = 14.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Authentication options card
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Google Login Button
                Card(
                    onClick = {
                        if (!viewModel.isAuthenticating) {
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
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (viewModel.isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Connecting securely...",
                                color = textPrimary.copy(alpha = 0.7f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // Styled multi-color Google "G" representation inside Canvas
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val w = size.width
                                val h = size.height
                                // Red segment path
                                val redPath = Path().apply {
                                    moveTo(w * 0.5f, h * 0.5f)
                                    lineTo(w * 0.95f, h * 0.35f)
                                    arcTo(
                                        rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                                        startAngleDegrees = -18f,
                                        sweepAngleDegrees = -135f,
                                        forceMoveTo = false
                                    )
                                    close()
                                }
                                drawPath(redPath, Color(0xFFEA4335))

                                // Yellow segment path
                                val yellowPath = Path().apply {
                                    moveTo(w * 0.5f, h * 0.5f)
                                    arcTo(
                                        rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                                        startAngleDegrees = -153f,
                                        sweepAngleDegrees = -72f,
                                        forceMoveTo = true
                                    )
                                    close()
                                }
                                drawPath(yellowPath, Color(0xFFFBBC05))

                                // Green segment path
                                val greenPath = Path().apply {
                                    moveTo(w * 0.5f, h * 0.5f)
                                    arcTo(
                                        rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                                        startAngleDegrees = -225f,
                                        sweepAngleDegrees = -70f,
                                        forceMoveTo = true
                                    )
                                    close()
                                }
                                drawPath(greenPath, Color(0xFF34A853))

                                // Blue segment path
                                val bluePath = Path().apply {
                                    moveTo(w * 0.5f, h * 0.5f)
                                    lineTo(w * 0.95f, h * 0.5f)
                                    arcTo(
                                        rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                                        startAngleDegrees = 0f,
                                        sweepAngleDegrees = -65f,
                                        forceMoveTo = false
                                    )
                                    close()
                                }
                                drawPath(bluePath, Color(0xFF4285F4))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Continue with Google",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Guest mode button
                Card(
                    onClick = {
                        viewModel.loginAsGuest(context)
                        Toast.makeText(context, "Welcome as Guest Visitor!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(
                            width = 1.dp,
                            color = accentPrimary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = accentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Explore as Guest",
                            color = accentPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Legal notice disclaimer
            Text(
                text = "By continuing, you agree to Jagr's Terms of Service & Privacy Policy.",
                fontSize = 11.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }

    // Interactive custom Google accounts dialog chooser
    if (showGoogleChooser) {
        GoogleAccountChooserDialog(
            viewModel = viewModel,
            onDismiss = { showGoogleChooser = false },
            onAccountSelected = { name, email ->
                showGoogleChooser = false
                viewModel.loginWithGoogle(context, email, name)
                Toast.makeText(context, "Successfully logged in as $name!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun GoogleAccountChooserDialog(
    viewModel: HabitViewModel,
    onDismiss: () -> Unit,
    onAccountSelected: (name: String, email: String) -> Unit
) {
    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var isAddingCustom by remember { mutableStateOf(false) }

    val accentPrimary = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = accentPrimary
                )
                Text(
                    text = "Choose an account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "to continue to Jagr app",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isAddingCustom) {
                    // Predefined mock Google account options
                    GoogleAccountRow(
                        name = "Jerome S.",
                        email = "jerome.steps@gmail.com",
                        avatarColor = Color(0xFF6C63FF),
                        onClick = { onAccountSelected("Jerome", "jerome.steps@gmail.com") }
                    )

                    GoogleAccountRow(
                        name = "Alex Mercer",
                        email = "alex.mercer@gmail.com",
                        avatarColor = Color(0xFF10B981),
                        onClick = { onAccountSelected("Alex", "alex.mercer@gmail.com") }
                    )

                    // Button to add another custom account
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAddingCustom = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Use another account",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Fields to enter custom Google account details
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customEmail,
                        onValueChange = { customEmail = it },
                        label = { Text("Google Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { isAddingCustom = false }) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                if (customName.isNotBlank() && customEmail.isNotBlank()) {
                                    onAccountSelected(customName, customEmail)
                                } else {
                                    onAccountSelected("Jerome", "jerome.steps@gmail.com")
                                }
                            }
                        ) {
                            Text("Sign In")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (!isAddingCustom) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun GoogleAccountRow(
    name: String,
    email: String,
    avatarColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Column {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = email,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
