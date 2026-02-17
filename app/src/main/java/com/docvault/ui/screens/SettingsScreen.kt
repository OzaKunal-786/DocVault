package com.docvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docvault.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onAddMonitoredFolderClick: () -> Unit
) {
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    val monitoredFolders by viewModel.monitoredFolders.collectAsState()

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            onCreateCategory = { name ->
                viewModel.createCategory(name, "📁")
                showCreateCategoryDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsGroup(title = "Security") {
                SettingsItemRow(
                    icon = Icons.Outlined.Lock,
                    title = "Change PIN",
                    subtitle = "Update your 6-digit security code",
                    onClick = onChangePinClick
                )
                SettingsItemRow(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face to unlock",
                    isToggled = viewModel.showBiometric,
                    onToggle = { viewModel.onBiometricEnabled() }
                )
            }

            SettingsGroup(title = "Customization") {
                SettingsItemRow(
                    icon = Icons.Outlined.CreateNewFolder,
                    title = "Create Category",
                    subtitle = "Add a custom folder for your documents",
                    onClick = { showCreateCategoryDialog = true }
                )
            }

            SettingsGroup(title = "Monitored Folders") {
                if (monitoredFolders.isEmpty()) {
                    Text(
                        "No folders added yet",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                monitoredFolders.forEach { uri ->
                    MonitoredFolderItem(uri, { viewModel.removeMonitoredFolder(uri) })
                }
                SettingsItemRow(
                    icon = Icons.Outlined.AddCircleOutline,
                    title = "Add Monitored Folder",
                    subtitle = "App will deep scan these for documents",
                    onClick = onAddMonitoredFolderClick
                )
            }

            SettingsGroup(title = "Scanning & Performance") {
                IgnoreFileSizeSettings(viewModel)
                SettingsItemRow(
                    icon = Icons.Outlined.Timer,
                    title = "Scan Interval",
                    subtitle = "Current: ${viewModel.scanIntervalHours} hours",
                    onClick = { /* Could open a picker dialog */ }
                )
            }

            SettingsGroup(title = "Data & Backup") {
                SettingsItemRow(
                    icon = Icons.Outlined.Backup,
                    title = "Backup Vault",
                    subtitle = "Export an encrypted ZIP of your documents",
                    onClick = onBackupClick
                )
                SettingsItemRow(
                    icon = Icons.Outlined.Restore,
                    title = "Restore Vault",
                    subtitle = "Import your documents from a backup",
                    onClick = onRestoreClick
                )
            }

            SettingsGroup(title = "About") {
                SettingsItemRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "DocVault v1.0.0 (Beta)",
                    onClick = {}
                )
                SettingsItemRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Reset App",
                    subtitle = "Permanently delete all data",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { /* TODO: Reset app logic */ }
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun CreateCategoryDialog(
    onDismissRequest: () -> Unit,
    onCreateCategory: (String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Create New Category") },
        text = {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("Category Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onCreateCategory(newCategoryName)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MonitoredFolderItem(
    folderUri: String,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = { Text(folderUri.substringAfterLast("%3A", "Folder"), fontSize = 14.sp) },
        supportingContent = { Text(folderUri, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        leadingContent = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun IgnoreFileSizeSettings(viewModel: AppViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.FilterAlt, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text("Ignore files smaller than: ${viewModel.minFileSizeKB} KB", style = MaterialTheme.typography.bodyMedium)
        }
        Slider(
            value = viewModel.minFileSizeKB.toFloat(),
            onValueChange = { viewModel.setMinFileSize(it.toInt()) },
            valueRange = 0f..1024f,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            letterSpacing = 1.sp
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    isToggled: Boolean? = null,
    onToggle: (Boolean) -> Unit = {},
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (titleColor == MaterialTheme.colorScheme.error) titleColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = titleColor)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isToggled != null) {
                Switch(
                    checked = isToggled,
                    onCheckedChange = onToggle,
                    thumbContent = if (isToggled) {
                        { Icon(Icons.Outlined.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                    } else null
                )
            }
        }
    }
}
