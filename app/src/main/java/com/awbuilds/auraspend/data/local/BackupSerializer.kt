package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.core.AuraLog
import com.awbuilds.auraspend.data.local.entities.ClassificationMemoryEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageStatus
import com.awbuilds.auraspend.data.local.entities.UnrecognizedSmsEntity
import com.awbuilds.auraspend.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

object BackupSerializer {

    private const val TAG = "BackupSerializer"

    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_BUDGETS = "budgets"
    private const val KEY_SUBSCRIPTIONS = "subscriptions"
    private const val KEY_SMS_MESSAGES = "smsMessages"
    private const val KEY_SAVINGS_GOALS = "savingsGoals"
    private const val KEY_CLASSIFICATION_MEMORY = "classificationMemory"
    private const val KEY_UNRECOGNIZED_SMS = "unrecognizedSms"
    private const val VERSION = "version"

    /**
     * v4 adds `unrecognizedSms`. Reading is tolerant of a missing key, so a v3
     * payload restores with an empty list — the same rule every earlier bump
     * followed, and the reason a downgrade of this app after a restore does not
     * need special handling.
     */
    private const val CURRENT_VERSION = 4

    fun serialize(data: BackupData): String {
        val root = JSONObject()
        root.put(VERSION, CURRENT_VERSION)
        root.put(KEY_TRANSACTIONS, serializeTransactions(data.transactions))
        root.put(KEY_CATEGORIES, serializeCategories(data.categories))
        root.put(KEY_BUDGETS, serializeBudgets(data.budgets))
        root.put(KEY_SUBSCRIPTIONS, serializeSubscriptions(data.subscriptions))
        root.put(KEY_SMS_MESSAGES, serializeSmsMessages(data.smsMessages))
        root.put(KEY_SAVINGS_GOALS, serializeSavingsGoals(data.savingsGoals))
        root.put(KEY_CLASSIFICATION_MEMORY, serializeClassificationMemory(data.classificationMemory))
        root.put(KEY_UNRECOGNIZED_SMS, serializeUnrecognizedSms(data.unrecognizedSms))
        return root.toString(2)
    }

    /**
     * Parses a backup produced by [serialize].
     *
     * Every field is read defensively and an unreadable *entry* is skipped rather
     * than aborting the whole restore. Previously a single missing key, malformed
     * date or unknown enum name threw out of here and crashed the app on a
     * user-initiated "Restore". The top-level structure (must be valid JSON) is
     * the only thing that can still fail, and callers guard that.
     */
    fun deserialize(json: String): BackupData {
        val root = JSONObject(json)
        return BackupData(
            transactions = deserializeTransactions(root.optJSONArray(KEY_TRANSACTIONS)),
            categories = deserializeCategories(root.optJSONArray(KEY_CATEGORIES)),
            budgets = deserializeBudgets(root.optJSONArray(KEY_BUDGETS)),
            subscriptions = deserializeSubscriptions(root.optJSONArray(KEY_SUBSCRIPTIONS)),
            smsMessages = deserializeSmsMessages(root.optJSONArray(KEY_SMS_MESSAGES)),
            savingsGoals = deserializeSavingsGoals(root.optJSONArray(KEY_SAVINGS_GOALS)),
            classificationMemory = deserializeClassificationMemory(
                root.optJSONArray(KEY_CLASSIFICATION_MEMORY)
            ),
            unrecognizedSms = deserializeUnrecognizedSms(root.optJSONArray(KEY_UNRECOGNIZED_SMS))
        )
    }

