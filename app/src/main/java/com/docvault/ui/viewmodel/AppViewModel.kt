package com.docvault.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvault.DocVaultApplication
import com.docvault.data.database.CategoryEntity
import com.docvault.data.database.DocumentEntity
import com.docvault.data.models.CategoryItem
import com.docvault.data.models.DocumentCategory
import com.docvault.data.models.DocumentItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AppScreen {
    object Onboarding : AppScreen()
    object PinSetup : AppScreen()
    object Lock : AppScreen()
    object Home : AppScreen()
    object Permission : AppScreen()
    object ChangePin : AppScreen()
    data class PdfViewer(val docId: String) : AppScreen()
    data class EditDocument(val uri: Uri) : AppScreen()
}

class AppViewModel : ViewModel() {

    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Onboarding)
        private set
    
    var pinError by mutableStateOf<String?>(null)
        private set
    
    var showBiometric by mutableStateOf(false)
        private set

    // --- Selection Mode ---
    val selectedDocumentIds = mutableStateListOf<String>()
    var isSelectMode by mutableStateOf(false)
        private set

    fun toggleSelection(docId: String) {
        if (selectedDocumentIds.contains(docId)) {
            selectedDocumentIds.remove(docId)
            if (selectedDocumentIds.isEmpty()) isSelectMode = false
        } else {
            selectedDocumentIds.add(docId)
            isSelectMode = true
        }
    }

    fun clearSelection() {
        selectedDocumentIds.clear()
        isSelectMode = false
    }

    fun deleteSelectedDocuments() {
        viewModelScope.launch {
            repository.deleteDocumentsByIds(selectedDocumentIds.toList())
            clearSelection()
        }
    }

    // --- Dashboard & Search ---
    private val repository = DocVaultApplication.instance.repository
    
    var searchQuery by mutableStateOf("")
        private set

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
    }

    val categories: StateFlow<List<CategoryItem>> = combine(
        repository.getCategoryCounts(),
        repository.getCustomCategories()
    ) { counts, custom ->
        val list = mutableListOf<CategoryItem>()
        // 1. Built-in
        DocumentCategory.entries.forEach { category ->
            val count = counts.find { it.effectiveCategory == category.displayName }?.count ?: 0
            list.add(CategoryItem(category.emoji, category.displayName, count))
        }
        // 2. Custom
        custom.forEach { category ->
            val count = counts.find { it.effectiveCategory == category.name }?.count ?: 0
            list.add(CategoryItem(category.emoji, category.name, count))
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered documents based on search query
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredDocuments = snapshotFlow { searchQuery }
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getRecentDocuments(20)
            } else {
                val sanitizedQuery = query.trim().split(" ").filter { it.isNotEmpty() }.joinToString(" ") { "$it*" }
                repository.searchDocuments(sanitizedQuery)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        decideFirstScreen()
    }
    
    private fun decideFirstScreen() {
        val app = DocVaultApplication.instance
        val pinManager = app.pinManager
        
        if (!pinManager.isOnboardingComplete()) {
            currentScreen = AppScreen.Onboarding
            return
        }

        if (!hasRequiredPermissions(app)) {
            currentScreen = AppScreen.Permission
            return
        }

        if (!pinManager.isPinSet()) {
            currentScreen = AppScreen.PinSetup
            return
        }

        currentScreen = AppScreen.Lock
        showBiometric = pinManager.isBiometricEnabled() && 
                app.biometricHelper.isBiometricAvailable()
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }
        return perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun onOnboardingFinished() {
        val app = DocVaultApplication.instance
        app.pinManager.setOnboardingComplete(true)
        if (!hasRequiredPermissions(app)) {
            currentScreen = AppScreen.Permission
        } else if (!app.pinManager.isPinSet()) {
            currentScreen = AppScreen.PinSetup
        } else {
            currentScreen = AppScreen.Lock
        }
    }
    
    fun checkPermissions(context: Context) {
        if (!hasRequiredPermissions(context) && currentScreen == AppScreen.Home) {
            currentScreen = AppScreen.Permission
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            val app = DocVaultApplication.instance
            if (!app.pinManager.isPinSet()) {
                currentScreen = AppScreen.PinSetup
            } else {
                currentScreen = AppScreen.Lock
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
        
        // Don't auto-lock if we are in onboarding or setup flows
        if (currentScreen == AppScreen.Onboarding || 
            currentScreen == AppScreen.Permission || 
            currentScreen == AppScreen.PinSetup) return

        if (app.autoLockManager.shouldLock()) {
            currentScreen = AppScreen.Lock
            pinError = null
            showBiometric = app.pinManager.isBiometricEnabled() && 
                    app.biometricHelper.isBiometricAvailable()
        }
    }

    fun openViewer(docId: String) {
        currentScreen = AppScreen.PdfViewer(docId)
    }

    fun closeViewer() {
        currentScreen = AppScreen.Home
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            repository.deleteDocumentById(docId)
            currentScreen = AppScreen.Home
        }
    }

    fun renameDocument(docId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(docId, newTitle)
        }
    }

    fun updateCategory(docId: String, newCategory: String) {
        viewModelScope.launch {
            repository.updateCategory(docId, newCategory)
        }
    }

    fun createCategory(name: String, emoji: String) {
        viewModelScope.launch {
            repository.addCategory(name, emoji)
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(CategoryEntity(name))
        }
    }

    fun openEditor(uri: Uri) {
        currentScreen = AppScreen.EditDocument(uri)
    }

    // --- Settings / Monitored Folders ---
    private val folderPrefs = DocVaultApplication.instance.getSharedPreferences("monitored_folders", Context.MODE_PRIVATE)
    private val _monitoredFolders = MutableStateFlow(folderPrefs.getStringSet("uris", emptySet()) ?: emptySet())
    val monitoredFolders = _monitoredFolders.asStateFlow()

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

    private val scanPrefs = DocVaultApplication.instance.getSharedPreferences("scan_settings", Context.MODE_PRIVATE)
    var scanIntervalHours by mutableStateOf(scanPrefs.getInt("scan_interval", 24))
        private set
    var minFileSizeKB by mutableStateOf(scanPrefs.getInt("min_file_size", 80))
        private set

    fun setScanInterval(hours: Int) {
        scanIntervalHours = hours
        scanPrefs.edit().putInt("scan_interval", hours).apply()
    }

    fun setMinFileSize(kb: Int) {
        minFileSizeKB = kb
        scanPrefs.edit().putInt("min_file_size", kb).apply()
    }
}
