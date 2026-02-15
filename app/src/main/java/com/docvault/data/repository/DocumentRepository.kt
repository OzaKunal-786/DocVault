// Location: app/src/main/java/com/docvault/data/repository/DocumentRepository.kt

package com.docvault.data.repository

import com.docvault.data.database.CategoryCount
import com.docvault.data.database.DocumentDao
import com.docvault.data.database.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all document operations.
 * All UI/ViewModel code should use this — never access DAO directly.
 */
class DocumentRepository(private val documentDao: DocumentDao) {

    // ── READ ────────────────────────────────────────────

    fun getAllDocuments(): Flow<List<DocumentEntity>> =
        documentDao.getAllDocuments()

    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByCategory(category)

    suspend fun getDocumentById(id: String): DocumentEntity? =
        documentDao.getDocumentById(id)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
        documentDao.searchDocuments(query)

    fun getCategoryCounts(): Flow<List<CategoryCount>> =
        documentDao.getCategoryCounts()

    fun getRecentDocuments(limit: Int = 10): Flow<List<DocumentEntity>> =
        documentDao.getRecentDocuments(limit)

    fun getTotalCount(): Flow<Int> =
        documentDao.getTotalCount()

    fun getTotalSize(): Flow<Long?> =
        documentDao.getTotalSize()

    // ── WRITE ───────────────────────────────────────────

    suspend fun insertDocument(document: DocumentEntity) =
        documentDao.insertDocument(document)

    suspend fun insertDocuments(documents: List<DocumentEntity>) =
        documentDao.insertDocuments(documents)

    // ── UPDATE ──────────────────────────────────────────

    suspend fun updateCategory(id: String, newCategory: String) =
        documentDao.updateCategory(id, newCategory)

    suspend fun updateTitle(id: String, newTitle: String) =
        documentDao.updateTitle(id, newTitle)

    suspend fun updateFavorite(id: String, isFavorite: Boolean) =
        documentDao.updateFavorite(id, isFavorite)

    suspend fun markAsAccessed(id: String) =
        documentDao.updateLastAccessed(id)

    suspend fun updateDocument(document: DocumentEntity) =
        documentDao.updateDocument(document)

    // ── DELETE ──────────────────────────────────────────

    suspend fun deleteDocument(document: DocumentEntity) =
        documentDao.deleteDocument(document)

    suspend fun deleteDocumentById(id: String) =
        documentDao.deleteDocumentById(id)

    // ── DUPLICATE CHECK ─────────────────────────────────

    suspend fun isDuplicate(hash: String): Boolean =
        documentDao.existsByHash(hash)
}