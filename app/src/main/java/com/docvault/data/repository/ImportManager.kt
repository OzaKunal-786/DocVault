package com.docvault.data.repository

import android.content.Context
import com.docvault.ai.CategoryClassifier
import com.docvault.ai.MetadataExtractor
import com.docvault.ai.OcrEngine
import com.docvault.ai.TitleGenerator
import com.docvault.data.database.DocumentEntity
import com.docvault.pdf.PdfConverter
import com.docvault.scanner.FileScanner
import com.docvault.security.EncryptedFileManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

sealed class ImportStatus {
    object Idle : ImportStatus()
    data class Progress(val current: Int, val total: Int, val fileName: String) : ImportStatus()
    data class Success(val count: Int) : ImportStatus()
    data class Error(val message: String) : ImportStatus()
}

/**
 * Optimized ImportManager that processes multiple files in parallel
 * to improve import speed while managing system resources.
 */
class ImportManager(
    private val context: Context,
    private val repository: DocumentRepository,
    private val pdfConverter: PdfConverter,
    private val encryptedFileManager: EncryptedFileManager,
    private val ocrEngine: OcrEngine,
    private val metadataExtractor: MetadataExtractor,
    private val titleGenerator: TitleGenerator,
    private val categoryClassifier: CategoryClassifier
) {
    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus = _importStatus.asStateFlow()

    // Limit concurrency to avoid OOM or CPU starvation
    // 3 parallel tasks is a sweet spot for most mobile CPUs
    private val semaphore = Semaphore(3)

    suspend fun importFiles(files: List<FileScanner.ScannedFile>) = withContext(Dispatchers.IO) {
        _importStatus.value = ImportStatus.Progress(0, files.size, "Starting...")
        
        val totalFiles = files.size
        val importedCount = AtomicInteger(0)
        val processedCount = AtomicInteger(0)

        coroutineScope {
            files.map { scannedFile ->
                async {
                    semaphore.withPermit {
                        try {
                            if (!repository.isDuplicate(scannedFile.hash)) {
                                val documentId = UUID.randomUUID().toString()
                                
                                // 1. Prepare PDF
                                val pdfFile = preparePdf(scannedFile, documentId)

                                if (pdfFile != null && pdfFile.exists()) {
                                    // 2. AI Processing
                                    val ocrText = if (scannedFile.mimeType.startsWith("image")) {
                                        ocrEngine.extractText(scannedFile.uri)
                                    } else { "" }
                                    
                                    val metadata = metadataExtractor.extract(ocrText)
                                    val smartTitle = titleGenerator.generateTitle(ocrText, metadata, scannedFile.name)
                                    val category = categoryClassifier.classify(ocrText)

                                    // 3. Encrypt and store
                                    val vaultFile = encryptedFileManager.encryptAndStore(pdfFile, documentId)
                                    
                                    if (vaultFile != null) {
                                        // 4. Save to Database
                                        val entity = DocumentEntity(
                                            id = documentId,
                                            originalPath = scannedFile.path,
                                            originalFileName = scannedFile.name,
                                            originalHash = scannedFile.hash,
                                            vaultFileName = vaultFile.name,
                                            thumbnailFileName = null,
                                            title = smartTitle,
                                            category = category.displayName,
                                            extractedText = ocrText,
                                            metadata = "{}", // In a real app, use Gson/Moshi here
                                            aiConfidence = 0.8f,
                                            fileSize = scannedFile.size,
                                            mimeType = scannedFile.mimeType,
                                            sourceFolder = getFolderName(scannedFile.path),
                                            isProcessed = ocrText.isNotEmpty()
                                        )
                                        repository.insertDocument(entity)
                                        importedCount.incrementAndGet()
                                    }
                                    pdfFile.delete()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            val currentProcessed = processedCount.incrementAndGet()
                            _importStatus.value = ImportStatus.Progress(currentProcessed, totalFiles, scannedFile.name)
                        }
                    }
                }
            }.awaitAll()
        }
        
        _importStatus.value = ImportStatus.Success(importedCount.get())
    }

    private suspend fun preparePdf(scannedFile: FileScanner.ScannedFile, documentId: String): File? {
        return try {
            val temp = File(context.cacheDir, "temp_$documentId.pdf")
            if (scannedFile.mimeType == "application/pdf") {
                context.contentResolver.openInputStream(scannedFile.uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
                temp
            } else {
                if (pdfConverter.convertImageToPdf(scannedFile.uri, temp)) temp else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFolderName(path: String): String {
        return try {
            val file = File(path)
            file.parentFile?.name ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
