package com.awbuilds.auraspend.ui.classification

import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType
import java.time.LocalDateTime

data class ClassificationViewState(
    val rawMessage: String = "",
    val parsedMessage: ParsedBankMessage? = null,
    val availableCategories: List<Category> = emptyList(),
    val selectedCategoryId: String = "cat_other",
    val manualAmount: String = "",
    val manualNote: String = "",
    val manualMerchant: String = "",
    val manualType: TransactionType = TransactionType.EXPENSE,
    val useManualEntry: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val smsMessages: List<SmsInfo> = emptyList(),
    val smsPermissionGranted: Boolean = false
)

data class SmsInfo(
    val id: String,
    val address: String,
    val body: String,
    val timestamp: Long
)

sealed class ClassificationViewIntent {
    data class MessageChanged(val message: String) : ClassificationViewIntent()
    object ClassifyMessage : ClassificationViewIntent()
    data class SelectCategory(val categoryId: String) : ClassificationViewIntent()
    data class AmountChanged(val amount: String) : ClassificationViewIntent()
    data class NoteChanged(val note: String) : ClassificationViewIntent()
    data class MerchantChanged(val merchant: String) : ClassificationViewIntent()
    data class TypeChanged(val type: TransactionType) : ClassificationViewIntent()
    object ToggleManualEntry : ClassificationViewIntent()
    object RequestSmsPermission : ClassificationViewIntent()
    data class SmsPermissionResult(val granted: Boolean) : ClassificationViewIntent()
    object LoadSmsMessages : ClassificationViewIntent()
    data class SmsSelected(val sms: SmsInfo) : ClassificationViewIntent()
    object SaveTransaction : ClassificationViewIntent()
    object ResetSuccess : ClassificationViewIntent()
}
