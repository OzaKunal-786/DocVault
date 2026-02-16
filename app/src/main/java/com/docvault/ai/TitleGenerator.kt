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
        val lowerText = ocrText.lowercase()
        val vendor = findVendor(ocrText)
        val date = metadata.dates.firstOrNull()?.replace("/", "-") ?: ""
        
        // Priority based type inference
        val type = when {
            lowerText.contains("prescription") -> "Prescription"
            lowerText.contains("lab report") || lowerText.contains("blood test") -> "Medical_Report"
            lowerText.contains("passport") -> "Passport"
            lowerText.contains("license") -> "License"
            lowerText.contains("aadhaar") -> "Aadhaar"
            lowerText.contains("invoice") || lowerText.contains("bill") -> "Invoice"
            lowerText.contains("receipt") -> "Receipt"
            lowerText.contains("statement") -> "Statement"
            lowerText.contains("policy") -> "Policy"
            lowerText.contains("certificate") -> "Certificate"
            else -> ""
        }

        val parts = mutableListOf<String>()
        if (vendor.isNotEmpty() && !type.contains(vendor, ignoreCase = true)) parts.add(vendor)
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
        val lines = text.lines().take(10)
        val commonVendors = listOf(
            "Amazon", "Walmart", "Apple", "Google", "Uber", "Netflix", 
            "Starbucks", "McDonald's", "Zomato", "Swiggy", "Airtel", "Jio", "HDFC", "ICICI"
        )

        for (line in lines) {
            for (vendor in commonVendors) {
                if (line.contains(vendor, ignoreCase = true)) return vendor
            }
        }
        
        // If no common vendor, take the first line that looks like a name (uppercase, few words)
        return lines.firstOrNull { it.trim().length in 3..20 && it == it.uppercase() }?.trim()?.split(" ")?.firstOrNull() ?: ""
    }
}
