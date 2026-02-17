package com.docvault.data.repository

import android.content.Context
import android.net.Uri
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
 * Robust ImportManager designed for high volume imports.
 * Handles queuing, resource management, and detailed progress reporting.
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

    private val semaphore = Semaphore(2)

    suspend fun importFiles(files: List<FileScanner.ScannedFile>) = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext
        
        _importStatus.value = ImportStatus.Progress(0, files.size, "Analyzing storage...")
        
        val totalFiles = files.size
        val importedCount = AtomicInteger(0)
        val processedCount = AtomicInteger(0)

        files.chunked(50).forEach { chunk ->
            coroutineScope {
                chunk.map { scannedFile ->
                    async {
                        semaphore.withPermit {
                            try {
                                if (!repository.isDuplicate(scannedFile.hash)) {
                                    val documentId = UUID.randomUUID().toString()
                                    val pdfFile = preparePdf(scannedFile, documentId)

                                    if (pdfFile != null && pdfFile.exists()) {
                                        // 1. Multi-Format AI Analysis
                                        val ocrText = if (scannedFile.mimeType.startsWith("image")) {
                                            ocrEngine.extractText(scannedFile.uri)
                                        } else if (scannedFile.mimeType == "application/pdf") {
                                            ocrEngine.extractTextFromPdf(scannedFile.uri)
                                        } else {
                                            ""
                                        }
                                        
                                        // 2. Intelligent Categorization using content + filename
                                        val category = categoryClassifier.classify(ocrText, scannedFile.name)

                                        // 3. Encrypt and Store
                                        val vaultFile = encryptedFileManager.encryptAndStore(pdfFile, documentId)
                                        
                                        if (vaultFile != null) {
                                            val entity = DocumentEntity(
                                                id = documentId,
                                                originalPath = scannedFile.path,
                                                originalFileName = scannedFile.name,
                                                originalHash = scannedFile.hash,
                                                vaultFileName = vaultFile.name,
                                                thumbnailFileName = null,
                                                // FIXED: Keep original filenames as requested
                                                title = scannedFile.name.substringBeforeLast("."),
                                                category = category.displayName,
                                                extractedText = ocrText,
                                                metadata = "{}", 
                                                aiConfidence = 0.9f,
                                                fileSize = pdfFile.length(),
                                                mimeType = "application/pdf",
                                                sourceFolder = scannedFile.path.substringBeforeLast("/", "Unknown"),
                                                isProcessed = true
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
            yield()
        }
        
        _importStatus.value = ImportStatus.Success(importedCount.get())
        delay(3000)
        _importStatus.value = ImportStatus.Idle
    }

    private suspend fun preparePdf(scannedFile: FileScanner.ScannedFile, documentId: String): File? {
        return try {
            val temp = File(context.cacheDir, "imp_$documentId.pdf")
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
}
