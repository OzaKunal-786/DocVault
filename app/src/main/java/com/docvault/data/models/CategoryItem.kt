package com.docvault.data.models

data class CategoryItem(
    val emoji: String, 
    val name: String, 
    val count: Int
)

data class DocumentItem(
    val id: String,
    val title: String,
    val category: String,
    val date: String,
    val size: String
)
