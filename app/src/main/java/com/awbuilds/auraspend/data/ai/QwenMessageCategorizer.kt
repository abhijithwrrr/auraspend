package com.awbuilds.auraspend.data.ai

import android.util.Log
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
 * be parsed after one retry, the caller falls back to the existing heuristic classifier.
 *
 * Robustness measures:
 *  - few-shot examples in the prompt (dramatically improves a 0.5B model's format compliance);
 *  - balanced-brace JSON extraction that survives markdown fences and surrounding prose;
 *  - fuzzy category matching so "Food", "food & dining" or "FOOD & DINING" all resolve;
 *  - a single retry when the first generation contains no JSON object at all.
 */
class QwenMessageCategorizer(
    private val llm: LocalLlm?,
    private val modelFile: File
) {

    companion object {
        private const val TAG = "QwenCategorizer"
        private const val CATEGORY_OTHER_ID = "cat_other"
        private const val CATEGORY_SUBSCRIPTION_ID = "cat_subscription"
        private const val GENERATION_ATTEMPTS = 2
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

        val prompt = buildPrompt(rawMessage, categoryIdByName)
        var sawAnyOutput = false

        repeat(GENERATION_ATTEMPTS) {
            val output = try {
                engine.generate(prompt)
            } catch (e: Exception) {
                Log.w(TAG, "generate() threw; treating as unavailable", e)
                null
            }
            // A null generation means the engine is unavailable - retrying will not help.
            if (output == null) return null
            if (output.isNotBlank()) sawAnyOutput = true
            parse(output, categoryIdByName)?.let { return it }
        }

        // The model produced no usable JSON. Returning null (not a "not a transaction"
        // verdict!) lets the caller keep the regex result instead of wrongly vetoing it.
        if (sawAnyOutput) {
            Log.w(TAG, "Model output contained no parsable JSON after $GENERATION_ATTEMPTS attempts")
        }
        return null
    }

    internal fun buildPrompt(
        rawMessage: String,
        categoryIdByName: Map<String, String>
    ): String {
        val categories = categoryIdByName.values
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

        return buildString {
            appendLine("Extract bank-transaction data from ONE SMS. Reply with ONLY this JSON, on one line:")
            appendLine("""{"is_transaction":true|false,"type":"income|expense|none","category":"...","merchant":"...","subscription":true|false}""")
            appendLine()
            appendLine("Rules:")
            appendLine("1. is_transaction=true ONLY when money actually moved: paid/debited/spent/sent/withdrawn OR credited/received/refunded/cashback.")
            appendLine("2. is_transaction=false for OTP/PIN codes, balance-only alerts, offers/promos, cashback-earned announcements without a credit amount, KYC/block/token notices, failed or declined payments, card added/updated alerts.")
            appendLine("3. type: money received => \"income\"; money spent or sent => \"expense\"; not a transaction => \"none\".")
            appendLine("4. category must be EXACTLY one of: $categories. If nothing fits use \"Other\".")
            appendLine("5. Money sent to a person or UPI handle, or between own accounts => category \"Transfer\".")
            appendLine("6. Refund/cashback/reversal of a purchase => is_transaction=true, type=\"income\", category matching the original purchase when obvious.")
            appendLine("7. subscription=true ONLY for recurring charges: auto-pay/NACH/e-mandate/auto-debit, renewals, or services like Netflix/Spotify/iCloud/Prime/gym/insurance premiums.")
            appendLine("8. merchant: the store/service/person involved, properly capitalised (\"Swiggy\"); \"none\" if unclear. NEVER copy account numbers, card numbers, reference IDs, balances, or bank names as merchant.")
            appendLine("9. Output the JSON only. No explanations, no markdown fences, no extra fields.")
            appendLine()
            appendLine("Examples:")
            appendLine("SMS: INR 250 debited A/c XX1234 at SWIGGY on 01-07")
            appendLine("""OUT: {"is_transaction":true,"type":"expense","category":"Food & Dining","merchant":"Swiggy","subscription":false}""")
            appendLine("SMS: Sent Rs.299 from HDFC Bank a/c to rahim@ybl UPI ref 512345")
            appendLine("""OUT: {"is_transaction":true,"type":"expense","category":"Transfer","merchant":"Rahim","subscription":false}""")
            appendLine("SMS: Rs 45000 salary credited to A/c XX1234 NEFT ACME CORP")
            appendLine("""OUT: {"is_transaction":true,"type":"income","category":"Salary","merchant":"Acme Corp","subscription":false}""")
            appendLine("SMS: Rs 999 refunded for cancelled Zomato order")
            appendLine("""OUT: {"is_transaction":true,"type":"income","category":"Food & Dining","merchant":"Zomato","subscription":false}""")
            appendLine("SMS: AutoPay of Rs 499 NETFLIX e-mandate successful")
            appendLine("""OUT: {"is_transaction":true,"type":"expense","category":"Subscriptions","merchant":"Netflix","subscription":true}""")
            appendLine("SMS: INR 1200 spent on Credit Card XX5678 at IOCL PETROL PUMP")
            appendLine("""OUT: {"is_transaction":true,"type":"expense","category":"Transport","merchant":"IOCL Petrol Pump","subscription":false}""")
            appendLine("SMS: Use code GET50 for recharge, limited period offer!")
            appendLine("""OUT: {"is_transaction":false,"type":"none","category":"Other","merchant":"none","subscription":false}""")
            appendLine("SMS: Your OTP is 482913 do not share with anyone")
            appendLine("""OUT: {"is_transaction":false,"type":"none","category":"Other","merchant":"none","subscription":false}""")
            appendLine("SMS: Avl balance Rs 25,000.00 - SBI")
            appendLine("""OUT: {"is_transaction":false,"type":"none","category":"Other","merchant":"none","subscription":false}""")
            appendLine()
            append("SMS: ")
            append(rawMessage)
            appendLine()
            append("OUT:")
        }
    }

    internal fun parse(
        output: String,
        categoryIdByName: Map<String, String>
    ): AiCategorisation? {
        val trimmed = output.trim()
        val jsonBlock = extractJsonObject(trimmed) ?: return null

        // Tolerate camelCase / snake_case variants of the transaction flag.
        val explicitIsTransaction =
            Regex("""["']?(?:is_?transaction|transaction)["']?\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
                .find(jsonBlock)?.groupValues?.get(1)?.lowercase()

        val type = when (
            Regex("""["']?type["']?\s*:\s*["']?([a-z]+)["']?""", RegexOption.IGNORE_CASE)
                .find(jsonBlock)?.groupValues?.get(1)?.lowercase()
        ) {
            "income", "credit", "credited", "received", "refund", "refunded" -> TransactionType.INCOME
            "expense", "debit", "debited", "spent", "sent", "paid" -> TransactionType.EXPENSE
            else -> null
        }

        val categoryName = Regex("""["']?category["']?\s*:\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(jsonBlock)?.groupValues?.get(1)?.trim()

        val merchant = Regex("""["']?merchant["']?\s*:\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(jsonBlock)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() && !it.equals("none", true) }

        val isSubscription = categoryName.equals("subscription", ignoreCase = true) ||
            Regex("""["']?subscription["']?\s*:\s*["']?(true|false)["']?""", RegexOption.IGNORE_CASE)
                .find(jsonBlock)?.groupValues?.get(1)?.equals("true", ignoreCase = true) == true

        val categoryId = when {
            isSubscription -> CATEGORY_SUBSCRIPTION_ID
            else -> lookupCategoryId(categoryName, categoryIdByName)
        }

        // When the flag is missing entirely, infer from the type instead of defaulting to
        // "not a transaction" — that default used to veto perfectly good extractions.
        val isTransaction = when (explicitIsTransaction) {
            "true" -> true
            "false" -> false
            else -> type != null
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
        if (categoryName.isNullOrBlank()) return CATEGORY_OTHER_ID
        if (categoryName.equals("none", true) || categoryName.equals("other", true)) {
            return CATEGORY_OTHER_ID
        }

        // Exact name match (case-insensitive).
        categoryIdByName.entries
            .firstOrNull { (_, name) -> name.equals(categoryName, ignoreCase = true) }
            ?.let { return it.key }

        // The model may echo an id directly.
        categoryIdByName.keys
            .firstOrNull { it.equals(categoryName, ignoreCase = true) }
            ?.let { return it }

        // Normalized comparison ("Food&Dining!" -> "food & dining").
        val normalizedQuery = normalizeCategoryToken(categoryName)
        if (normalizedQuery.isNotBlank()) {
            categoryIdByName.entries
                .firstOrNull { (_, name) -> normalizeCategoryToken(name) == normalizedQuery }
                ?.let { return it.key }

            // Containment either direction ("food" vs "food & dining").
            categoryIdByName.entries
                .firstOrNull { (_, name) ->
                    val n = normalizeCategoryToken(name)
                    n.contains(normalizedQuery) || normalizedQuery.contains(n)
                }
                ?.let { return it.key }

            // Significant-word overlap ("salary income" -> "salary").
            val queryWords = normalizedQuery.split(' ').filter { it.length >= 4 }
            if (queryWords.isNotEmpty()) {
                categoryIdByName.entries
                    .firstOrNull { (_, name) ->
                        val words = normalizeCategoryToken(name).split(' ')
                        queryWords.any { q -> words.any { it.startsWith(q.take(4)) } }
                    }
                    ?.let { return it.key }
            }
        }

        return CATEGORY_OTHER_ID
    }
}

/** Lowercases, strips punctuation and collapses whitespace for tolerant comparisons. */
internal fun normalizeCategoryToken(value: String): String =
    value.lowercase()
        .replace(Regex("[^a-z0-9& ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Extracts the first balanced `{...}` block from raw model output. Handles:
 *  - markdown code fences (```json ... ```);
 *  - prose before/after the object;
 *  - braces inside JSON string values;
 *  - escaped quotes inside strings.
 */
internal fun extractJsonObject(raw: String): String? {
    var text = raw.trim()
    text = text.replace(Regex("(?is)```[a-zA-Z]*"), "").trim()

    val start = text.indexOf('{')
    if (start < 0) return null

    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until text.length) {
        val c = text[i]
        if (escaped) {
            escaped = false
            continue
        }
        when {
            c == '\\' && inString -> escaped = true
            c == '"' -> inString = !inString
            c == '{' && !inString -> depth++
            c == '}' && !inString -> {
                depth--
                if (depth == 0) return text.substring(start, i + 1)
            }
        }
    }
    return null
}
