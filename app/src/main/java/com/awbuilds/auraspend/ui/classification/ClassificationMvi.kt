package com.awbuilds.auraspend.ui.classification

import com.awbuilds.auraspend.ui.core.UiError

import com.awbuilds.auraspend.data.ai.AiModelState
import com.awbuilds.auraspend.data.classification.ClassifiedSms
import com.awbuilds.auraspend.data.classification.DuplicateTransaction
import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.Transaction
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
    val error: UiError? = null,
    val smsMessages: List<SmsInfo> = emptyList(),
    val smsPermissionGranted: Boolean = false,
    val classifiedSmsList: List<ClassifiedSms> = emptyList(),

    /**
     * The triage inbox: only messages still awaiting a decision.
     *
     * Saved and dismissed messages leave this list, so handling one is a
     * one-way action on screen — undo puts it back by reverting the status,
     * which the observer reflows into this list. [classifiedSmsList] stays the
     * full queue and is kept for lookups that need it (`saveAllClassified`,
     * `saveClassifiedSms`).
     */
    val pendingSmsList: List<ClassifiedSms> = emptyList(),
    val isBatchClassifying: Boolean = false,
    val isSavingAll: Boolean = false,

    // On-device AI model
    val consentRequired: Boolean = false,
    val aiModelState: AiModelState = AiModelState.NotDownloaded,
    val isAiEnriching: Boolean = false,
    /** True once the async LLM/memory pass has refined the current classification. */
    val aiRefined: Boolean = false,
    val aiProgressCurrent: Int = 0,
    val aiProgressTotal: Int = 0,

    // Duplicate detection
    val potentialDuplicate: Pair<Transaction, Float>? = null,
    val showDuplicateDialog: Boolean = false,
    val confidenceScore: Float = 0f,
    val merchantConfidence: Float = 0f
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
    object ClearError : ClassificationViewIntent()
    data class SetCategories(val categories: List<Category>) : ClassificationViewIntent()
    object LoadAndClassifyAll : ClassificationViewIntent()
    data class SaveClassifiedSms(val smsId: String) : ClassificationViewIntent()
    object SaveAllClassified : ClassificationViewIntent()
    data class DismissClassifiedSms(val smsId: String) : ClassificationViewIntent()

    // On-device AI model
    data class ConsentResult(val accepted: Boolean) : ClassificationViewIntent()

    /**
     * User declined the categoriser for now. Clears the offer **without** recording
     * consent, so it is not re-asked on this screen, but the model is still
     * downloadable later from Settings.
     */
    object DismissModelConsent : ClassificationViewIntent()
    /**
     * Reverse a triage "swipe right to save". Removes the transaction the save
     * created and returns the message to the inbox, so the gesture is safe to
     * perform by accident — which matters because it is a swipe.
     */
    data class UndoSaveClassifiedSms(val smsId: String) : ClassificationViewIntent()

    /** Reverse a triage "swipe left to dismiss": put the message back in the queue. */
    data class UndoDismissClassifiedSms(val smsId: String) : ClassificationViewIntent()

    object StartModelDownload : ClassificationViewIntent()
    object CancelModelDownload : ClassificationViewIntent()
    object RefreshAiModelState : ClassificationViewIntent()

    // Duplicate detection
    object CheckForDuplicates : ClassificationViewIntent()
    data class DuplicateDetected(val duplicate: Pair<Transaction, Float>) : ClassificationViewIntent()
    object IgnoreDuplicate : ClassificationViewIntent()
    object MarkAsDuplicate : ClassificationViewIntent()
}
