package com.awbuilds.auraspend.domain.model

data class SavingsGoal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null // epoch millis, optional
)
