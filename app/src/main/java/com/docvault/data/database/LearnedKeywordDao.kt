package com.docvault.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedKeywordDao {
    @Query("SELECT * FROM learned_keywords")
    fun getAllLearnedKeywords(): Flow<List<LearnedKeywordEntity>>

    @Query("SELECT * FROM learned_keywords")
    suspend fun getAllLearnedKeywordsOnce(): List<LearnedKeywordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearnedKeyword(keyword: LearnedKeywordEntity)

    @Query("UPDATE learned_keywords SET frequency = frequency + 1 WHERE keyword = :keyword")
    suspend fun incrementFrequency(keyword: String)

    @Delete
    suspend fun deleteLearnedKeyword(keyword: LearnedKeywordEntity)
}
