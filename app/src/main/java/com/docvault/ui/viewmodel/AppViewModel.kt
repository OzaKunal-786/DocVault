// File: app/src/main/java/com/docvault/ui/viewmodel/AppViewModel.kt
// STATUS: BRAND NEW FILE — create it

package com.docvault.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.docvault.DocVaultApplication

/**
 * The "brain" of the app.
 *
 * It keeps track of what screen should be showing
 * and handles all the logic for locking/unlocking.
 *
 * "sealed class" = a fixed list of options. The screen can ONLY be
 * one of these three. Nothing else. This prevents bugs.
 */
sealed class AppScreen {
    /** First-time user: needs to create a PIN */
    object PinSetup : AppScreen()

    /** Returning user: needs to unlock with PIN or fingerprint */
    object Lock : AppScreen()

    /** User is in: show the main app */
    object Home : AppScreen()
}

class AppViewModel : ViewModel() {

    // Get references to the security tools
    private val app = DocVaultApplication.instance
    private val pinManager = app.pinManager
    private val autoLockManager = app.autoLockManager

    // ── What screen is currently showing ────────────────
    // "mutableStateOf" = whenever this value changes,
    // the screen automatically updates. Like magic.
    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Lock)
        private set  // only this ViewModel can change it

    // ── Error message for wrong PIN ─────────────────────
    var pinError by mutableStateOf<String?>(null)
        private set

    // ── Should we show the fingerprint button? ──────────
    var showBiometric by mutableStateOf(false)
        private set

    // ── This runs when the ViewModel is first created ───
    init {
        decideFirstScreen()
    }

    /**
     * Decide which screen to show when app opens.
     *
     * If user has never set a PIN → show PIN setup
     * If user has a PIN → show lock screen
     */
    private fun decideFirstScreen() {
        if (pinManager.isPinSet()) {
            // User has a PIN — show lock screen
            currentScreen = AppScreen.Lock

            // Check if fingerprint is available AND enabled
            showBiometric = pinManager.isBiometricEnabled() &&
                    app.biometricHelper.isBiometricAvailable()
        } else {
            // First time user — show PIN setup
            currentScreen = AppScreen.PinSetup
        }
    }

    // ═══════════════════════════════════════════════════
    // ACTIONS (called by the screens when user does something)
    // ═══════════════════════════════════════════════════

    /**
     * Called when user creates a PIN for the first time.
     * Saves the PIN and takes user to home screen.
     */
    fun onPinCreated(pin: String) {
        pinManager.setPin(pin)
        autoLockManager.onUnlocked()
        currentScreen = AppScreen.Home
    }

    /**
     * Called when user enters PIN on the lock screen.
     * Checks if it's correct.
     */
    fun onPinEntered(pin: String) {
        if (pinManager.verifyPin(pin)) {
            // Correct PIN!
            pinError = null
            autoLockManager.onUnlocked()
            currentScreen = AppScreen.Home
        } else {
            // Wrong PIN
            pinError = "Wrong PIN. Try again."
        }
    }

    /**
     * Called when fingerprint authentication succeeds.
     */
    fun onBiometricSuccess() {
        pinError = null
        autoLockManager.onUnlocked()
        currentScreen = AppScreen.Home
    }

    /**
     * Called when fingerprint authentication fails or is cancelled.
     */
    fun onBiometricError(error: String) {
        if (error == "USE_PIN") {
            // User tapped "Use PIN instead" — just hide fingerprint button
            // They can still use the number pad
            showBiometric = false
        }
        // For other errors, do nothing — user can still type PIN
    }

    /**
     * Called when user enables biometric during PIN setup.
     */
    fun onBiometricEnabled() {
        pinManager.setBiometricEnabled(true)
        showBiometric = true
    }

    // ═══════════════════════════════════════════════════
    // AUTO-LOCK (called by MainActivity lifecycle)
    // ═══════════════════════════════════════════════════

    /**
     * Called when user leaves the app (switches to another app).
     * Starts the lock timer.
     */
    fun onAppPaused() {
        if (currentScreen == AppScreen.Home) {
            autoLockManager.onAppBackgrounded()
        }
    }

    /**
     * Called when user comes back to the app.
     * If they were gone too long, lock the app.
     */
    fun onAppResumed() {
        if (currentScreen == AppScreen.Home && autoLockManager.shouldLock()) {
            // User was away too long — lock the app
            currentScreen = AppScreen.Lock
            showBiometric = pinManager.isBiometricEnabled() &&
                    app.biometricHelper.isBiometricAvailable()
        }
    }
}