package com.docvault.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.foundation.lazy.grid.GridItemSpan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categories: List<CategoryItem>,
    recentDocuments: List<DocumentItem>,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit,
    onDocumentClick: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Docs, 1=Search, 2=Settings
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
                    IconButton(onClick = { /* Notifications later */ }) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { /* Settings wired via NavGraph later */ }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Docs") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; onSearchClick() },
                    label = { Text("Search") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 /* TODO: Wire to settings navigation */ },
                    label = { Text("Settings") },
                    icon = {}
                )
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
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Search documents…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    enabled = true,
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
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
                    style = MaterialTheme.typography.titleMedium
                )
            }

            recentDocuments.forEach { doc ->
                item(span = { GridItemSpan(2) }) {
                    DocumentRow(doc = doc, onClick = { onDocumentClick(doc.id) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) } // bottom padding
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
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant ?: Color.LightGray)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.date} • ${doc.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}