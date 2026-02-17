package com.docvault.ai

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Extracts structured data (dates, amounts, etc.) from raw OCR text.
 */
class MetadataExtractor {

    data class ExtractedMetadata(
        val dates: List<String> = emptyList(),
        val amounts: List<String> = emptyList(),
        val documentNumbers: List<String> = emptyList()
    )

    /**
     * Scans text for patterns and returns structured metadata.
     */
    fun extract(text: String): ExtractedMetadata {
        return ExtractedMetadata(
            dates = findDates(text),
            amounts = findAmounts(text),
            documentNumbers = findDocumentNumbers(text)
        )
    }

    private fun findDates(text: String): List<String> {
        // Matches common formats: DD/MM/YYYY, YYYY-MM-DD, DD-MMM-YYYY
        val datePatterns = listOf(
            Pair("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b", "dd/MM/yyyy"),
            Pair("\\b(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b", "yyyy-MM-dd"),
            Pair("\\b(\\d{1,2}\\s(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s\\d{2,4})\\b", "dd MMM yyyy")
        )

        val results = mutableListOf<String>()
        for ((pattern, format) in datePatterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text)
            while (matcher.find()) {
                val dateString = matcher.group(1)
                try {
                    val date = SimpleDateFormat(format, Locale.getDefault()).parse(dateString)
                    results.add(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date))
                } catch (e: Exception) {
                    // Ignore invalid dates
                }
            }
        }
        return results.distinct()
    }

    private fun findAmounts(text: String): List<String> {
        // Matches $123.45, ₹1,234, € 50.00, etc.
        val amountPattern = Pattern.compile(
            "(?:[\\$€£₹]|RS\\.?)\\s?\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})\\b",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = amountPattern.matcher(text)
        val results = mutableListOf<String>()
        while (matcher.find()) {
            results.add(matcher.group())
        }
        return results.distinct()
    }

    private fun findDocumentNumbers(text: String): List<String> {
        // Matches common labels like Invoice #, Policy No, ID: ABC12345
        val patterns = listOf(
            Pair(Pattern.compile("(?:Invoice|Policy|ID|Ref|Receipt|Bill)\\s?[#: ]+([A-Z0-9-]{4,20})", Pattern.CASE_INSENSITIVE), 1),
            Pair(Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z]{1,2}\\d{4}\\b", Pattern.CASE_INSENSITIVE), 0) // Generic ID format like Passport/License
        )

        val results = mutableListOf<String>()
        for ((pattern, groupIndex) in patterns) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val match = if (groupIndex >= 0 && matcher.groupCount() >= groupIndex + 1) matcher.group(groupIndex + 1) else matcher.group()
                results.add(match.trim())
            }
        }
        return results.distinct()
    }
}