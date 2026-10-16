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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Circular Glowing Logo
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
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
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSignUpMode) "Create Firebase Account" else "Welcome to Jagr",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isSignUpMode) "Join Jagr with Firebase Authentication to sync habits across devices." else "Sign in with your Firebase account or Google to sync habits securely.",
                    fontSize = 13.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Firebase Authentication Form Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderStrokeColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Sign In / Sign Up Mode Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSignUpMode) accentPrimary else Color.Transparent)
                                .clickable {
                                    isSignUpMode = false
                                    localErrorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign In",
                                color = if (!isSignUpMode) Color.White else textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSignUpMode) accentPrimary else Color.Transparent)
                                .clickable {
                                    isSignUpMode = true
                                    localErrorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Register",
                                color = if (isSignUpMode) Color.White else textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Display Name (Only in Register mode)
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Alex Mercer") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Email Input Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("you@example.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Input Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error message callout if present
                    if (localErrorMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = localErrorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Primary Firebase Submit Button
                    Button(
                        onClick = {
                            localErrorMessage = null
                            if (isSignUpMode) {
                                viewModel.signUpWithFirebaseEmail(
                                    context = context,
                                    email = emailInput,
                                    pass = passwordInput,
                                    name = nameInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Welcome! Firebase account created successfully.", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        localErrorMessage = err
                                    }
                                )
                            } else {
                                viewModel.loginWithFirebaseEmail(
                                    context = context,
                                    email = emailInput,
                                    pass = passwordInput,
                                    onSuccess = {
                                        Toast.makeText(context, "Logged in as ${viewModel.userName}!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { err ->
                                        localErrorMessage = err
                                    }
                                )
                            }
                        },
                        enabled = !viewModel.isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentPrimary)
                    ) {
                        if (viewModel.isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticating...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = if (isSignUpMode) Icons.Default.PersonAdd else Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSignUpMode) "Create Firebase Account" else "Sign In with Firebase",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider & Alternative Methods
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = borderStrokeColor.copy(alpha = 0.5f))
                    Text(
                        text = "  OR  ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = borderStrokeColor.copy(alpha = 0.5f))
                }

                // Google Login Button
                OutlinedButton(
                    onClick = {
                        if (!viewModel.isAuthenticating) {
                            viewModel.authenticateWithGoogle(
                                context = context,
                                onSuccess = {
                                    Toast.makeText(context, "Successfully logged in as ${viewModel.userName}!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderStrokeColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val w = size.width
                            val h = size.height
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

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Continue with Google",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Guest mode button
                TextButton(
                    onClick = {
                        viewModel.loginAsGuestFirebase(
                            context = context,
                            onSuccess = {
                                Toast.makeText(context, "Welcome as Guest Visitor!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                Toast.makeText(context, "Guest mode activated.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Explore as Guest Visitor",
                            color = accentPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
}


