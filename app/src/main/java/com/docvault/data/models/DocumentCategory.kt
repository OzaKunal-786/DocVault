// Location: app/src/main/java/com/docvault/data/models/DocumentCategory.kt

package com.docvault.data.models

enum class DocumentCategory(
    val displayName: String,
    val emoji: String,
    val description: String
) {
    ID_PERSONAL(
        displayName = "ID & Personal",
        emoji = "📋",
        description = "Passport, License, Aadhaar, PAN, Voter ID"
    ),
    FINANCIAL(
        displayName = "Financial",
        emoji = "💰",
        description = "Bank statements, Tax docs, Investment records"
    ),
    RECEIPTS(
        displayName = "Receipts",
        emoji = "🧾",
        description = "Shopping, Restaurant, Online orders"
    ),
    MEDICAL(
        displayName = "Medical",
        emoji = "🏥",
        description = "Prescriptions, Lab reports, Medical bills"
    ),
    EDUCATION(
        displayName = "Education",
        emoji = "🎓",
        description = "Certificates, Marksheets, Transcripts"
    ),
    VEHICLE(
        displayName = "Vehicle",
        emoji = "🚗",
        description = "RC, Insurance, PUC, Service records"
    ),
    PROPERTY(
        displayName = "Property",
        emoji = "🏠",
        description = "Rent agreement, Electricity, Water bills"
    ),
    OTHER(
        displayName = "Other",
        emoji = "📄",
        description = "Uncategorized documents"
    );

    companion object {
        fun fromString(value: String): DocumentCategory {
            return entries.find {
                it.name.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}