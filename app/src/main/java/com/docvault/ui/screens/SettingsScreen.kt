package com.docvault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.docvault.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategory("Security")
            SettingsItem(
                icon = Icons.Outlined.Lock,
                title = "Change PIN",
                subtitle = "Update your 4-digit security code",
                onClick = { /* TODO: Week 7 Day 7 */ }
            )
            SettingsItem(
                icon = Icons.Outlined.Fingerprint,
                title = "Biometric Unlock",
                subtitle = "Use fingerprint or face to unlock",
                isToggled = viewModel.showBiometric,
                onToggle = { viewModel.onBiometricEnabled() }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsCategory("Data & Backup")
            SettingsItem(
                icon = Icons.Outlined.Backup,
                title = "Backup Vault",
                subtitle = "Export an encrypted ZIP of your documents",
                onClick = onBackupClick
            )
            SettingsItem(
                icon = Icons.Outlined.Restore,
                title = "Restore Vault",
                subtitle = "Import your documents from a backup",
                onClick = onRestoreClick
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsCategory("App Info")
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Version",
                subtitle = "DocVault v1.0.0 (Beta)",
                onClick = {}
            )
            
            Spacer(Modifier.height(32.dp))
            
            TextButton(
                onClick = { /* TODO: Delete all logic */ },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete All Data & Reset App")
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    isToggled: Boolean? = null,
    onToggle: (Boolean) -> Unit = {}
) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isToggled != null) {
                Switch(checked = isToggled, onCheckedChange = onToggle)
            }
        }
    }
}
