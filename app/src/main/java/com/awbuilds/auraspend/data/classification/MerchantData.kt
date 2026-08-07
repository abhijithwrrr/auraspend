package com.awbuilds.auraspend.data.classification

data class MerchantInfo(
    val merchantName: String,
    val merchantAlias: String,
    val category: String,
    val subcategory: String,
    val keywords: String,
    val confidence: Float
)

data class DuplicateTransaction(
    val originalId: String,
    val duplicateId: String,
    val similarity: Float,
    val reason: String
)
