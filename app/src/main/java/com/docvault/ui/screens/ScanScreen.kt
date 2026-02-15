package com.docvault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docvault.scanner.FileScanner
import com.docvault.ui.viewmodel.ScanViewModel

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onImportClick: (List<FileScanner.ScannedFile>) -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Scan for Documents") }
            )
        },
        floatingActionButton = {
            if (scanState.scannedFiles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onImportClick(scanState.scannedFiles) },
                    text = { Text("Import ${scanState.scannedFiles.size} Documents") },
                    icon = { /* No icon needed for now */ }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress Bar
            if (scanState.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Scanning... Found ${scanState.scannedFiles.size} potential documents",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                if (scanState.scannedFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No new documents found.")
                    }
                } else {
                    Text(
                        text = "Found ${scanState.scannedFiles.size} new documents. Ready to import.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Results List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(scanState.scannedFiles) { file ->
                    ScannedFileRow(file)
                }
            }
        }
    }
}

@Composable
fun ScannedFileRow(file: FileScanner.ScannedFile) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge)
                Text(file.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${(file.size / 1024)} KB",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
