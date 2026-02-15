package com.docvault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docvault.scanner.FileScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanState(
    val isScanning: Boolean = false,
    val scannedFiles: List<FileScanner.ScannedFile> = emptyList()
)

class ScanViewModel(private val scanner: FileScanner) : ViewModel() {

    private val _scanState = MutableStateFlow(ScanState())
    val scanState = _scanState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _scanState.value = _scanState.value.copy(isScanning = true)
            val results = scanner.scanForDocuments()
            _scanState.value = _scanState.value.copy(
                isScanning = false,
                scannedFiles = results
            )
        }
    }
}
