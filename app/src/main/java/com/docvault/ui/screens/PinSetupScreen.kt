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
fun PinSetupScreen(
onPinCreated: (String) -> Unit,
onBiometricSetup: () -> Unit
) {
var step by remember { mutableStateOf(1) } // 1 = create, 2 = confirm
var pin1 by remember { mutableStateOf("") }
var pin2 by remember { mutableStateOf("") }
var error by remember { mutableStateOf<String?>(null) }
var pinCreated by remember { mutableStateOf(false) }
// Pulse animation for lock icon (subtle)
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
    if (pinCreated) return
    if (step == 1 && pin1.length < 6) {
        pin1 += d
        if (pin1.length == 6) {
            step = 2
        }
    } else if (step == 2 && pin2.length < 6) {
        pin2 += d
        if (pin2.length == 6) {
            if (pin2 == pin1) {
                onPinCreated(pin2)
                pinCreated = true
                error = null
            } else {
                error = "PINs don't match"
                // reset to step 1
                pin1 = ""
                pin2 = ""
                step = 1
            }
        }
    }
}

fun backspace() {
    if (pinCreated) return
    if (step == 2 && pin2.isNotEmpty()) {
        pin2 = pin2.dropLast(1)
    } else if (step == 1 && pin1.isNotEmpty()) {
        pin1 = pin1.dropLast(1)
    }
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

        // Header
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
                text = if (!pinCreated) {
                    if (step == 1) "Create a 6-digit PIN" else "Confirm your PIN"
                } else "Enable fingerprint unlock?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )
        }

        // Dots + error or biometric choice
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!pinCreated) {
                val length = if (step == 1) pin1.length else pin2.length
                PinDots(length = length)
                Spacer(Modifier.height(8.dp))
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                // Biometric question buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ElevatedButton(
                        onClick = onBiometricSetup,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Enable biometric",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Enable")
                    }
                    OutlinedButton(
                        onClick = { /* User chose to skip; handled by caller later */ },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Skip")
                    }
                }
            }
        }

        // Number pad (hidden after PIN created)
        if (!pinCreated) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                KeyRow(
                    labels = listOf("1", "2", "3"),
                    onClick = { addDigit(it.first()) }
                )
                KeyRow(
                    labels = listOf("4", "5", "6"),
                    onClick = { addDigit(it.first()) }
                )
                KeyRow(
                    labels = listOf("7", "8", "9"),
                    onClick = { addDigit(it.first()) }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(80.dp, 56.dp))
                    ElevatedButton(
                        onClick = { addDigit('0') },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(80.dp, 56.dp)
                    ) {
                        Text("0", style = MaterialTheme.typography.titleMedium)
                    }
                    ElevatedButton(
                        onClick = { backspace() },
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
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}
}

@Composable
private fun PinDots(length: Int) {
Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
repeat(6) { index ->
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
onClick: (String) -> Unit
) {
Row(
horizontalArrangement = Arrangement.spacedBy(12.dp),
verticalAlignment = Alignment.CenterVertically
) {
labels.forEach { label ->
ElevatedButton(
onClick = { onClick(label) },
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
