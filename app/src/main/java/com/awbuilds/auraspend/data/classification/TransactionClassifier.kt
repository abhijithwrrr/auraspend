package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import java.time.LocalDateTime
import java.util.UUID

object TransactionClassifier {

    fun classify(rawMessage: String): ParsedBankMessage {
        return BankMessageParser.parse(rawMessage)
    }

    fun toTransaction(
        parsed: ParsedBankMessage,
        categoryId: String? = null,
        note: String? = null,
        manualAmount: Double? = null,
        manualType: TransactionType? = null,
        manualMerchant: String? = null,
        manualDate: LocalDateTime? = null
    ): Transaction {
        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = manualAmount ?: parsed.amount ?: 0.0,
            categoryId = categoryId ?: parsed.categoryId ?: "cat_other",
            note = note ?: parsed.note ?: parsed.rawMessage.take(100),
            merchant = manualMerchant ?: parsed.merchant,
            bankName = parsed.bankName,
            date = manualDate ?: parsed.date ?: LocalDateTime.now(),
            type = manualType ?: parsed.type ?: TransactionType.EXPENSE
        )
    }
}
