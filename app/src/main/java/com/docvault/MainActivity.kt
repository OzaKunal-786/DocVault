// File: app/src/main/java/com/docvault/MainActivity.kt

package com.docvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.docvault.scanner.FileScanner
import com.docvault.ui.navigation.AppNavGraph
import com.docvault.ui.navigation.Routes
import com.docvault.ui.screens.*
import com.docvault.ui.theme.DocVaultTheme
import com.docvault.ui.viewmodel.AppScreen
import com.docvault.ui.viewmodel.AppViewModel
import com.docvault.util.BackupManager
import com.docvault.util.CameraHelper
import com.docvault.util.FileUtil
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : FragmentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private lateinit var cameraHelper: CameraHelper
    private var tempCameraFile: File? = null

    // 1. Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        viewModel.onPermissionsResult(allGranted)
        if (allGranted) {
            Toast.makeText(this, "Permissions Ready.", Toast.LENGTH_SHORT).show()
            // FIXED: Initial scan no longer starts automatically.
        }
    }

    // 2. Camera Launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                importSingleFile(Uri.fromFile(file), file.name)
            }
        }
    }

    // 3. File Picker Launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                importSingleFile(uri, getFileName(uri))
            }
        }
    }

    // 4. Folder Picker Launcher
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // Save this folder to monitored list
            viewModel.addMonitoredFolder(it.toString())
            startFolderScan(it)
        }
    }

    // 5. Backup Launcher
    private val backupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val success = BackupManager(this@MainActivity).createBackup(it)
                Toast.makeText(this@MainActivity, if(success) "Backup saved!" else "Backup failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 6. Restore Launcher
    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val success = BackupManager(this@MainActivity).restoreBackup(it)
                Toast.makeText(this@MainActivity, if(success) "Restore successful! Restarting..." else "Restore failed", Toast.LENGTH_LONG).show()
                if (success) {
                    finish()
                    startActivity(Intent(this@MainActivity, MainActivity::class.java))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraHelper = CameraHelper(this)

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
                            OnboardingScreen(onFinished = { viewModel.onOnboardingFinished() })
                        }
                        is AppScreen.PinSetup -> {
                            PinSetupScreen(
                                onPinCreated = { viewModel.onPinCreated(it) },
                                onBiometricSetup = { viewModel.onBiometricEnabled() }
                            )
                        }
                        is AppScreen.Lock -> {
                            LockScreen(
                                onPinEntered = { viewModel.onPinEntered(it) },
                                onBiometricClick = { triggerBiometric() },
                                showBiometric = viewModel.showBiometric,
                                pinError = viewModel.pinError,
                                isLoading = false
                            )
                        }
                        is AppScreen.Permission -> {
                            PermissionScreen(onGrantClick = { launchPermissionRequest() })
                        }
                        is AppScreen.Home, is AppScreen.ChangePin -> {
                            MainAppContent(viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun triggerInitialScan() {
        lifecycleScope.launch {
            val scanner = FileScanner(this@MainActivity)
            val found = scanner.scanForDocuments()
            if (found.isNotEmpty()) {
                DocVaultApplication.instance.importManager.importFiles(found)
            } else {
                Toast.makeText(this@MainActivity, "No documents found in default folders.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importSingleFile(uri: Uri, name: String) {
        lifecycleScope.launch {
            val scannedFile = FileScanner.ScannedFile(
                uri = uri,
                name = name,
                path = uri.path ?: "",
                size = 0,
                mimeType = contentResolver.getType(uri) ?: "application/octet-stream",
                dateModified = System.currentTimeMillis(),
                hash = uri.toString() + System.currentTimeMillis()
            )
            DocVaultApplication.instance.importManager.importFiles(listOf(scannedFile))
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "unknown_file"
    }

    private fun startFolderScan(uri: Uri) {
        lifecycleScope.launch {
            val scanner = FileScanner(this@MainActivity)
            val found = scanner.scanFolderRecursively(uri)
            DocVaultApplication.instance.importManager.importFiles(found)
        }
    }

    fun launchCamera() {
        try {
            val file = cameraHelper.createTempImageFile()
            tempCameraFile = file
            val uri = cameraHelper.getUriForFile(file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchFilePicker() {
        filePickerLauncher.launch("*/*")
    }

    fun launchFolderPicker() {
        folderPickerLauncher.launch(null)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            uri?.let { importSingleFile(it, getFileName(it)) }
        }
    }

    @Composable
    fun MainAppContent(viewModel: AppViewModel) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        val items = listOf(
            BottomNavItem("Docs", Routes.HOME, Icons.Default.Description),
            BottomNavItem("Search", Routes.SEARCH, Icons.Default.Search),
            BottomNavItem("Settings", Routes.SETTINGS, Icons.Default.Settings)
        )

        Scaffold(
            bottomBar = {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.name) },
                            label = { Text(item.name) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                AppNavGraph(
                    navController = navController,
                    appViewModel = viewModel,
                    onAddCameraClick = { launchCamera() },
                    onAddFileClick = { launchFilePicker() },
                    onInitialImportClick = { triggerInitialScan() },
                    onDocumentClick = { docId -> openDocument(docId) },
                    onBackupClick = { backupLauncher.launch("DocVault_Backup.zip") },
                    onRestoreClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                    onAddMonitoredFolderClick = { launchFolderPicker() }
                )
            }
        }
    }

    private fun openDocument(docId: String) {
        lifecycleScope.launch {
            val file = DocVaultApplication.instance.encryptedFileManager.decryptToTemp(docId)
            if (file != null && file.exists()) {
                val uri = cameraHelper.getUriForFile(file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(Intent.createChooser(intent, "Open Document"))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "No PDF viewer found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Error opening document", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchPermissionRequest() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun triggerBiometric() {
        DocVaultApplication.instance.biometricHelper.authenticate(
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
