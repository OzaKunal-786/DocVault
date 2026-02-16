package com.docvault.ai

import com.docvault.data.models.DocumentCategory

/**
 * Classifies document text into a category using keyword scoring with priorities.
 */
class CategoryClassifier {

    private val categoryKeywords = mapOf(
        DocumentCategory.ID_PERSONAL to mapOf(
            "passport" to 10, "license" to 10, "aadhaar" to 10, "social security" to 10, 
            "birth certificate" to 10, "government" to 2, "identity" to 5, "voter id" to 10,
            "pan card" to 10, "driving license" to 10
        ),
        DocumentCategory.FINANCIAL to mapOf(
            "statement" to 5, "bank" to 3, "account" to 3, "tax" to 8, "income" to 5, 
            "investment" to 5, "loan" to 5, "credit card" to 4, "debit card" to 4, "financial" to 5
        ),
        DocumentCategory.RECEIPTS to mapOf(
            "receipt" to 8, "invoice" to 8, "bill" to 5, "total" to 2, "subtotal" to 2,
            "cash" to 2, "order summary" to 5, "payment" to 3, "transaction" to 3
        ),
        DocumentCategory.MEDICAL to mapOf(
            "medical" to 8, "hospital" to 8, "clinic" to 8, "prescription" to 10,
            "doctor" to 5, "patient" to 5, "diagnosis" to 8, "lab report" to 10,
            "pharmacy" to 8, "blood test" to 10, "vaccination" to 10, "health" to 5
        ),
        DocumentCategory.EDUCATION to mapOf(
            "university" to 8, "college" to 8, "school" to 5, "degree" to 10,
            "certificate" to 5, "transcript" to 10, "marksheet" to 10, "semester" to 5,
            "diploma" to 10, "educational" to 5
        ),
        DocumentCategory.VEHICLE to mapOf(
            "vehicle" to 8, "car" to 5, "motorcycle" to 5, "insurance" to 5, 
            "registration" to 10, "service" to 5, "vin" to 10, "engine" to 5,
            "chassis" to 10, "polution" to 10
        ),
        DocumentCategory.PROPERTY to mapOf(
            "property" to 8, "rent" to 8, "lease" to 8, "agreement" to 5, 
            "electricity" to 5, "utility" to 5, "mortgage" to 10, "deed" to 10,
            "tax receipt" to 8, "water bill" to 8
        )
    )

    /**
     * Analyzes the text and assigns the most likely category.
     * Higher weights for unique identifiers to prevent misclassification.
     */
    fun classify(text: String): DocumentCategory {
        val scores = mutableMapOf<DocumentCategory, Int>()
        val lowerText = text.lowercase()

        // Give extra weight to strong medical indicators if they appear with receipt terms
        val medicalWeightBoost = if (lowerText.contains("prescription") || lowerText.contains("lab report")) 10 else 0

        categoryKeywords.forEach { (category, keywords) ->
            var categoryScore = 0
            keywords.forEach { (keyword, weight) ->
                if (lowerText.contains(keyword)) {
                    categoryScore += weight
                }
            }
            
            if (category == DocumentCategory.MEDICAL) {
                categoryScore += medicalWeightBoost
            }
            
            scores[category] = categoryScore
        }

        val topCategory = scores.maxByOrNull { it.value }
        
        return if (topCategory == null || topCategory.value == 0) {
            DocumentCategory.OTHER
        } else {
            topCategory.key
        }
    }
}
