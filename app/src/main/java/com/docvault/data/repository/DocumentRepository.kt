// Location: app/src/main/java/com/docvault/data/repository

package com.docvault.data.repository

import com.docvault.data.database.*
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for all document and category operations.
 */
class DocumentRepository(
    private val documentDao: DocumentDao,
    private val categoryDao: CategoryDao,
    private val learnedKeywordDao: LearnedKeywordDao
) {

    // ── DOCUMENTS: READ ─────────────────────────────────

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

    suspend fun getAllDocumentsOnce(): List<DocumentEntity> =
        documentDao.getAllDocumentsOnce()

    // ── DOCUMENTS: WRITE ────────────────────────────────

    suspend fun insertDocument(document: DocumentEntity) =
        documentDao.insertDocument(document)

    suspend fun insertDocuments(documents: List<DocumentEntity>) =
        documentDao.insertDocuments(documents)

    // ── DOCUMENTS: UPDATE ───────────────────────────────

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

    // ── DOCUMENTS: DELETE ───────────────────────────────

    suspend fun deleteDocument(document: DocumentEntity) =
        documentDao.deleteDocument(document)

    suspend fun deleteDocumentById(id: String) =
        documentDao.deleteDocumentById(id)

    suspend fun deleteDocumentsByIds(ids: List<String>) =
        documentDao.deleteDocumentsByIds(ids)

    // ── DUPLICATE CHECK ─────────────────────────────────

    suspend fun isDuplicate(hash: String): Boolean =
        documentDao.existsByHash(hash)

    // ── CUSTOM CATEGORIES ───────────────────────────────

    fun getCustomCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun addCategory(name: String, emoji: String = "📁") =
        categoryDao.insertCategory(CategoryEntity(name, emoji))

    suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    // ── LEARNED KEYWORDS ───────────────────────────────

    fun getAllLearnedKeywords(): Flow<List<LearnedKeywordEntity>> =
        learnedKeywordDao.getAllLearnedKeywords()

    suspend fun getAllLearnedKeywordsOnce(): List<LearnedKeywordEntity> =
        learnedKeywordDao.getAllLearnedKeywordsOnce()

    suspend fun insertLearnedKeyword(keyword: LearnedKeywordEntity) =
        learnedKeywordDao.insertLearnedKeyword(keyword)

    suspend fun incrementKeywordFrequency(keyword: String) =
        learnedKeywordDao.incrementFrequency(keyword)
}
