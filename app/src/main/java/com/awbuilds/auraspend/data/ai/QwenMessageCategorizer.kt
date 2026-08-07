package com.awbuilds.auraspend.data.ai

import com.awbuilds.auraspend.domain.model.TransactionType
import java.io.File

/**
 * Result of an on-device LLM categorisation of a single message.
 *
 * The LLM handles the parts regex is bad at: subscription detection, category selection and
 * income-vs-expense. Amount/merchant/date stay with the existing parser (BankMessageParser).
 */
data class AiCategorisation(
    val isTransaction: Boolean,
    val type: TransactionType?,
    val categoryId: String?,
    val isSubscription: Boolean,
    val merchant: String?,
    val rawModelOutput: String
)

/**
 * Builds prompts for Qwen2.5-0.5B-Instruct and parses its strict JSON output into an
 * [AiCategorisation].
 *
 * Deliberately deterministic: the model is asked to return a single compact JSON object, which we
 * parse defensively so partial/verbose generations still yield a usable result. When nothing can
 * be parsed, the caller falls back to the existing heuristic classifier.
 */
class QwenMessageCategorizer(
    private val llm: LocalLlm?,
    private val modelFile: File
) {

    companion object {
        private const val CATEGORY_OTHER_ID = "cat_other"
        private const val CATEGORY_SUBSCRIPTION_ID = "cat_subscription"
    }

    /**
     * @param rawMessage the SMS / note text.
     * @param categoryIdByName the user's available categories keyed by id -> human name, used to
     *        constrain the model's output and to map the returned name back to an id.
     */
    fun categorise(
        rawMessage: String,
        categoryIdByName: Map<String, String>
    ): AiCategorisation? {
        val engine = llm ?: return null
        if (!engine.isModelAvailable(modelFile)) return null

        val output = try {
            engine.generate(buildPrompt(rawMessage, categoryIdByName))
        } catch (e: Exception) {
            return null
        } ?: return null

        return parse(output, categoryIdByName) ?: AiCategorisation(
            isTransaction = false,
            type = null,
            categoryId = null,
            isSubscription = false,
            merchant = null,
            rawModelOutput = output
        )
    }

    internal fun buildPrompt(
        rawMessage: String,
        categoryIdByName: Map<String, String>
    ): String {
        val categories = categoryIdByName.values
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

        val system = buildString {
            append("You are a personal finance assistant. Your only job is to read an SMS ")
            append("and return ONE compact JSON object with exactly these keys:\n")
            append("{\"is_transaction\":true|false,\"type\":\"income|expense|none\",\"category\":\"<categoryName>\",\"merchant\":\"<name or none>\",\"subscription\":true|false}\n")
            append("Rules:\n")
            append("- \"is_transaction\" is true if the message describes money spent (expense) or received (income). False for OTPs, balance alerts (without a transaction), data alerts, or personal chats.\n")
            append("- \"type\" is income if money is received, expense if spent, otherwise \"none\".\n")
            append("- \"category\" must be one of: $categories (use \"other\" when none fit).\n")
            append("- For recurring charges (Netflix, iCloud, etc.) use category \"subscription\" and subscription true.\n")
            append("- Output ONLY the JSON object. No prose.\n")
        }

        return "$system\n\nMessage: $rawMessage"
    }

    internal fun parse(
        output: String,
        categoryIdByName: Map<String, String>
    ): AiCategorisation? {
        val trimmed = output.trim()
        val closed = trimmed.substringAfter('{', "").substringBeforeLast('}', "")
        if (closed.isBlank()) return null

        val isTransaction = Regex("\"is_transaction\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
            .find(closed)?.groupValues?.get(1)?.equals("true", ignoreCase = true) == true

        val type = when (Regex("\"type\"\\s*:\\s*\"(income|expense)\"", RegexOption.IGNORE_CASE)
            .find(closed)?.groupValues?.get(1)?.lowercase()) {
            "income" -> TransactionType.INCOME
            "expense" -> TransactionType.EXPENSE
            else -> null
        }

        val categoryName = Regex("\"category\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            .find(closed)?.groupValues?.get(1)?.trim()

        val merchant = Regex("\"merchant\"\\s*:\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            .find(closed)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() && it != "none" }

        val isSubscription = categoryName.equals("subscription", ignoreCase = true) ||
            Regex("\"subscription\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
                .find(closed)?.groupValues?.get(1)?.equals("true", ignoreCase = true) == true

        val categoryId = when {
            isSubscription -> CATEGORY_SUBSCRIPTION_ID
            else -> lookupCategoryId(categoryName, categoryIdByName)
        }

        return AiCategorisation(
            isTransaction = isTransaction,
            type = type,
            categoryId = categoryId,
            isSubscription = isSubscription,
            merchant = merchant,
            rawModelOutput = trimmed
        )
    }

    private fun lookupCategoryId(
        categoryName: String?,
        categoryIdByName: Map<String, String>
    ): String? {
        if (categoryName.isNullOrBlank() || categoryName.equals("other", ignoreCase = true)) {
            return CATEGORY_OTHER_ID
        }
        return categoryIdByName.entries
            .firstOrNull { (_, name) -> name.equals(categoryName, ignoreCase = true) }
            ?.key
            ?: CATEGORY_OTHER_ID
    }
}
