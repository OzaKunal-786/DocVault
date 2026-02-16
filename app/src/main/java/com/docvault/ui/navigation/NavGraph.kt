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
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val CATEGORY_DETAIL = "category_detail/{categoryName}"
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
    onAddMonitoredFolderClick: () -> Unit
) {
    val categories by appViewModel.categories.collectAsState()
    val recentDocsEntities by appViewModel.recentDocuments.collectAsState()
    val importStatus by DocVaultApplication.instance.importManager.importStatus.collectAsState()
    
    val recentDocuments = recentDocsEntities.map { entity ->
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
                recentDocuments = recentDocuments,
                importStatus = importStatus,
                onCategoryClick = { category -> 
                    navController.navigate("category_detail/$category")
                },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onAddCameraClick = onAddCameraClick,
                onAddFileClick = onAddFileClick,
                onInitialImportClick = onInitialImportClick,
                onDocumentClick = onDocumentClick
            )
        }
        
        composable(
            route = Routes.CATEGORY_DETAIL,
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
                onBackClick = { navController.popBackStack() },
                onDocumentClick = onDocumentClick
            )
        }
        
        composable(Routes.SEARCH) {
            val searchViewModel: SearchViewModel = viewModel(
                factory = SearchViewModelFactory(DocVaultApplication.instance.repository)
            )
            SearchScreen(
                viewModel = searchViewModel,
                onBackClick = { navController.popBackStack() },
                onDocumentClick = onDocumentClick
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = appViewModel,
                onBackupClick = onBackupClick,
                onRestoreClick = onRestoreClick,
                onBackClick = { navController.popBackStack() },
                onChangePinClick = { appViewModel.onChangePinRequested() },
                onAddMonitoredFolderClick = onAddMonitoredFolderClick
            )
        }
    }
}

class SearchViewModelFactory(private val repository: com.docvault.data.repository.DocumentRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(repository) as T
    }
}
