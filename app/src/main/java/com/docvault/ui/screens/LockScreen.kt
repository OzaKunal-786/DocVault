package com.docvault.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    onPinEntered: (String) -> Unit,
    onBiometricClick: () -> Unit,
    showBiometric: Boolean,
    pinError: String?,
    isLoading: Boolean
) {
    var inputPin by remember { mutableStateOf("") }
    
    // ✨ AUTO-TRIGGER BIOMETRIC POPUP ✨
    // This runs once when the screen first appears
    LaunchedEffect(Unit) {
    println("LockScreen: Attempting to trigger biometric...")
    onBiometricClick()
}
    
    // Clear PIN when an error is shown
    LaunchedEffect(pinError) {
        if (pinError != null) inputPin = ""
    }

    // Pulse animation for lock icon
    val pulse = rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        ).value

    fun addDigit(d: Char) {
        if (isLoading) return
        if (inputPin.length < 4) {
            inputPin += d
            if (inputPin.length == 4) {
                onPinEntered(inputPin)
            }
        }
    }

    fun backspace() {
        if (isLoading) return
        if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Header with lock icon and title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulse)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "DocVault",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Enter your PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            }

            // PIN dots + error + loader
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PinDots(length = inputPin.length)
                Spacer(Modifier.height(8.dp))
                if (pinError != null) {
                    Text(
                        text = pinError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isLoading) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }

            // Number pad
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                KeyRow(
                    labels = listOf("1", "2", "3"),
                    onClick = { addDigit(it.first()) },
                    enabled = !isLoading
                )
                KeyRow(
                    labels = listOf("4", "5", "6"),
                    onClick = { addDigit(it.first()) },
                    enabled = !isLoading
                )
                KeyRow(
                    labels = listOf("7", "8", "9"),
                    onClick = { addDigit(it.first()) },
                    enabled = !isLoading
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Biometric button (left)
                    if (showBiometric) {
                        ElevatedButton(
                            onClick = onBiometricClick,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(80.dp, 56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = "Biometric",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(80.dp, 56.dp))
                    }

                    // Zero
                    ElevatedButton(
                        onClick = { addDigit('0') },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(80.dp, 56.dp)
                    ) {
                        Text("0", style = MaterialTheme.typography.titleMedium)
                    }

                    // Backspace
                    ElevatedButton(
                        onClick = { backspace() },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(80.dp, 56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Backspace,
                            contentDescription = "Backspace"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDots(length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { index ->
            val filled = index < length
            Surface(
                shape = CircleShape,
                color = if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                tonalElevation = if (filled) 2.dp else 0.dp,
                modifier = Modifier.size(16.dp)
            ) {}
        }
    }
}

@Composable
private fun KeyRow(
    labels: List<String>,
    onClick: (String) -> Unit,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEach { label ->
            ElevatedButton(
                onClick = { onClick(label) },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(80.dp, 56.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}