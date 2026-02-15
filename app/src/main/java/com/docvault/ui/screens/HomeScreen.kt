package com.docvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docvault.ui.theme.*
import com.docvault.data.models.CategoryItem
import com.docvault.data.models.DocumentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categories: List<CategoryItem>,
    recentDocuments: List<DocumentItem>,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAddCameraClick: () -> Unit,
    onAddFileClick: () -> Unit,
    onDocumentClick: (String) -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("DocVault")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showAddMenu) {
                    SmallFloatingActionButton(
                        onClick = { 
                            showAddMenu = false
                            onAddFileClick() 
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Outlined.FileOpen, contentDescription = "Import File")
                    }
                    Spacer(Modifier.height(16.dp))
                    SmallFloatingActionButton(
                        onClick = { 
                            showAddMenu = false
                            onAddCameraClick() 
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Scan Document")
                    }
                    Spacer(Modifier.height(16.dp))
                }
                FloatingActionButton(
                    onClick = { showAddMenu = !showAddMenu },
                    containerColor = if (showAddMenu) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (showAddMenu) Icons.Outlined.Close else Icons.Outlined.Add,
                        contentDescription = "Add"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                // Search bar placeholder
                OutlinedTextField(
                    value = "",
                    onValueChange = { onSearchClick() },
                    readOnly = true,
                    placeholder = { Text("Search documents…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    enabled = true,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(categories) { cat ->
                CategoryCard(cat = cat, onClick = { onCategoryClick(cat.name) })
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Recently Added",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (recentDocuments.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No documents yet. Tap + to add one!", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                recentDocuments.forEach { doc ->
                    item(span = { GridItemSpan(2) }) {
                        DocumentRow(doc = doc, onClick = { onDocumentClick(doc.id) })
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CategoryCard(cat: CategoryItem, onClick: () -> Unit) {
    val bg = when (cat.name.lowercase()) {
        "id & personal", "id" -> CategoryId
        "financial" -> CategoryFinancial
        "receipts" -> CategoryReceipts
        "medical" -> CategoryMedical
        "education" -> CategoryEducation
        "vehicle" -> CategoryVehicle
        "property" -> CategoryProperty
        else -> CategoryOther
    }
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(96.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(text = cat.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(text = cat.name, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "${cat.count}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun DocumentRow(doc: DocumentItem, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = doc.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${doc.date} • ${doc.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
