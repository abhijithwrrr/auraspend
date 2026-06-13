package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.data.local.entities.SavingsGoalEntity
import com.awbuilds.auraspend.domain.model.SavingsGoal

fun SavingsGoalEntity.toDomain(): SavingsGoal = SavingsGoal(
    id = this.id,
    name = this.name,
    targetAmount = this.targetAmount,
    currentAmount = this.currentAmount,
    deadline = this.deadline
)

fun SavingsGoal.toEntity(): SavingsGoalEntity = SavingsGoalEntity(
    id = this.id,
    name = this.name,
    targetAmount = this.targetAmount,
    currentAmount = this.currentAmount,
    deadline = this.deadline
)
