package com.docvault.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.docvault.DocVaultApplication

/**
 * The "brain" of the app.
 * Keeps track of what screen should be showing.
 */
sealed class AppScreen {
    /** First-time user: needs to create a PIN */
    object PinSetup : AppScreen()
    
    /** Returning user: needs to unlock */
    object Lock : AppScreen()
    
    /** User is unlocked */
    object Home : AppScreen()
}

class AppViewModel : ViewModel() {

    // What screen is showing
    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Lock)
        private set
    
    // Error message for wrong PIN or biometric issues
    var pinError by mutableStateOf<String?>(null)
        private set
    
    // Show fingerprint button?
    var showBiometric by mutableStateOf(false)
        private set
    
    // This runs when ViewModel is created
    init {
        decideFirstScreen()
    }
    
    /**
     * Decide which screen to show when app opens.
     */
    private fun decideFirstScreen() {
        val app = DocVaultApplication.instance
        val pinManager = app.pinManager
        
        if (pinManager.isPinSet()) {
            // User has a PIN – show lock screen
            currentScreen = AppScreen.Lock
            
            // Check if fingerprint available and enabled by user
            showBiometric = pinManager.isBiometricEnabled() && 
                    app.biometricHelper.isBiometricAvailable()
        } else {
            // First time – show PIN setup
            currentScreen = AppScreen.PinSetup
        }
    }
    
    // ── User Actions ────────────────────────────────────
    
    /**
     * User creates PIN for first time
     */
    fun onPinCreated(pin: String) {
        val app = DocVaultApplication.instance
        app.pinManager.setPin(pin)
        app.autoLockManager.onUnlocked()
        currentScreen = AppScreen.Home
    }
    
    /**
     * User enters PIN on lock screen
     */
    fun onPinEntered(pin: String) {
        val app = DocVaultApplication.instance
        
        if (app.pinManager.verifyPin(pin)) {
            // Correct!
            pinError = null
            app.autoLockManager.onUnlocked()
            currentScreen = AppScreen.Home
        } else {
            // Wrong PIN
            pinError = "Wrong PIN. Try again."
        }
    }
    
    /**
     * Fingerprint succeeded
     */
    fun onBiometricSuccess() {
        val app = DocVaultApplication.instance
        pinError = null
        app.autoLockManager.onUnlocked()
        currentScreen = AppScreen.Home
    }
    
    /**
     * Fingerprint failed or cancelled
     */
    fun onBiometricError(error: String) {
        if (error == "USE_PIN") {
            // User tapped "Use PIN instead" - just hide biometric button for this session
            showBiometric = false
            pinError = null
        } else {
            // Show the actual error (e.g. "Too many attempts")
            pinError = error
        }
    }
    
    /**
     * User enables biometric during setup
     */
    fun onBiometricEnabled() {
        val app = DocVaultApplication.instance
        app.pinManager.setBiometricEnabled(true)
        showBiometric = app.biometricHelper.isBiometricAvailable()
    }
    
    // ── Auto-Lock ───────────────────────────────────────
    
    /**
     * User leaves the app
     */
    fun onAppPaused() {
        if (currentScreen == AppScreen.Home) {
            val app = DocVaultApplication.instance
            app.autoLockManager.onAppBackgrounded()
        }
    }
    
    /**
     * User returns to the app
     */
    fun onAppResumed() {
        val app = DocVaultApplication.instance
        
        if (currentScreen == AppScreen.Home && app.autoLockManager.shouldLock()) {
            // User was away too long – lock it
            currentScreen = AppScreen.Lock
            pinError = null
            showBiometric = app.pinManager.isBiometricEnabled() && 
                    app.biometricHelper.isBiometricAvailable()
        }
    }
}