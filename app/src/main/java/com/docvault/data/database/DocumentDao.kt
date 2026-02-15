// Location: app/src/main/java/com/docvault/data/database/DocumentDao.kt

package com.docvault.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // ── INSERT ──────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    // ── QUERY ALL ───────────────────────────────────────
    @Query("SELECT * FROM documents ORDER BY importDate DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY importDate DESC")
    suspend fun getAllDocumentsOnce(): List<DocumentEntity>

    // ── QUERY BY CATEGORY ───────────────────────────────
    @Query("""
        SELECT * FROM documents 
        WHERE category = :category 
        OR userCategory = :category 
        ORDER BY importDate DESC
    """)
    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>>

    // ── QUERY BY ID ─────────────────────────────────────
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?

    // ── SEARCH ──────────────────────────────────────────
    @Query("""
        SELECT * FROM documents 
        WHERE title LIKE '%' || :query || '%' 
        OR extractedText LIKE '%' || :query || '%'
        OR category LIKE '%' || :query || '%'
        OR userCategory LIKE '%' || :query || '%'
        OR userTitle LIKE '%' || :query || '%'
        OR metadata LIKE '%' || :query || '%'
        ORDER BY importDate DESC
    """)
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    // ── CATEGORY COUNTS ─────────────────────────────────
    @Query("""
        SELECT 
            CASE 
                WHEN userCategory IS NOT NULL THEN userCategory 
                ELSE category 
            END as effectiveCategory, 
            COUNT(*) as count 
        FROM documents 
        GROUP BY effectiveCategory
    """)
    fun getCategoryCounts(): Flow<List<CategoryCount>>

    // ── DUPLICATE CHECK ─────────────────────────────────
    @Query("SELECT EXISTS(SELECT 1 FROM documents WHERE originalHash = :hash)")
    suspend fun existsByHash(hash: String): Boolean

    // ── RECENT DOCUMENTS ────────────────────────────────
    @Query("SELECT * FROM documents ORDER BY importDate DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 10): Flow<List<DocumentEntity>>

    // ── UPDATE ──────────────────────────────────────────
    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("""
        UPDATE documents 
        SET userCategory = :newCategory, isUserCorrected = 1 
        WHERE id = :id
    """)
    suspend fun updateCategory(id: String, newCategory: String)

    @Query("""
        UPDATE documents 
        SET userTitle = :newTitle 
        WHERE id = :id
    """)
    suspend fun updateTitle(id: String, newTitle: String)

    @Query("""
        UPDATE documents 
        SET isFavorite = :isFavorite 
        WHERE id = :id
    """)
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("""
        UPDATE documents 
        SET lastAccessedDate = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateLastAccessed(id: String, timestamp: Long = System.currentTimeMillis())

    // ── DELETE ───────────────────────────────────────────
    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    // ── STATS ───────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM documents")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT SUM(fileSize) FROM documents")
    fun getTotalSize(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM documents WHERE isProcessed = 0")
    fun getUnprocessedCount(): Flow<Int>
}

/**
 * Helper data class for category count query results
 */
data class CategoryCount(
    val effectiveCategory: String,
    val count: Int
)