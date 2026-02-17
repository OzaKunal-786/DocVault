package com.docvault.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learned_keywords")
data class LearnedKeywordEntity(
    @PrimaryKey
    val keyword: String,
    val assignedCategory: String,
    val frequency: Int = 1
)
