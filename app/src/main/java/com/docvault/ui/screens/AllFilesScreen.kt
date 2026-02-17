package com.docvault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.docvault.data.models.DocumentItem
import com.docvault.ui.components.DocumentRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFilesScreen(
    documents: List<DocumentItem>,
    selectedIds: List<String>,
    isSelectMode: Boolean,
    onDocumentClick: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onClearSelection: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    searchQuery: String,
    onDocumentLongClick: (DocumentItem) -> Unit
) {
    Scaffold(
        topBar = {
            if (isSelectMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    },
                    actions = {
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("All Files") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Integrated Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search all files...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Outlined.Clear, null)
                        }
                    }
                },
                singleLine = true
            )

            if (documents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents) { doc ->
                        DocumentRow(
                            doc = doc,
                            isSelected = selectedIds.contains(doc.id),
                            onClick = {
                                if (isSelectMode) onToggleSelection(doc.id)
                                else onDocumentClick(doc.id)
                            },
                            onLongClick = { onDocumentLongClick(doc) }
                        )
                    }
                }
            }
        }
    }
}
