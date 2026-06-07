package com.awbuilds.auraspend.domain.usecase

import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.repository.TransactionRepository

class SaveTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.saveTransaction(transaction)
    }
}
