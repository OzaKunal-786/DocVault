// Location: app/src/main/java/com/docvault/data/database/DocumentEntity.kt

package com.docvault.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Original file info
    val originalPath: String,           // where the file was found
    val originalFileName: String,       // original filename
    val originalHash: String,           // MD5 hash for duplicate detection

    // Vault storage
    val vaultFileName: String,          // encrypted file name in vault
    val thumbnailFileName: String?,     // encrypted thumbnail name

    // AI-generated info
    val title: String,                  // AI-generated meaningful title
    val category: String,               // AI-assigned category
    val extractedText: String,          // full OCR text (for search)
    val metadata: String,               // JSON: date, amount, vendor, etc.
    val aiConfidence: Float,            // 0.0 to 1.0 — how sure AI was

    // User corrections
    val isUserCorrected: Boolean = false,
    val userCategory: String? = null,   // if user changed category
    val userTitle: String? = null,      // if user renamed

    // File info
    val fileSize: Long,                 // in bytes
    val mimeType: String,               // image/jpeg, application/pdf, etc.
    val sourceFolder: String,           // Downloads, WhatsApp, Camera, etc.
    val pageCount: Int = 1,             // number of pages in PDF

    // Timestamps
    val importDate: Long = System.currentTimeMillis(),
    val documentDate: Long? = null,     // date found ON the document
    val lastAccessedDate: Long? = null,

    // Status
    val isProcessed: Boolean = false,   // OCR + categorization complete?
    val isFavorite: Boolean = false
) {
    /**
     * Returns the effective category — user's correction takes priority
     */
    fun effectiveCategory(): String {
        return userCategory ?: category
    }

    /**
     * Returns the effective title — user's rename takes priority
     */
    fun effectiveTitle(): String {
        return userTitle ?: title
    }
}