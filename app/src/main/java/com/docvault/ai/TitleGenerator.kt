package com.docvault.ai

import java.util.*

/**
 * Generates meaningful titles for documents based on extracted text and metadata.
 */
class TitleGenerator {

    /**
     * Generates a title.
     * @param ocrText The full text from the document.
     * @param metadata The extracted metadata (dates, amounts, etc.)
     * @param originalName The original filename as a fallback.
     */
    fun generateTitle(
        ocrText: String,
        metadata: MetadataExtractor.ExtractedMetadata,
        originalName: String
    ): String {
        val vendor = findVendor(ocrText)
        val date = metadata.dates.firstOrNull()?.replace("/", "-") ?: ""
        val type = inferType(ocrText)

        val parts = mutableListOf<String>()
        if (vendor.isNotEmpty()) parts.add(vendor)
        if (type.isNotEmpty()) parts.add(type)
        if (date.isNotEmpty()) parts.add(date)

        return if (parts.isEmpty()) {
            originalName.substringBeforeLast(".")
        } else {
            parts.joinToString("_")
                .replace(Regex("[^a-zA-Z0-9_-]"), "") // Clean for filename
        }
    }

    private fun findVendor(text: String): String {
        val lines = text.lines().take(10) // Vendors are usually at the top
        
        // Common vendors list (could be expanded)
        val commonVendors = listOf(
            "Amazon", "Walmart", "Apple", "Google", "Uber", "Netflix", 
            "Starbucks", "McDonald's", "Zomato", "Swiggy", "Airtel", "Jio"
        )

        for (line in lines) {
            for (vendor in commonVendors) {
                if (line.contains(vendor, ignoreCase = true)) return vendor
            }
        }
        
        // If no common vendor, take the first non-empty line as a guess (often company name)
        return lines.firstOrNull { it.trim().length > 3 }?.trim()?.split(" ")?.firstOrNull() ?: ""
    }

    private fun inferType(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("invoice") || lowerText.contains("bill") -> "Bill"
            lowerText.contains("receipt") -> "Receipt"
            lowerText.contains("statement") -> "Statement"
            lowerText.contains("passport") -> "Passport"
            lowerText.contains("license") -> "License"
            lowerText.contains("policy") -> "Policy"
            lowerText.contains("prescription") -> "Prescription"
            lowerText.contains("certificate") -> "Certificate"
            else -> ""
        }
    }
}
