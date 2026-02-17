package com.docvault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.docvault.ui.screens.*
import com.docvault.ui.viewmodel.AppViewModel
import com.docvault.ui.viewmodel.SearchViewModel
import com.docvault.DocVaultApplication
import com.docvault.data.models.DocumentItem
import com.docvault.ui.viewmodel.AppScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Routes {
    const val HOME = "home"
    const val ALL_FILES = "all_files"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appViewModel: AppViewModel,
    onAddCameraClick: () -> Unit,
    onAddFileClick: () -> Unit,
    onInitialImportClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onAddMonitoredFolderClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDocumentLongClick: (DocumentItem) -> Unit,
    onCategoryLongClick: (String) -> Unit
) {
    val categories by appViewModel.categories.collectAsState()
    val filteredDocsEntities by appViewModel.filteredDocuments.collectAsState()
    val importStatus by DocVaultApplication.instance.importManager.importStatus.collectAsState()
    val selectedIds = appViewModel.selectedDocumentIds
    val isSelectMode = appViewModel.isSelectMode
    val searchQuery = appViewModel.searchQuery
    
    val displayDocuments = filteredDocsEntities.map { entity ->
        DocumentItem(
            id = entity.id,
            title = entity.effectiveTitle(),
            date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entity.importDate)),
            size = "${entity.fileSize / 1024} KB",
            category = entity.effectiveCategory()
        )
    }

    if (appViewModel.currentScreen == AppScreen.ChangePin) {
        ChangePinScreen(
            onPinChanged = { appViewModel.onPinChanged(it) },
            onBack = { appViewModel.onPinEntered("") } 
        )
        return
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                categories = categories,
                importStatus = importStatus,
                onCategoryClick = { category -> 
                    navController.navigate("category_detail/$category")
                },
                onCategoryLongClick = onCategoryLongClick,
                onAddCameraClick = onAddCameraClick,
                onAddFileClick = onAddFileClick,
                onInitialImportClick = onInitialImportClick,
                onSettingsClick = onSettingsClick,
                onNotificationsClick = { /* TODO */ }
            )
        }

        composable(Routes.ALL_FILES) {
            AllFilesScreen(
                documents = displayDocuments,
                selectedIds = selectedIds,
                isSelectMode = isSelectMode,
                onDocumentClick = onDocumentClick,
                onToggleSelection = { appViewModel.toggleSelection(it) },
                onDeleteSelected = { appViewModel.deleteSelectedDocuments() },
                onClearSelection = { appViewModel.clearSelection() },
                onSearchQueryChange = { appViewModel.onSearchQueryChange(it) },
                searchQuery = searchQuery,
                onDocumentLongClick = onDocumentLongClick
            )
        }
        
        composable(
            route = "category_detail/{categoryName}",
            arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val docsEntities by DocVaultApplication.instance.repository.getDocumentsByCategory(categoryName).collectAsState(initial = emptyList())
            
            val docs = docsEntities.map { entity ->
                DocumentItem(
                    id = entity.id,
                    title = entity.effectiveTitle(),
                    date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entity.importDate)),
                    size = "${entity.fileSize / 1024} KB",
                    category = entity.effectiveCategory()
                )
            }
            
            CategoryDetailScreen(
                categoryName = categoryName,
                documents = docs,
                selectedIds = selectedIds,
                isSelectMode = isSelectMode,
                onBackClick = { navController.popBackStack() },
                onDocumentClick = onDocumentClick,
                onToggleSelection = { appViewModel.toggleSelection(it) },
                onDeleteSelected = { appViewModel.deleteSelectedDocuments() },
                onClearSelection = { appViewModel.clearSelection() },
                onDocumentLongClick = onDocumentLongClick
            )
        }
    }
}

class SearchViewModelFactory(private val repository: com.docvault.data.repository.DocumentRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(repository) as T
    }
}
