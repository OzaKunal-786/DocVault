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
import androidx.lifecycle.viewModelScope
import com.docvault.DocVaultApplication
import com.docvault.data.database.DocumentEntity
import com.docvault.data.models.CategoryItem
import com.docvault.data.models.DocumentCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AppScreen {
    object Onboarding : AppScreen()
    object PinSetup : AppScreen()
    object Lock : AppScreen()
    object Home : AppScreen()
    object Permission : AppScreen()
    object ChangePin : AppScreen()
}

class AppViewModel : ViewModel() {

    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Onboarding)
        private set
    
    var pinError by mutableStateOf<String?>(null)
        private set
    
    var showBiometric by mutableStateOf(false)
        private set

    // --- Dashboard Data ---
    private val repository = DocVaultApplication.instance.repository
    
    val categories = repository.getCategoryCounts().map { counts ->
        DocumentCategory.entries.map { category ->
            val count = counts.find { it.effectiveCategory == category.displayName }?.count ?: 0
            CategoryItem(category.emoji, category.displayName, count)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments = repository.getRecentDocuments(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
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
        
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            currentScreen = AppScreen.PinSetup
        } else {
            currentScreen = AppScreen.Permission
        }
    }
    
    fun checkPermissions(context: Context) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted && (currentScreen == AppScreen.Home)) {
            currentScreen = AppScreen.Permission
        } else if (allGranted && currentScreen == AppScreen.Permission) {
            if (!DocVaultApplication.instance.pinManager.isPinSet()) {
                currentScreen = AppScreen.PinSetup
            } else {
                currentScreen = AppScreen.Home
            }
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            if (!DocVaultApplication.instance.pinManager.isPinSet()) {
                currentScreen = AppScreen.PinSetup
            } else {
                currentScreen = AppScreen.Home
            }
        }
    }

    fun onPinCreated(pin: String) {
        DocVaultApplication.instance.pinManager.setPin(pin)
        DocVaultApplication.instance.autoLockManager.onUnlocked()
        currentScreen = AppScreen.Home
    }
    
    fun onPinEntered(pin: String) {
        if (DocVaultApplication.instance.pinManager.verifyPin(pin)) {
            pinError = null
            DocVaultApplication.instance.autoLockManager.onUnlocked()
            currentScreen = AppScreen.Home
        } else {
            pinError = "Wrong PIN. Try again."
        }
    }

    fun onChangePinRequested() {
        currentScreen = AppScreen.ChangePin
    }

    fun onPinChanged(newPin: String) {
        DocVaultApplication.instance.pinManager.setPin(newPin)
        currentScreen = AppScreen.Home
    }
    
    fun onBiometricSuccess() {
        pinError = null
        DocVaultApplication.instance.autoLockManager.onUnlocked()
        currentScreen = AppScreen.Home
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
        DocVaultApplication.instance.pinManager.setBiometricEnabled(true)
        showBiometric = DocVaultApplication.instance.biometricHelper.isBiometricAvailable()
    }
    
    fun onAppPaused() {
        if (currentScreen == AppScreen.Home || currentScreen == AppScreen.Permission) {
            DocVaultApplication.instance.autoLockManager.onAppBackgrounded()
        }
    }
    
    fun onAppResumed() {
        val app = DocVaultApplication.instance
        if ((currentScreen == AppScreen.Home || currentScreen == AppScreen.Permission) && app.autoLockManager.shouldLock()) {
            currentScreen = AppScreen.Lock
            pinError = null
            showBiometric = app.pinManager.isBiometricEnabled() && app.biometricHelper.isBiometricAvailable()
        }
    }

    // --- Settings / Monitored Folders ---
    private val scanPrefs = DocVaultApplication.instance.getSharedPreferences("scan_settings", Context.MODE_PRIVATE)
    
    var scanIntervalHours by mutableStateOf(scanPrefs.getInt("scan_interval", 24))
        private set

    private val folderPrefs = DocVaultApplication.instance.getSharedPreferences("monitored_folders", Context.MODE_PRIVATE)
    private val _monitoredFolders = MutableStateFlow(folderPrefs.getStringSet("uris", emptySet()) ?: emptySet())
    val monitoredFolders = _monitoredFolders.asStateFlow()

    fun setScanInterval(hours: Int) {
        scanIntervalHours = hours
        scanPrefs.edit().putInt("scan_interval", hours).apply()
    }

    fun addMonitoredFolder(uri: String) {
        val newSet = _monitoredFolders.value.toMutableSet()
        newSet.add(uri)
        _monitoredFolders.value = newSet
        folderPrefs.edit().putStringSet("uris", newSet).apply()
    }

    fun removeMonitoredFolder(uri: String) {
        val newSet = _monitoredFolders.value.toMutableSet()
        newSet.remove(uri)
        _monitoredFolders.value = newSet
        folderPrefs.edit().putStringSet("uris", newSet).apply()
    }
}