package com.docvault.ai

import com.docvault.data.database.LearnedKeywordEntity
import com.docvault.data.models.DocumentCategory

/**
 * Super-intelligent classifier that combines OCR content analysis, filename intelligence,
 * and learned user patterns.
 */
class CategoryClassifier {

    private val exclusiveMarkers = mapOf(
        "passport" to DocumentCategory.ID_PERSONAL,
        "aadhaar" to DocumentCategory.ID_PERSONAL,
        "pan card" to DocumentCategory.ID_PERSONAL,
        "voter id" to DocumentCategory.ID_PERSONAL,
        "driving license" to DocumentCategory.ID_PERSONAL,
        "prescription" to DocumentCategory.MEDICAL,
        "lab report" to DocumentCategory.MEDICAL,
        "blood test" to DocumentCategory.MEDICAL,
        "x-ray" to DocumentCategory.MEDICAL,
        "vaccination" to DocumentCategory.MEDICAL,
        "invoice" to DocumentCategory.RECEIPTS,
        "receipt" to DocumentCategory.RECEIPTS,
        "salary slip" to DocumentCategory.FINANCIAL,
        "payslip" to DocumentCategory.FINANCIAL,
        "bank statement" to DocumentCategory.FINANCIAL,
        "marksheet" to DocumentCategory.EDUCATION,
        "transcript" to DocumentCategory.EDUCATION,
        "rent agreement" to DocumentCategory.PROPERTY,
        "lease" to DocumentCategory.PROPERTY,
        "rc book" to DocumentCategory.VEHICLE,
        "chassis" to DocumentCategory.VEHICLE
    )

    private val signalVocabulary = mapOf(
        DocumentCategory.ID_PERSONAL to mapOf(
            "identity" to 5, "government" to 3, "national" to 3, "personal" to 2, "card" to 2, "citizen" to 4, "address" to 2
        ),
        DocumentCategory.FINANCIAL to mapOf(
            "bank" to 5, "account" to 4, "tax" to 8, "income" to 5, "salary" to 6, "investment" to 5, "portfolio" to 5, "loan" to 5, "credit" to 4, "debit" to 4, "interest" to 4
        ),
        DocumentCategory.RECEIPTS to mapOf(
            "total" to 3, "subtotal" to 3, "amount paid" to 5, "bill to" to 4, "transaction" to 4, "order id" to 6, "payment" to 3, "checkout" to 4, "gst" to 5, "vat" to 5
        ),
        DocumentCategory.MEDICAL to mapOf(
            "hospital" to 6, "clinic" to 6, "doctor" to 5, "patient" to 5, "diagnosis" to 8, "medicine" to 6, "symptoms" to 5, "pharmacy" to 5, "surgery" to 7, "treatment" to 5
        ),
        DocumentCategory.EDUCATION to mapOf(
            "university" to 6, "college" to 6, "school" to 4, "degree" to 8, "marks" to 5, "grade" to 5, "semester" to 5, "diploma" to 8, "educational" to 4, "certificate" to 3
        ),
        DocumentCategory.VEHICLE to mapOf(
            "registration" to 6, "engine" to 5, "insurance" to 5, "puc" to 8, "service" to 4, "vehicle" to 5, "chassis" to 8, "odometer" to 6, "model" to 3
        ),
        DocumentCategory.PROPERTY to mapOf(
            "property" to 6, "apartment" to 5, "house" to 5, "mortgage" to 8, "deed" to 10, "utility" to 4, "maintenance" to 5, "electricity bill" to 7, "water bill" to 7
        )
    )

    /**
     * The master classification engine.
     * Combines filename, full text, and learned patterns.
     */
    fun classify(
        text: String, 
        filename: String, 
        learnedKeywords: List<LearnedKeywordEntity> = emptyList()
    ): DocumentCategory {
        val combinedInput = (text + " " + filename).lowercase()

        // Pass 0: Check learned patterns (User Feedback is King)
        learnedKeywords.forEach { learned ->
            if (combinedInput.contains(learned.keyword.lowercase())) {
                return DocumentCategory.fromString(learned.assignedCategory)
            }
        }

        // Pass 1: Check for high-confidence exclusive markers (FAST)
        for ((marker, category) in exclusiveMarkers) {
            if (combinedInput.contains(marker)) return category
        }

        // Pass 2: Weighted Intelligence Scan (DEEP)
        val scores = mutableMapOf<DocumentCategory, Int>()
        signalVocabulary.forEach { (category, signals) ->
            var categoryScore = 0
            signals.forEach { (keyword, weight) ->
                if (combinedInput.contains(keyword)) {
                    categoryScore += weight
                }
            }
            scores[category] = categoryScore
        }

        val winner = scores.maxByOrNull { it.value }
        
        return if (winner == null || winner.value < 5) {
            DocumentCategory.OTHER
        } else {
            winner.key
        }
    }
}
