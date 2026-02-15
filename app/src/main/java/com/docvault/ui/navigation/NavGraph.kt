package com.docvault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.docvault.ui.screens.HomeScreen
import com.docvault.ui.screens.SearchScreen
import com.docvault.ui.screens.SettingsScreen
import com.docvault.ui.viewmodel.AppViewModel
import com.docvault.ui.viewmodel.SearchViewModel
import com.docvault.DocVaultApplication

/**
 * Navigation routes for the app.
 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appViewModel: AppViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                categories = emptyList(), // TODO: Connect to VM in Week 8
                recentDocuments = emptyList(), // TODO: Connect to VM in Week 8
                onCategoryClick = { category -> 
                    // TODO: Navigate to category detail in Week 2
                },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onAddCameraClick = { /* TODO: Trigger Camera */ },
                onAddFileClick = { /* TODO: Trigger File Picker */ },
                onDocumentClick = { docId ->
                    // TODO: Navigate to viewer in Week 3
                }
            )
        }
        
        composable(Routes.SEARCH) {
            val searchViewModel: SearchViewModel = viewModel(
                factory = SearchViewModelFactory(DocVaultApplication.instance.repository)
            )
            SearchScreen(
                viewModel = searchViewModel,
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { docId -> /* Open viewer */ }
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = appViewModel,
                onBackupClick = { /* Trigger backup */ },
                onRestoreClick = { /* Trigger restore */ },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// Simple Factory for SearchViewModel since we aren't using Dagger/Hilt yet
class SearchViewModelFactory(private val repository: com.docvault.data.repository.DocumentRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(repository) as T
    }
}
