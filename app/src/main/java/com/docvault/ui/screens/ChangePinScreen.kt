package com.docvault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(
    onPinChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: New, 2: Confirm
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun addDigit(d: Char) {
        if (step == 1 && pin1.length < 6) {
            pin1 += d
            if (pin1.length == 6) step = 2
        } else if (step == 2 && pin2.length < 6) {
            pin2 += d
            if (pin2.length == 6) {
                if (pin1 == pin2) {
                    onPinChanged(pin1)
                } else {
                    error = "PINs do not match"
                    pin1 = ""
                    pin2 = ""
                    step = 1
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change PIN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (step == 1) "Enter New 6-Digit PIN" else "Confirm New PIN",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Reusing PinDots logic or simple visual
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val length = if (step == 1) pin1.length else pin2.length
                repeat(6) { i ->
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (i < length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(Modifier.height(48.dp))

            // Simplified keypad for this screen
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("","0","C")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { label ->
                            if (label.isEmpty()) {
                                Spacer(Modifier.size(64.dp))
                            } else {
                                Button(
                                    onClick = { 
                                        if (label == "C") {
                                            if (step == 2 && pin2.isNotEmpty()) pin2 = pin2.dropLast(1)
                                            else if (step == 1 && pin1.isNotEmpty()) pin1 = pin1.dropLast(1)
                                        } else addDigit(label[0])
                                    },
                                    modifier = Modifier.size(64.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
