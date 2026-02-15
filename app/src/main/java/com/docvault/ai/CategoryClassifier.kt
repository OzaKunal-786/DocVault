package com.docvault.ai

import com.docvault.data.models.DocumentCategory

/**
 * Classifies document text into a category using keyword scoring.
 */
class CategoryClassifier {

    // Weighted keywords for each category. Higher weight = stronger signal.
    private val categoryKeywords = mapOf(
        DocumentCategory.ID_PERSONAL to mapOf(
            "passport" to 5, "license" to 5, "aadhaar" to 5, "social security" to 5, 
            "birth certificate" to 5, "government" to 2, "identity" to 3
        ),
        DocumentCategory.FINANCIAL to mapOf(
            "statement" to 3, "bank" to 2, "account" to 2, "tax" to 4, "income" to 3, 
            "investment" to 3, "loan" to 3
        ),
        DocumentCategory.RECEIPTS to mapOf(
            "receipt" to 4, "invoice" to 4, "bill" to 3, "total" to 1, "subtotal" to 1,
            "cash" to 1, "credit card" to 1, "order summary" to 3
        ),
        DocumentCategory.MEDICAL to mapOf(
            "medical" to 4, "hospital" to 3, "clinic" to 3, "prescription" to 5,
            "doctor" to 2, "patient" to 2, "diagnosis" to 4, "lab report" to 5
        ),
        DocumentCategory.EDUCATION to mapOf(
            "university" to 4, "college" to 3, "school" to 2, "degree" to 5,
            "certificate" to 3, "transcript" to 5, "marksheet" to 5, "semester" to 2
        ),
        DocumentCategory.VEHICLE to mapOf(
            "vehicle" to 3, "car" to 2, "motorcycle" to 2, "insurance" to 4, 
            "registration" to 5, "service" to 2, "vin" to 4
        ),
        DocumentCategory.PROPERTY to mapOf(
            "property" to 3, "rent" to 4, "lease" to 4, "agreement" to 2, 
            "electricity" to 2, "utility" to 2, "mortgage" to 5
        )
    )

    /**
     * Analyzes the text and assigns the most likely category.
     * @param text The full OCR text of the document.
     * @return The determined DocumentCategory.
     */
    fun classify(text: String): DocumentCategory {
        val scores = mutableMapOf<DocumentCategory, Int>()
        val lowerText = text.lowercase()

        categoryKeywords.forEach { (category, keywords) ->
            var categoryScore = 0
            keywords.forEach { (keyword, weight) ->
                if (lowerText.contains(keyword)) {
                    categoryScore += weight
                }
            }
            scores[category] = categoryScore
        }

        // Find the category with the highest score
        val topCategory = scores.maxByOrNull { it.value }?.key

        // If no keywords matched, or scores are all zero, return Other
        return if (scores.values.sum() == 0 || topCategory == null) {
            DocumentCategory.OTHER
        } else {
            topCategory
        }
    }
}
