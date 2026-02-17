// File: app/src/main/java/com/docvault/DocVaultApplication.kt

package com.docvault

import android.app.Application
import com.docvault.ai.CategoryClassifier
import com.docvault.ai.MetadataExtractor
import com.docvault.ai.OcrEngine
import com.docvault.ai.TitleGenerator
import com.docvault.data.database.AppDatabase
import com.docvault.data.repository.DocumentRepository
import com.docvault.data.repository.ImportManager
import com.docvault.pdf.PdfConverter
import com.docvault.security.AutoLockManager
import com.docvault.security.BiometricHelper
import com.docvault.security.EncryptedFileManager
import com.docvault.security.EncryptionManager
import com.docvault.security.PinManager

/**
 * Optimized Application class using lazy initialization for all heavy components.
 */
class DocVaultApplication : Application() {

    // ── Security Tools (Lazy) ──
    val encryptionManager by lazy { EncryptionManager(this) }
    val pinManager by lazy { PinManager(this) }
    val biometricHelper by lazy { BiometricHelper(this) }
    val autoLockManager by lazy { AutoLockManager(pinManager) }
    val encryptedFileManager by lazy { EncryptedFileManager(this) }

    // ── AI & Processing Engines (Lazy) ──
    val ocrEngine by lazy { OcrEngine(this) }
    val metadataExtractor by lazy { MetadataExtractor() }
    val titleGenerator by lazy { TitleGenerator(ocrEngine) }
    val categoryClassifier by lazy { CategoryClassifier() }
    val pdfConverter by lazy { PdfConverter(this) }

    // ── Database & Repository (Lazy) ──
    val database by lazy {
        val passphrase = encryptionManager.getDatabasePassphrase()
        AppDatabase.getInstance(this, passphrase)
    }

    val repository by lazy {
        DocumentRepository(
            database.documentDao(), 
            database.categoryDao(),
            database.learnedKeywordDao()
        )
    }

    // ── Coordinator (Lazy) ──
    val importManager by lazy {
        ImportManager(
            context = this,
            repository = repository,
            pdfConverter = pdfConverter,
            encryptedFileManager = encryptedFileManager,
            ocrEngine = ocrEngine,
            metadataExtractor = metadataExtractor,
            titleGenerator = titleGenerator,
            categoryClassifier = categoryClassifier
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DocVaultApplication
            private set
    }
}
