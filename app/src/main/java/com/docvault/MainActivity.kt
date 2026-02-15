// File: app/src/main/java/com/docvault/MainActivity.kt

package com.docvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.docvault.ui.navigation.AppNavGraph
import com.docvault.ui.navigation.Routes
import com.docvault.ui.screens.LockScreen
import com.docvault.ui.screens.OnboardingScreen
import com.docvault.ui.screens.PermissionScreen
import com.docvault.ui.screens.PinSetupScreen
import com.docvault.ui.theme.DocVaultTheme
import com.docvault.ui.viewmodel.AppScreen
import com.docvault.ui.viewmodel.AppViewModel
import com.docvault.util.FileUtil

class MainActivity : FragmentActivity() {

    private val viewModel: AppViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        viewModel.onPermissionsResult(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle incoming share intents when app starts
        handleIntent(intent)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    viewModel.onAppPaused()
                    FileUtil.clearTempFiles(this)
                }
                Lifecycle.Event.ON_START -> {
                    viewModel.onAppResumed()
                    viewModel.checkPermissions(this)
                }
                else -> {}
            }
        })

        setContent {
            DocVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (viewModel.currentScreen) {
                        is AppScreen.Onboarding -> {
                            OnboardingScreen(
                                onFinished = { viewModel.onOnboardingFinished() }
                            )
                        }

                        is AppScreen.PinSetup -> {
                            PinSetupScreen(
                                onPinCreated = { pin -> viewModel.onPinCreated(pin) },
                                onBiometricSetup = { viewModel.onBiometricEnabled() }
                            )
                        }

                        is AppScreen.Lock -> {
                            LockScreen(
                                onPinEntered = { pin -> viewModel.onPinEntered(pin) },
                                onBiometricClick = { triggerBiometric() },
                                showBiometric = viewModel.showBiometric,
                                pinError = viewModel.pinError,
                                isLoading = false
                            )
                        }

                        is AppScreen.Permission -> {
                            PermissionScreen(
                                onGrantClick = { launchPermissionRequest() }
                            )
                        }

                        is AppScreen.Home -> {
                            MainAppScaffold(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle share intents if the app is already open in background
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            uri?.let {
                // TODO: In Week 6 Day 5, we will trigger a "Quick Import" screen here
                // For now, we log it and ensure the logic is ready
            }
        }
    }

    private fun launchPermissionRequest() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions)
    }

    @Composable
    fun MainAppScaffold(viewModel: AppViewModel) {
        val navController = rememberNavController()
        
        val items = listOf(
            BottomNavItem("Docs", Routes.HOME, Icons.Default.Description),
            BottomNavItem("Search", Routes.SEARCH, Icons.Default.Search),
            BottomNavItem("Settings", Routes.SETTINGS, Icons.Default.Settings)
        )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.name) },
                            label = { Text(item.name) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AppNavGraph(navController = navController, appViewModel = viewModel)
            }
        }
    }

    private fun triggerBiometric() {
        val app = application as DocVaultApplication
        app.biometricHelper.authenticate(
            activity = this,
            onSuccess = { viewModel.onBiometricSuccess() },
            onError = { error -> viewModel.onBiometricError(error) }
        )
    }
}

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