    /** Iterates [arr], skipping (and logging) any entry that is not a JSON object. */
    private inline fun forEachObject(arr: JSONArray?, section: String, action: (JSONObject) -> Unit) {
        if (arr == null) return
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
            if (obj == null) {
                AuraLog.w(TAG, "Skipping non-object entry $i in $section")
                continue
            }
            action(obj)
        }
    }

    private fun serializeTransactions(transactions: List<Transaction>): JSONArray {
        val arr = JSONArray()
        transactions.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("amount", t.amount)
            obj.put("categoryId", t.categoryId)
            obj.put("note", t.note)
            obj.putOpt("merchant", t.merchant)
            obj.putOpt("bankName", t.bankName)
            obj.put("date", t.date.toString())
            obj.put("type", t.type.name)
            obj.put("isRecurring", t.isRecurring)
            t.recurrenceFrequency?.let { obj.put("recurrenceFrequency", it.name) }
            t.nextDueDate?.let { obj.put("nextDueDate", it.toString()) }
            t.subscriptionName?.let { obj.put("subscriptionName", it) }
            t.sourceSmsId?.let { obj.put("sourceSmsId", it) }
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeTransactions(arr: JSONArray?): List<Transaction> {
        val list = mutableListOf<Transaction>()
        forEachObject(arr, KEY_TRANSACTIONS) { obj ->
            val id = obj.optString("id").ifBlank { null } ?: return@forEachObject
            val amount = obj.optDoubleOrNull("amount") ?: return@forEachObject
            val categoryId = obj.optString("categoryId").ifBlank { null } ?: return@forEachObject
            val date = parseDateTime(obj.optString("date")) ?: return@forEachObject
            val type = enumOrNull<TransactionType>(obj.optString("type")) ?: return@forEachObject

            list.add(
                Transaction(
                    id = id,
                    amount = amount,
                    categoryId = categoryId,
                    note = obj.optString("note", ""),
                    merchant = obj.optString("merchant", "").ifBlank { null },
                    bankName = obj.optString("bankName", "").ifBlank { null },
                    date = date,
                    type = type,
                    isRecurring = obj.optBoolean("isRecurring", false),
                    recurrenceFrequency = enumOrNull<RecurrenceFrequency>(
                        obj.optString("recurrenceFrequency", "")
                    ),
                    nextDueDate = parseDateTime(obj.optString("nextDueDate", "")),
                    subscriptionName = obj.optString("subscriptionName", "").ifBlank { null },
                    sourceSmsId = obj.optString("sourceSmsId", "").ifBlank { null }
                )
            )
        }
        return list
    }

    private fun serializeCategories(categories: List<Category>): JSONArray {
        val arr = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("icon", c.icon)
            obj.put("color", c.color)
            obj.put("isDefault", c.isDefault)
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeCategories(arr: JSONArray?): List<Category> {
        val list = mutableListOf<Category>()
        forEachObject(arr, KEY_CATEGORIES) { obj ->
            val id = obj.optString("id").ifBlank { null } ?: return@forEachObject
            val name = obj.optString("name").ifBlank { null } ?: return@forEachObject
            list.add(
                Category(
                    id = id,
                    name = name,
                    icon = obj.optString("icon", ""),
                    color = obj.optInt("color"),
                    isDefault = obj.optBoolean("isDefault", false)
                )
            )
        }
        return list
    }

    private fun serializeBudgets(budgets: List<Budget>): JSONArray {
        val arr = JSONArray()
        budgets.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("categoryId", b.categoryId)
            obj.put("limitAmount", b.limitAmount)
            obj.put("spentAmount", b.spentAmount)
            obj.put("period", b.period.name)
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeBudgets(arr: JSONArray?): List<Budget> {
        val list = mutableListOf<Budget>()
        forEachObject(arr, KEY_BUDGETS) { obj ->
            val id = obj.optString("id").ifBlank { null } ?: return@forEachObject
            val categoryId = obj.optString("categoryId").ifBlank { null } ?: return@forEachObject
            val limit = obj.optDoubleOrNull("limitAmount") ?: return@forEachObject
            val period = enumOrNull<BudgetPeriod>(obj.optString("period")) ?: return@forEachObject
            list.add(
                Budget(
                    id = id,
                    categoryId = categoryId,
                    limitAmount = limit,
                    spentAmount = obj.optDouble("spentAmount", 0.0),
                    period = period
                )
            )
        }
        return list
    }

    private fun serializeSubscriptions(subscriptions: List<Subscription>): JSONArray {
        val arr = JSONArray()
        subscriptions.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("amount", s.amount)
            obj.put("categoryId", s.categoryId)
            obj.put("billingCycle", s.billingCycle.name)
            obj.put("nextBillingDate", s.nextBillingDate.toString())
            obj.put("active", s.active)
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeSubscriptions(arr: JSONArray?): List<Subscription> {
        val list = mutableListOf<Subscription>()
        forEachObject(arr, KEY_SUBSCRIPTIONS) { obj ->
            val id = obj.optString("id").ifBlank { null } ?: return@forEachObject
            val name = obj.optString("name").ifBlank { null } ?: return@forEachObject
            val amount = obj.optDoubleOrNull("amount") ?: return@forEachObject
            val categoryId = obj.optString("categoryId").ifBlank { null } ?: return@forEachObject
            val cycle = enumOrNull<RecurrenceFrequency>(obj.optString("billingCycle")) ?: return@forEachObject
            val nextBilling = parseDateTime(obj.optString("nextBillingDate")) ?: return@forEachObject
            list.add(
                Subscription(
                    id = id,
                    name = name,
                    amount = amount,
                    categoryId = categoryId,
                    billingCycle = cycle,
                    nextBillingDate = nextBilling,
                    active = obj.optBoolean("active", true)
                )
            )
        }
        return list
    }

    private fun serializeSmsMessages(messages: List<SmsMessageEntity>): JSONArray {
        val arr = JSONArray()
        messages.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("address", m.address)
            obj.put("body", m.body)
            obj.put("receivedAt", m.receivedAt)
            obj.put("status", m.status)
            obj.putOpt("amount", m.amount)
            obj.putOpt("type", m.type)
            obj.putOpt("merchant", m.merchant)
            obj.putOpt("categoryId", m.categoryId)
            obj.put("isSubscription", m.isSubscription)
            obj.put("confidence", m.confidence.toDouble())
            obj.put("attempts", m.attempts)
            obj.put("updatedAt", m.updatedAt)
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeSmsMessages(arr: JSONArray?): List<SmsMessageEntity> {
        val list = mutableListOf<SmsMessageEntity>()
        forEachObject(arr, KEY_SMS_MESSAGES) { obj ->
            val id = obj.optString("id").ifBlank { null } ?: return@forEachObject
            val address = obj.optString("address").ifBlank { null } ?: return@forEachObject
            val body = obj.optString("body").ifBlank { null } ?: return@forEachObject
            val receivedAt = obj.optLong("receivedAt", 0L)
            val rawStatus = obj.optString("status", SmsMessageStatus.NEW.name)
            list.add(
                SmsMessageEntity(
                    id = id,
                    address = address,
                    body = body,
                    receivedAt = receivedAt,
                    status = enumOrNull<SmsMessageStatus>(rawStatus)?.name ?: SmsMessageStatus.NEW.name,
                    amount = if (obj.has("amount") && !obj.isNull("amount")) obj.optDouble("amount") else null,
                    type = obj.optString("type", "").ifBlank { null },
                    merchant = obj.optString("merchant", "").ifBlank { null },
                    categoryId = obj.optString("categoryId", "").ifBlank { null },
                    isSubscription = obj.optBoolean("isSubscription", false),
                    confidence = obj.optDouble("confidence", 0.0).toFloat(),
                    attempts = obj.optInt("attempts", 0),
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            )
        }
        return list
    }

    private fun serializeSavingsGoals(goals: List<SavingsGoal>): JSONArray {
        val arr = JSONArray()
        goals.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("targetAmount", g.targetAmount)
            obj.put("currentAmount", g.currentAmount)
            g.deadline?.let { obj.put("deadline", it) }
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeSavingsGoals(arr: JSONArray?): List<SavingsGoal> {
        val list = mutableListOf<SavingsGoal>()
        forEachObject(arr, KEY_SAVINGS_GOALS) { obj ->
            val id = obj.optString("id").ifBlank { null } ?: return@forEachObject
            val name = obj.optString("name").ifBlank { null } ?: return@forEachObject
            val target = obj.optDoubleOrNull("targetAmount") ?: return@forEachObject
            list.add(
                SavingsGoal(
                    id = id,
                    name = name,
                    targetAmount = target,
                    currentAmount = obj.optDouble("currentAmount", 0.0),
                    deadline = if (obj.has("deadline") && !obj.isNull("deadline")) {
                        obj.optLong("deadline")
                    } else {
                        null
                    }
                )
            )
        }
        return list
    }

    private fun serializeUnrecognizedSms(entries: List<UnrecognizedSmsEntity>): JSONArray {
        val arr = JSONArray()
        entries.forEach { u ->
            val obj = JSONObject()
            obj.put("id", u.id)
            obj.put("sender", u.sender)
            obj.put("body", u.body)
            obj.put("receivedAt", u.receivedAt)
            obj.put("createdAt", u.createdAt)
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeUnrecognizedSms(arr: JSONArray?): List<UnrecognizedSmsEntity> {
        val list = mutableListOf<UnrecognizedSmsEntity>()
        forEachObject(arr, KEY_UNRECOGNIZED_SMS) { obj ->
            val id = obj.optString("id").ifBlank { null } ?: return@forEachObject
            val body = obj.optString("body").ifBlank { null } ?: return@forEachObject
            list.add(
                UnrecognizedSmsEntity(
                    id = id,
                    sender = obj.optString("sender"),
                    body = body,
                    receivedAt = obj.optLong("receivedAt"),
                    createdAt = obj.optLong("createdAt")
                )
            )
        }
        return list
    }

    private fun serializeClassificationMemory(entries: List<ClassificationMemoryEntity>): JSONArray {
        val arr = JSONArray()
        entries.forEach { m ->
            val obj = JSONObject()
            obj.put("memoryKey", m.memoryKey)
            obj.put("categoryId", m.categoryId)
            obj.putOpt("type", m.type)
            obj.put("source", m.source)
            obj.put("hits", m.hits)
            obj.put("updatedAt", m.updatedAt)
            arr.put(obj)
        }
        return arr
    }

    private fun deserializeClassificationMemory(arr: JSONArray?): List<ClassificationMemoryEntity> {
        val list = mutableListOf<ClassificationMemoryEntity>()
        forEachObject(arr, KEY_CLASSIFICATION_MEMORY) { obj ->
            val key = obj.optString("memoryKey").ifBlank { null } ?: return@forEachObject
            val categoryId = obj.optString("categoryId").ifBlank { null } ?: return@forEachObject
            list.add(
                ClassificationMemoryEntity(
                    memoryKey = key,
                    categoryId = categoryId,
                    type = obj.optString("type", "").ifBlank { null },
                    source = obj.optString("source", "auto").ifBlank { "auto" },
                    hits = obj.optInt("hits", 1),
                    updatedAt = obj.optLong("updatedAt", 0L)
                )
            )
        }
        return list
    }

    /**
     * [JSONObject.getDouble] throws when the value is a non-numeric string, and
     * returns 0.0 for a missing key — which we do not want to treat as a real
     * amount. Distinguish the two by requiring the key to exist and parse.
     */
    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val raw = opt(key) ?: return null
        if (raw is Number) return raw.toDouble()
        return raw.toString().toDoubleOrNull()
    }

    private fun parseDateTime(value: String): LocalDateTime? {
        if (value.isBlank()) return null
        return try {
            LocalDateTime.parse(value)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Unparseable date '$value' — skipping entry", e)
            null
        }
    }

    /** Returns null for a blank or unknown enum name instead of throwing. */
    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? {
        if (name.isBlank()) return null
        return try {
            enumValueOf<T>(name)
        } catch (e: Exception) {
            AuraLog.w(TAG, "Unknown ${T::class.simpleName} '$name' — skipping entry", e)
            null
        }
    }
}
