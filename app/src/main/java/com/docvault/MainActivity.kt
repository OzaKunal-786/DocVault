// File: app/src/main/java/com/docvault/MainActivity.kt
// STATUS: REPLACE the entire file

package com.docvault

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.docvault.data.models.CategoryItem
import com.docvault.data.models.DocumentCategory
import com.docvault.ui.screens.HomeScreen
import com.docvault.ui.screens.LockScreen
import com.docvault.ui.screens.PinSetupScreen
import com.docvault.ui.theme.DocVaultTheme
import com.docvault.ui.viewmodel.AppScreen
import com.docvault.ui.viewmodel.AppViewModel
import com.docvault.util.FileUtil

/**
 * The one and only Activity in the entire app.
 *
 * WHY FragmentActivity?
 * Android's fingerprint popup (BiometricPrompt) requires it.
 * FragmentActivity is a type of Activity that supports this.
 * It works with Jetpack Compose just fine.
 */
class MainActivity : FragmentActivity() {

    // "by viewModels()" = Android creates the ViewModel for us
    // and keeps it alive even when the screen rotates
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Watch for app going to background / foreground ──
        // This is how auto-lock works:
        // App goes to background → start timer
        // App comes back → check if timer expired → lock if yes
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // User left the app (pressed home, switched apps, etc.)
                    viewModel.onAppPaused()
                    // Also clean up any temporary decrypted files
                    FileUtil.clearTempFiles(this)
                }
                Lifecycle.Event.ON_START -> {
                    // User came back to the app
                    viewModel.onAppResumed()
                }
                else -> { /* ignore other lifecycle events */ }
            }
        })

        // ── Set up the UI ───────────────────────────────────
        setContent {
            DocVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show different screen based on app state
                    when (val screen = viewModel.currentScreen) {

                        // ── FIRST TIME: Create a PIN ────────
                        is AppScreen.PinSetup -> {
                            PinSetupScreen(
                                onPinCreated = { pin ->
                                    viewModel.onPinCreated(pin)
                                },
                                onBiometricSetup = {
                                    viewModel.onBiometricEnabled()
                                }
                            )
                        }

                        // ── LOCKED: Enter PIN or use fingerprint ──
                        is AppScreen.Lock -> {
                            LockScreen(
                                onPinEntered = { pin ->
                                    viewModel.onPinEntered(pin)
                                },
                                onBiometricClick = {
                                    triggerBiometric()
                                },
                                showBiometric = viewModel.showBiometric,
                                pinError = viewModel.pinError,
                                isLoading = false
                            )
                        }

                        // ── UNLOCKED: Show the main app ─────
                        is AppScreen.Home -> {
                            HomeScreen(
                                categories = getPlaceholderCategories(),
                                recentDocuments = emptyList(),
                                onCategoryClick = { /* TODO: Week 2 */ },
                                onSearchClick = { /* TODO: Week 6 */ },
                                onAddClick = { /* TODO: Week 6 */ },
                                onDocumentClick = { /* TODO: Week 3 */ }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Show the fingerprint popup.
     */
    private fun triggerBiometric() {
        val app = application as DocVaultApplication
        app.biometricHelper.authenticate(
            activity = this,
            onSuccess = { _ ->
                // result contains crypto object if we used one
                viewModel.onBiometricSuccess()
            },
            onError = { error ->
                viewModel.onBiometricError(error)
            }
        )
    }

    /**
     * Placeholder category data for the home screen.
     * In Week 2+, this will come from the real database.
     *
     * For now, it just shows the 8 categories with zero documents.
     * This lets us test that the home screen looks right.
     */
    private fun getPlaceholderCategories(): List<CategoryItem> {
        return DocumentCategory.entries.map { category ->
            CategoryItem(
                emoji = category.emoji,
                name = category.displayName,
                count = 0
            )
        }
    }
}
