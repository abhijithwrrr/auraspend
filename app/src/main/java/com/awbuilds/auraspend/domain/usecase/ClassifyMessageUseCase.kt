package com.awbuilds.auraspend.domain.usecase

import com.awbuilds.auraspend.data.classification.TransactionClassifier
import com.awbuilds.auraspend.domain.model.ParsedBankMessage

class ClassifyMessageUseCase {
    operator fun invoke(rawMessage: String): ParsedBankMessage {
        return TransactionClassifier.classify(rawMessage)
    }
}
