package com.docvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docvault.ui.theme.*
import com.docvault.data.models.CategoryItem
import com.docvault.data.repository.ImportStatus

/**
 * Dashboard screen showing only categories.
 * Optimized to fit on a single view where possible.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    categories: List<CategoryItem>,
    importStatus: ImportStatus,
    onCategoryClick: (String) -> Unit,
    onCategoryLongClick: (String) -> Unit,
    onAddCameraClick: () -> Unit,
    onAddFileClick: () -> Unit,
    onInitialImportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                        progress = { importStatus.current.toFloat() / importStatus.total.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Dashboard Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = true // Enabled in case of many custom categories
            ) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(categories) { cat ->
                    CategoryCard(
                        cat = cat,
                        onClick = { onCategoryClick(cat.name) },
                        onLongClick = { onCategoryLongClick(cat.name) }
                    )
                }

                if (categories.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(onClick = onInitialImportClick) {
                                Text("Scan phone for documents")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CategoryCard(cat: CategoryItem, onClick: () -> Unit, onLongClick: () -> Unit) {
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
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
