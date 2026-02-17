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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    // Launchers
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        viewModel.onPermissionsResult(permissions.values.all { it })
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempCameraFile?.let { viewModel.openEditor(Uri.fromFile(it)) }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { importSingleFile(it, getFileName(it)) }
    }

    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.addMonitoredFolder(it.toString())
            startFolderScan(it)
        }
    }

    private val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { lifecycleScope.launch { BackupManager(this@MainActivity).createBackup(it) } }
    }

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { lifecycleScope.launch { if (BackupManager(this@MainActivity).restoreBackup(it)) { finish(); startActivity(intent) } } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraHelper = CameraHelper(this)
        handleIntent(intent)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { viewModel.onAppPaused(); FileUtil.clearTempFiles(this) }
            if (event == Lifecycle.Event.ON_START) { viewModel.onAppResumed(); viewModel.checkPermissions(this) }
        })

        setContent {
            DocVaultTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val screen = viewModel.currentScreen
                    when (screen) {
                        is AppScreen.Onboarding -> OnboardingScreen { viewModel.onOnboardingFinished() }
                        is AppScreen.PinSetup -> PinSetupScreen(onPinCreated = { viewModel.onPinCreated(it) }, onBiometricSetup = { viewModel.onBiometricEnabled() } )
                        is AppScreen.Lock -> LockScreen(onPinEntered = { viewModel.onPinEntered(it) }, onBiometricClick = { triggerBiometric() }, showBiometric = viewModel.showBiometric, pinError = viewModel.pinError, isLoading = false)
                        is AppScreen.Permission -> PermissionScreen { launchPermissionRequest() }
                        is AppScreen.PdfViewer -> InternalPdfViewer(screen.docId)
                        is AppScreen.EditDocument -> EditDocumentScreen(imageUri = screen.uri, onSave = { importSingleFile(it, getFileName(it)); viewModel.closeViewer() }, onBack = { viewModel.closeViewer() })
                        is AppScreen.Home, is AppScreen.ChangePin -> MainAppContent(viewModel)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainAppContent(viewModel: AppViewModel) {
        val navController = rememberNavController()
        var showSettings by remember { mutableStateOf(false) }
        var docToManage by remember { mutableStateOf<com.docvault.data.models.DocumentItem?>(null) }
        var catToDelete by remember { mutableStateOf<String?>(null) }

        if (showSettings) {
            SettingsScreen(
                viewModel = viewModel,
                onBackupClick = { backupLauncher.launch("DocVault_Backup.zip") },
                onRestoreClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                onBackClick = { showSettings = false },
                onChangePinClick = { viewModel.onChangePinRequested() },
                onAddMonitoredFolderClick = { launchFolderPicker() }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("DocVault", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                        actions = {
                            IconButton(onClick = { triggerInitialScan() }) { Icon(Icons.Default.Refresh, "Refresh") }
                            IconButton(onClick = { /* TODO */ }) { Icon(Icons.Default.Notifications, "Notifications") }
                            IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Settings") }
                        }
                    )
                },
                bottomBar = {
                    val items = listOf(
                        BottomNavItem("Docs", Routes.HOME, Icons.Default.Description),
                        BottomNavItem("All Files", Routes.ALL_FILES, Icons.Default.FolderCopy)
                    )
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
                        onDocumentClick = { viewModel.openViewer(it) },
                        onBackupClick = { backupLauncher.launch("DocVault_Backup.zip") },
                        onRestoreClick = { restoreLauncher.launch(arrayOf("application/zip")) },
                        onAddMonitoredFolderClick = { launchFolderPicker() },
                        onSettingsClick = { showSettings = true },
                        onDocumentLongClick = { docToManage = it },
                        onCategoryLongClick = { catToDelete = it }
                    )
                }
            }
        }

        // Dialogs
        docToManage?.let { doc ->
            var newName by remember { mutableStateOf(doc.title) }
            AlertDialog(
                onDismissRequest = { docToManage = null },
                title = { Text("Manage Document") },
                text = {
                    Column {
                        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Rename") }, singleLine = true)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                IconButton(onClick = { 
                                    docToManage = null
                                    lifecycleScope.launch {
                                        val file = DocVaultApplication.instance.encryptedFileManager.decryptToTemp(doc.id)
                                        file?.let { viewModel.openEditor(Uri.fromFile(it)) }
                                    }
                                }) { Icon(Icons.Outlined.Edit, "Edit") }
                                Text("Edit", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                IconButton(onClick = { shareDocument(doc.id) }) { Icon(Icons.Outlined.Share, "Share") }
                                Text("Share", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                IconButton(onClick = { viewModel.deleteDocument(doc.id); docToManage = null }) { 
                                    Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                                Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.renameDocument(doc.id, newName); docToManage = null }) { Text("Save Name") }
                },
                dismissButton = {
                    TextButton(onClick = { docToManage = null }) { Text("Cancel") }
                }
            )
        }

        catToDelete?.let { cat ->
            AlertDialog(
                onDismissRequest = { catToDelete = null },
                title = { Text("Delete Category?") },
                text = { Text("Are you sure you want to delete '$cat'?") },
                confirmButton = {
                    Button(onClick = { viewModel.deleteCategory(cat); catToDelete = null }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { catToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }

    @Composable
    private fun InternalPdfViewer(docId: String) {
        var file by remember { mutableStateOf<File?>(null) }
        var title by remember { mutableStateOf("") }
        LaunchedEffect(docId) {
            val entity = DocVaultApplication.instance.repository.getDocumentById(docId)
            title = entity?.effectiveTitle() ?: "Document"
            file = DocVaultApplication.instance.encryptedFileManager.decryptToTemp(docId)
        }
        if (file != null) {
            PdfViewerScreen(file = file!!, title = title, onBack = { viewModel.closeViewer() }, onDelete = { viewModel.deleteDocument(docId) }, onShare = { shareDocument(docId) }, onUpdateCategory = { viewModel.updateCategory(docId, it) })
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        }
    }

    private fun shareDocument(docId: String) {
        lifecycleScope.launch {
            val file = DocVaultApplication.instance.encryptedFileManager.decryptToTemp(docId)
            if (file != null) {
                val uri = cameraHelper.getUriForFile(file)
                val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                startActivity(Intent.createChooser(intent, "Share Document"))
            }
        }
    }

    private fun triggerInitialScan() {
        lifecycleScope.launch {
            val scanner = FileScanner(this@MainActivity)
            val found = scanner.scanForDocuments()
            if (found.isNotEmpty()) DocVaultApplication.instance.importManager.importFiles(found)
        }
    }

    private fun importSingleFile(uri: Uri, name: String) {
        lifecycleScope.launch {
            val scannedFile = FileScanner.ScannedFile(uri = uri, name = name, path = uri.path ?: "", size = 0, mimeType = contentResolver.getType(uri) ?: "application/octet-stream", dateModified = System.currentTimeMillis(), hash = uri.toString() + System.currentTimeMillis())
            DocVaultApplication.instance.importManager.importFiles(listOf(scannedFile))
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: "unknown"
    }

    private fun startFolderScan(uri: Uri) {
        lifecycleScope.launch {
            val scanner = FileScanner(this@MainActivity)
            val found = scanner.scanFolderRecursively(uri)
            DocVaultApplication.instance.importManager.importFiles(found)
        }
    }

    fun launchCamera() {
        val file = cameraHelper.createTempImageFile()
        tempCameraFile = file
        cameraLauncher.launch(cameraHelper.getUriForFile(file))
    }

    fun launchFilePicker() = filePickerLauncher.launch("*/*")
    fun launchFolderPicker() = folderPickerLauncher.launch(null)

    override fun onNewIntent(intent: Intent?) { super.onNewIntent(intent); intent?.let { handleIntent(it) } }
    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            uri?.let { importSingleFile(it, getFileName(it)) }
        }
    }

    private fun launchPermissionRequest() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.READ_MEDIA_IMAGES) else perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun triggerBiometric() = DocVaultApplication.instance.biometricHelper.authenticate(activity = this, onSuccess = { viewModel.onBiometricSuccess() }, onError = { viewModel.onBiometricError(it) })
}

data class BottomNavItem(val name: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
