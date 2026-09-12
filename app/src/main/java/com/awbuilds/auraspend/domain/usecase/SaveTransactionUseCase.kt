package com.awbuilds.auraspend.domain.usecase

import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.repository.TransactionRepository

class SaveTransactionUseCase(
    private val repository: TransactionRepository
) {
    // PII masking happens at the repository boundary (TransactionRepositoryImpl) so
    // EVERY write path is covered: SMS pipeline, Smart Add, manual add, CSV import,
    // and Drive restore.
    suspend operator fun invoke(transaction: Transaction) {
        repository.saveTransaction(transaction)
    }
}
