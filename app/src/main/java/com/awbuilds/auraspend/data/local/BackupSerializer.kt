package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

object BackupSerializer {

    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_BUDGETS = "budgets"
    private const val KEY_SUBSCRIPTIONS = "subscriptions"
    private const val VERSION = "version"
    private const val CURRENT_VERSION = 1

    fun serialize(data: BackupData): String {
        val root = JSONObject()
        root.put(VERSION, CURRENT_VERSION)
        root.put(KEY_TRANSACTIONS, serializeTransactions(data.transactions))
        root.put(KEY_CATEGORIES, serializeCategories(data.categories))
        root.put(KEY_BUDGETS, serializeBudgets(data.budgets))
        root.put(KEY_SUBSCRIPTIONS, serializeSubscriptions(data.subscriptions))
        return root.toString(2)
    }

    fun deserialize(json: String): BackupData {
        val root = JSONObject(json)
        return BackupData(
            transactions = deserializeTransactions(root.optJSONArray(KEY_TRANSACTIONS)),
            categories = deserializeCategories(root.optJSONArray(KEY_CATEGORIES)),
            budgets = deserializeBudgets(root.optJSONArray(KEY_BUDGETS)),
            subscriptions = deserializeSubscriptions(root.optJSONArray(KEY_SUBSCRIPTIONS))
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
                    subscriptionName = obj.optString("subscriptionName", "").ifBlank { null }
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
}
