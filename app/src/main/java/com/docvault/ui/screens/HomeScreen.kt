package com.docvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docvault.ui.theme.*
import com.docvault.data.models.CategoryItem
import com.docvault.data.models.DocumentItem
import com.docvault.data.repository.ImportStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categories: List<CategoryItem>,
    recentDocuments: List<DocumentItem>,
    importStatus: ImportStatus,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAddCameraClick: () -> Unit,
    onAddFileClick: () -> Unit,
    onInitialImportClick: () -> Unit,
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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("DocVault", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onInitialImportClick) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Initial Scan")
                    }
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            // ── Background Progress Bar ──
            if (importStatus is ImportStatus.Progress) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Importing: ${importStatus.fileName}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${importStatus.current}/${importStatus.total}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = importStatus.current.toFloat() / importStatus.total.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            } else if (importStatus is ImportStatus.Success) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Import complete! ${importStatus.count} files added.",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── SEARCH BAR ──
                item(span = { GridItemSpan(2) }) {
                    OutlinedCard(
                        onClick = onSearchClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text("Search documents...", color = Color.Gray)
                        }
                    }
                }

                // ── CATEGORIES SECTION ──
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(categories) { cat ->
                    CategoryCard(cat = cat, onClick = { onCategoryClick(cat.name) })
                }

                // ── RECENT DOCUMENTS ──
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Recently Added",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                if (recentDocuments.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "No documents yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = onInitialImportClick) {
                                        Text("Scan phone for documents")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(recentDocuments, span = { GridItemSpan(2) }) { doc ->
                        DocumentRow(doc = doc, onClick = { onDocumentClick(doc.id) })
                    }
                }

                item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
            }
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
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().height(110.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg.copy(alpha = 0.15f))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bg.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = cat.emoji, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = cat.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${cat.count}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun DocumentRow(doc: DocumentItem, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.date} • ${doc.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
