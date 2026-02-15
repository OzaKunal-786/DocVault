// Location: app/src/main/java/com/docvault/data/database/DocumentEntity.kt

package com.docvault.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["category"]),
        Index(value = ["importDate"]),
        Index(value = ["originalHash"], unique = true) // Fast duplicate detection
    ]
)
data class DocumentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Original file info
    val originalPath: String,
    val originalFileName: String,
    val originalHash: String,

    // Vault storage
    val vaultFileName: String,
    val thumbnailFileName: String?,

    // AI-generated info
    val title: String,
    val category: String,
    val extractedText: String,
    val metadata: String,
    val aiConfidence: Float,

    // User corrections
    val isUserCorrected: Boolean = false,
    val userCategory: String? = null,
    val userTitle: String? = null,

    // File info
    val fileSize: Long,
    val mimeType: String,
    val sourceFolder: String,
    val pageCount: Int = 1,

    // Timestamps
    val importDate: Long = System.currentTimeMillis(),
    val documentDate: Long? = null,
    val lastAccessedDate: Long? = null,

    // Status
    val isProcessed: Boolean = false,
    val isFavorite: Boolean = false
) {
    fun effectiveCategory(): String = userCategory ?: category
    fun effectiveTitle(): String = userTitle ?: title
}
