package com.docvault.data.database

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = DocumentEntity::class)
@Entity(tableName = "document_fts")
data class DocumentFtsEntity(
    val title: String,
    val extractedText: String,
    val category: String,
    val metadata: String
)
