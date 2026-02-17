package com.docvault.ai

import android.net.Uri
import java.util.*

/**
 * Generates smarter titles for documents based on extracted text and metadata.
 */
class TitleGenerator(private val ocrEngine: OcrEngine) {

    suspend fun generateTitle(
        imageUri: Uri,
        metadata: MetadataExtractor.ExtractedMetadata,
        originalName: String
    ): String {
        // AI Logic: First read the content to decide the name
        val ocrText = if (originalName.lowercase().endsWith(".pdf")) {
            ocrEngine.extractTextFromPdf(imageUri)
        } else {
            ocrEngine.extractText(imageUri)
        }

        val lowerText = ocrText.lowercase()
        val vendor = findVendor(ocrText)
        val date = metadata.dates.firstOrNull()?.replace("/", "-") ?: ""

        // Priority-based type inference
        val type = getDocumentType(lowerText)

        val parts = mutableListOf<String>()

        // 1. Add Vendor if found (Amazon, Starbucks, etc.)
        if (vendor.isNotEmpty() && !type.contains(vendor, ignoreCase = true)) {
            parts.add(vendor)
        }

        // 2. Add Type (Passport, Receipt, etc.)
        if (type.isNotEmpty()) {
            parts.add(type)
        } else {
            parts.add(originalName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_-]"), "_"))
        }

        // 3. Add Date if found
        if (date.isNotEmpty()) {
            parts.add(date)
        }

        return parts.joinToString("_")
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(50)
    }

    private fun findVendor(text: String): String {
        val lines = text.lines().take(15)
        val commonVendors = listOf(
            "Amazon", "Walmart", "Apple", "Google", "Uber", "Netflix",
            "Starbucks", "McDonald's", "Zomato", "Swiggy", "Airtel", "Jio",
            "HDFC", "ICICI", "SBI", "LIC", "Vodafone", "Zoom"
        )

        for (line in lines) {
            for (vendor in commonVendors) {
                if (line.contains(vendor, ignoreCase = true)) return vendor
            }
        }

        return lines.firstOrNull {
            val trimmed = it.trim()
            trimmed.length in 3..25 &&
            trimmed.all { char -> char.isUpperCase() || char.isWhitespace() }
        }?.trim()?.split(" ")?.firstOrNull() ?: ""
    }

    private fun getDocumentType(text: String): String {
        return when {
            text.contains("prescription") || text.contains("rx") -> "Prescription"
            text.contains("report") && (text.contains("blood") || text.contains("lab") || text.contains("clinic")) -> "Medical_Report"
            text.contains("passport") -> "Passport"
            text.contains("driving license") || text.contains("dl") -> "Driving_License"
            text.contains("aadhaar") || text.contains("unique identification") -> "Aadhaar"
            text.contains("pan card") || text.contains("income tax department") -> "PAN_Card"
            text.contains("voter id") || text.contains("election commission") -> "Voter_ID"
            text.contains("invoice") || text.contains("bill to") -> "Invoice"
            text.contains("receipt") || text.contains("transaction") -> "Receipt"
            text.contains("statement") && (text.contains("bank") || text.contains("account")) -> "Bank_Statement"
            text.contains("policy") && text.contains("insurance") -> "Insurance_Policy"
            text.contains("certificate") -> "Certificate"
            else -> ""
        }
    }
}
