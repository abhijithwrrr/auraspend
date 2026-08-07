package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.data.local.entities.SmsMessageEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageStatus
import com.awbuilds.auraspend.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

object BackupSerializer {

    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_BUDGETS = "budgets"
    private const val KEY_SUBSCRIPTIONS = "subscriptions"
    private const val KEY_SMS_MESSAGES = "smsMessages"
    private const val VERSION = "version"
    private const val CURRENT_VERSION = 2

    fun serialize(data: BackupData): String {
        val root = JSONObject()
        root.put(VERSION, CURRENT_VERSION)
        root.put(KEY_TRANSACTIONS, serializeTransactions(data.transactions))
        root.put(KEY_CATEGORIES, serializeCategories(data.categories))
        root.put(KEY_BUDGETS, serializeBudgets(data.budgets))
        root.put(KEY_SUBSCRIPTIONS, serializeSubscriptions(data.subscriptions))
        root.put(KEY_SMS_MESSAGES, serializeSmsMessages(data.smsMessages))
        return root.toString(2)
    }

    fun deserialize(json: String): BackupData {
        val root = JSONObject(json)
        return BackupData(
            transactions = deserializeTransactions(root.optJSONArray(KEY_TRANSACTIONS)),
            categories = deserializeCategories(root.optJSONArray(KEY_CATEGORIES)),
            budgets = deserializeBudgets(root.optJSONArray(KEY_BUDGETS)),
            subscriptions = deserializeSubscriptions(root.optJSONArray(KEY_SUBSCRIPTIONS)),
            smsMessages = deserializeSmsMessages(root.optJSONArray(KEY_SMS_MESSAGES))
        )
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
        if (arr == null) return emptyList()
        val list = mutableListOf<Transaction>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Transaction(
                    id = obj.getString("id"),
                    amount = obj.getDouble("amount"),
                    categoryId = obj.getString("categoryId"),
                    note = obj.optString("note", ""),
                    merchant = obj.optString("merchant", "").ifBlank { null },
                    bankName = obj.optString("bankName", "").ifBlank { null },
                    date = LocalDateTime.parse(obj.getString("date")),
                    type = TransactionType.valueOf(obj.getString("type")),
                    isRecurring = obj.optBoolean("isRecurring", false),
                    recurrenceFrequency = obj.optString("recurrenceFrequency", "")
                        .takeIf { it.isNotBlank() }?.let { RecurrenceFrequency.valueOf(it) },
                    nextDueDate = obj.optString("nextDueDate", "")
                        .takeIf { it.isNotBlank() }?.let { LocalDateTime.parse(it) },
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
        if (arr == null) return emptyList()
        val list = mutableListOf<Category>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Category(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    icon = obj.optString("icon", ""),
                    color = obj.getInt("color"),
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
        if (arr == null) return emptyList()
        val list = mutableListOf<Budget>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Budget(
                    id = obj.getString("id"),
                    categoryId = obj.getString("categoryId"),
                    limitAmount = obj.getDouble("limitAmount"),
                    spentAmount = obj.optDouble("spentAmount", 0.0),
                    period = BudgetPeriod.valueOf(obj.getString("period"))
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
        if (arr == null) return emptyList()
        val list = mutableListOf<Subscription>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Subscription(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    amount = obj.getDouble("amount"),
                    categoryId = obj.getString("categoryId"),
                    billingCycle = RecurrenceFrequency.valueOf(obj.getString("billingCycle")),
                    nextBillingDate = LocalDateTime.parse(obj.getString("nextBillingDate")),
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
        if (arr == null) return emptyList()
        val list = mutableListOf<SmsMessageEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rawStatus = obj.optString("status", SmsMessageStatus.NEW.name)
            list.add(
                SmsMessageEntity(
                    id = obj.getString("id"),
                    address = obj.getString("address"),
                    body = obj.getString("body"),
                    receivedAt = obj.getLong("receivedAt"),
                    status = runCatching { SmsMessageStatus.valueOf(rawStatus) }
                        .getOrDefault(SmsMessageStatus.NEW).name,
                    amount = if (obj.has("amount") && !obj.isNull("amount")) obj.getDouble("amount") else null,
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
}
