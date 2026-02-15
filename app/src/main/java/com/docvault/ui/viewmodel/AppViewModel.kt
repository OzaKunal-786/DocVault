package com.docvault.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.docvault.DocVaultApplication

/**
 * The "brain" of the app.
 */
sealed class AppScreen {
    object Onboarding : AppScreen()
    object PinSetup : AppScreen()
    object Lock : AppScreen()
    object Home : AppScreen()
    object Permission : AppScreen()
}

class AppViewModel : ViewModel() {

    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Onboarding)
        private set
    
    var pinError by mutableStateOf<String?>(null)
        private set
    
    var showBiometric by mutableStateOf(false)
        private set
    
    init {
        decideFirstScreen()
    }
    
    private fun decideFirstScreen() {
        val app = DocVaultApplication.instance
        val pinManager = app.pinManager
        
        currentScreen = when {
            !pinManager.isOnboardingComplete() -> AppScreen.Onboarding
            !pinManager.isPinSet() -> AppScreen.PinSetup
            else -> AppScreen.Lock
        }

        if (currentScreen == AppScreen.Lock) {
            showBiometric = pinManager.isBiometricEnabled() && 
                    app.biometricHelper.isBiometricAvailable()
        }
    }

    fun onOnboardingFinished() {
        val app = DocVaultApplication.instance
        app.pinManager.setOnboardingComplete(true)
        currentScreen = AppScreen.PinSetup
    }
    
    // ── Permissions ─────────────────────────────────────

    fun checkPermissions(context: Context) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted && currentScreen == AppScreen.Home) {
            currentScreen = AppScreen.Permission
        } else if (allGranted && currentScreen == AppScreen.Permission) {
            currentScreen = AppScreen.Home
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            currentScreen = AppScreen.Home
        }
    }

    // ── User Actions ────────────────────────────────────
    
    fun onPinCreated(pin: String) {
        val app = DocVaultApplication.instance
        app.pinManager.setPin(pin)
        app.autoLockManager.onUnlocked()
        
        checkPermissions(app)
        if (currentScreen != AppScreen.Permission) {
            currentScreen = AppScreen.Home
        }
    }
    
    fun onPinEntered(pin: String) {
        val app = DocVaultApplication.instance
        
        if (app.pinManager.verifyPin(pin)) {
            pinError = null
            app.autoLockManager.onUnlocked()
            checkPermissions(app)
            if (currentScreen != AppScreen.Permission) {
                currentScreen = AppScreen.Home
            }
        } else {
            pinError = "Wrong PIN. Try again."
        }
    }
    
    fun onBiometricSuccess() {
        val app = DocVaultApplication.instance
        pinError = null
        app.autoLockManager.onUnlocked()
        checkPermissions(app)
        if (currentScreen != AppScreen.Permission) {
            currentScreen = AppScreen.Home
        }
    }
    
    fun onBiometricError(error: String) {
        if (error == "USE_PIN") {
            showBiometric = false
            pinError = null
        } else {
            pinError = error
        }
    }
    
    fun onBiometricEnabled() {
        val app = DocVaultApplication.instance
        app.pinManager.setBiometricEnabled(true)
        showBiometric = app.biometricHelper.isBiometricAvailable()
    }
    
    // ── Auto-Lock ───────────────────────────────────────
    
    fun onAppPaused() {
        if (currentScreen == AppScreen.Home || currentScreen == AppScreen.Permission) {
            val app = DocVaultApplication.instance
            app.autoLockManager.onAppBackgrounded()
        }
    }
    
    fun onAppResumed() {
        val app = DocVaultApplication.instance
        
        if ((currentScreen == AppScreen.Home || currentScreen == AppScreen.Permission) 
            && app.autoLockManager.shouldLock()) {
            currentScreen = AppScreen.Lock
            pinError = null
            showBiometric = app.pinManager.isBiometricEnabled() && 
                    app.biometricHelper.isBiometricAvailable()
        }
    }
}