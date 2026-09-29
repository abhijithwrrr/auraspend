package com.awbuilds.auraspend.ui.classification

import com.awbuilds.auraspend.core.AuraLog
import com.awbuilds.auraspend.ui.core.UiError

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awbuilds.auraspend.AuraSpendApp
import com.awbuilds.auraspend.data.ai.LocalLlmProvider
import com.awbuilds.auraspend.data.ai.ModelDownloadManager
import com.awbuilds.auraspend.data.ai.SmsAiEnricher
import com.awbuilds.auraspend.data.classification.AutoClassificationWorker
import com.awbuilds.auraspend.data.classification.AutoDetect
import com.awbuilds.auraspend.data.classification.ClassifiedSms
import com.awbuilds.auraspend.data.classification.MerchantRepository
import com.awbuilds.auraspend.data.classification.SmsAutoClassifier
import com.awbuilds.auraspend.data.classification.SmsIngestor
import com.awbuilds.auraspend.data.classification.SmsStatus
import com.awbuilds.auraspend.data.classification.TransactionClassifier
import com.awbuilds.auraspend.data.local.entities.SmsMessageEntity
import com.awbuilds.auraspend.data.local.entities.SmsMessageStatus
import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.usecase.ClassifyMessageUseCase
import com.awbuilds.auraspend.domain.usecase.SaveTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.LocalDateTime

class ClassificationViewModel(
    private val classifyMessageUseCase: ClassifyMessageUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ClassificationViewState())
    val state: StateFlow<ClassificationViewState> = _state.asStateFlow()

    init {
        LocalLlmProvider.init(context)
        ModelDownloadManager.sync(context)

        _state.update { it.copy(aiModelState = ModelDownloadManager.state.value) }

        viewModelScope.launch {
            ModelDownloadManager.state.collectLatest { modelState ->
                _state.update { it.copy(aiModelState = modelState) }
            }
        }

        if (!ModelDownloadManager.hasConsent(context) && !ModelDownloadManager.hasModel(context)) {
            _state.update { it.copy(consentRequired = true) }
        }
    }

    fun handleIntent(intent: ClassificationViewIntent) {
        when (intent) {
            is ClassificationViewIntent.MessageChanged -> {
                _state.update { it.copy(rawMessage = intent.message) }
            }
            is ClassificationViewIntent.ClassifyMessage -> classify()
            is ClassificationViewIntent.SelectCategory -> {
                _state.update { it.copy(selectedCategoryId = intent.categoryId) }
            }
            is ClassificationViewIntent.AmountChanged -> {
                _state.update { it.copy(manualAmount = intent.amount) }
            }
            is ClassificationViewIntent.NoteChanged -> {
                _state.update { it.copy(manualNote = intent.note) }
            }
            is ClassificationViewIntent.MerchantChanged -> {
                _state.update { it.copy(manualMerchant = intent.merchant) }
            }
            is ClassificationViewIntent.TypeChanged -> {
                _state.update { it.copy(manualType = intent.type) }
            }
            is ClassificationViewIntent.ToggleManualEntry -> {
                _state.update { it.copy(useManualEntry = !it.useManualEntry) }
            }
            is ClassificationViewIntent.RequestSmsPermission -> {
                // Handled by the UI layer
            }
            is ClassificationViewIntent.SmsPermissionResult -> {
                _state.update { it.copy(smsPermissionGranted = intent.granted) }
                if (intent.granted) {
                    loadAndClassifyAll()
                    // Immediately read + classify + save device messages.
                    AutoClassificationWorker.runNow(context)
                }
            }
            is ClassificationViewIntent.LoadSmsMessages -> loadAndClassifyAll()
            is ClassificationViewIntent.SmsSelected -> {
                _state.update {
                    it.copy(
                        rawMessage = intent.sms.body,
                        parsedMessage = null
                    )
                }
                classify()
            }
            is ClassificationViewIntent.SaveTransaction -> saveTransaction()
            is ClassificationViewIntent.ResetSuccess -> {
                _state.update { it.copy(saveSuccess = false) }
            }
            is ClassificationViewIntent.ClearError -> _state.update { it.copy(error = null) }
            is ClassificationViewIntent.SetCategories -> {
                _state.update { it.copy(availableCategories = intent.categories) }
            }
            is ClassificationViewIntent.LoadAndClassifyAll -> loadAndClassifyAll()
            is ClassificationViewIntent.SaveClassifiedSms -> saveClassifiedSms(intent.smsId)
            is ClassificationViewIntent.SaveAllClassified -> saveAllClassified()
            is ClassificationViewIntent.DismissClassifiedSms -> dismissClassifiedSms(intent.smsId)

            // On-device AI model
            is ClassificationViewIntent.ConsentResult -> {
                _state.update { it.copy(consentRequired = false) }
                if (intent.accepted) {
                    ModelDownloadManager.markConsentGiven(context)
                    // Opt the user into automatic classification of device messages.
                    AutoDetect.setEnabled(context, true)
                    ModelDownloadManager.start(context)
                    AutoClassificationWorker.runNow(context)
                }
            }
            is ClassificationViewIntent.StartModelDownload -> {
                _state.update { it.copy(consentRequired = false) }
                ModelDownloadManager.start(context)
            }
            is ClassificationViewIntent.CancelModelDownload -> ModelDownloadManager.cancel()
            is ClassificationViewIntent.RefreshAiModelState -> {
                ModelDownloadManager.sync(context)
                _state.update { it.copy(aiModelState = ModelDownloadManager.state.value) }
            }

            // Duplicate detection
            is ClassificationViewIntent.CheckForDuplicates -> checkForDuplicates()
            is ClassificationViewIntent.DuplicateDetected -> {
                _state.update { 
                    it.copy(
                        potentialDuplicate = intent.duplicate,
                        showDuplicateDialog = true
                    )
                }
            }
            is ClassificationViewIntent.IgnoreDuplicate -> {
                _state.update { 
                    it.copy(
                        potentialDuplicate = null,
                        showDuplicateDialog = false
                    )
                }
            }
            is ClassificationViewIntent.MarkAsDuplicate -> {
                _state.update { 
                    it.copy(
                        potentialDuplicate = null,
                        showDuplicateDialog = false,
                        saveSuccess = true // Mark as saved since duplicate is detected
                    )
                }
            }
        }
    }

    private fun checkForDuplicates() {
        // Will be called before saving
        val currentState = _state.value
        if (currentState.parsedMessage?.amount == null) return
        
        // For now, just proceed - duplicate checking will be done during save
        // This can be extended to check against existing transactions
    }

    private fun classify() {
        val message = _state.value.rawMessage
        if (message.isBlank()) return

        val parsed = classifyMessageUseCase(message)

        // Get merchant confidence from repository
        var merchantConfidence = 0f
        if (parsed.merchant != null) {
            val merchantSuggestion = MerchantRepository.suggestCategory(parsed.merchant)
            merchantConfidence = merchantSuggestion?.second ?: 0f
        }

        _state.update {
            it.copy(
                parsedMessage = parsed,
                selectedCategoryId = parsed.categoryId ?: "cat_other",
                manualAmount = parsed.amount?.let { formatAmount(it) } ?: "",
                manualNote = parsed.note ?: "",
                manualType = parsed.type ?: TransactionType.EXPENSE,
                merchantConfidence = merchantConfidence,
                aiRefined = false,
                isAiEnriching = true
            )
        }

        // Async refinement: learned memory first, then the on-device LLM. Regex results are shown
        // instantly; the AI result is only applied if the user has not moved on to another message.
        viewModelScope.launch(Dispatchers.IO) {
            val app = context.applicationContext as? AuraSpendApp
            val categoriesByName = _state.value.availableCategories.associate { it.id to it.name }
            val enricher = SmsAiEnricher(context, app?.classificationMemory)
            val probe = ClassifiedSms(
                sms = SmsInfo(id = "", address = "", body = message, timestamp = 0L),
                parsed = parsed
            )
            val enriched = runCatching { enricher.enrich(probe, categoriesByName) }.getOrNull()
                ?: return@launch

            _state.update { current ->
                // The user may have edited or replaced the message while the model was thinking.
                if (current.rawMessage != message) return@update current.copy(isAiEnriching = false)
                val refined = enriched.parsed
                current.copy(
                    parsedMessage = refined,
                    selectedCategoryId = refined.categoryId ?: current.selectedCategoryId,
                    merchantConfidence = MerchantRepository.suggestCategory(refined.merchant ?: "")?.second
                        ?: current.merchantConfidence,
                    aiRefined = true,
                    isAiEnriching = false
                )
            }
        }
    }

    private fun saveTransaction() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                val s = _state.value
                val parsed = s.parsedMessage

                val amount = if (s.useManualEntry) {
                    s.manualAmount.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid amount")
                } else {
                    s.manualAmount.toDoubleOrNull() ?: parsed?.amount ?: throw IllegalArgumentException("No amount detected")
                }

                val note = if (s.useManualEntry) s.manualNote else (s.manualNote.ifBlank { parsed?.note } ?: "Transaction")
                val merchant = if (s.useManualEntry) s.manualMerchant else parsed?.merchant

                val transaction = TransactionClassifier.toTransaction(
                    parsed = parsed ?: ParsedBankMessage(rawMessage = s.rawMessage),
                    categoryId = s.selectedCategoryId,
                    note = note,
                    manualAmount = amount,
                    manualType = s.manualType,
                    manualMerchant = merchant,
                    manualDate = LocalDateTime.now()
                )

                // Check for duplicates before saving
                // Note: This is a simplified check - in production you'd want to fetch actual transactions from DB
                val potentialDuplicate = checkPotentialDuplicate(transaction)
                
                if (potentialDuplicate != null && s.potentialDuplicate == null) {
                    // Show duplicate dialog and pause save
                    _state.update { 
                        it.copy(
                            isSaving = false,
                            potentialDuplicate = potentialDuplicate,
                            showDuplicateDialog = true
                        )
                    }
                    return@launch
                }

                // If user confirmed to ignore or no duplicate found, proceed with save
                saveTransactionUseCase(transaction)
                rememberClassification(transaction)
                _state.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                AuraLog.e(TAG, "Saving transaction failed", e)
                _state.update { it.copy(isSaving = false, error = UiError.SAVE_FAILED) }
            }
        }
    }

    private suspend fun checkPotentialDuplicate(newTransaction: Transaction): Pair<Transaction, Float>? {
        // This is a placeholder - in production, you'd fetch existing transactions from the repository
        // For now, we'll just return null since we can't access the database from here
        // The actual implementation would be:
        // 1. Fetch recent transactions from repository
        // 2. Use duplicate detection to find similar ones
        // 3. Return the most likely duplicate if found
        return null
    }

    private fun loadAndClassifyAll() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val app = context.applicationContext as? AuraSpendApp ?: return
        val dao = app.database.smsMessageDao()

        viewModelScope.launch {
            _state.update { it.copy(isBatchClassifying = true, error = null) }

            // Snapshot new device messages into the persistent queue (idempotent on the provider
            // `_id`). Everything below reads the queue, so the list is stable across background
            // runs instead of being a fresh inbox scan each time.
            withContext(Dispatchers.IO) {
                SmsIngestor.ingest(context = context, dao = dao)
            }
            AutoClassificationWorker.runNow(context)

            // Observe the queue live: rows advance to SAVED/SKIPPED/FAILED as the worker processes
            // them, and the UI reflects real status instead of a one-shot snapshot.
            dao.observeAll()
                .map { rows -> rows.map { it.toClassifiedSms() } }
                .collectLatest { classified ->
                    _state.update {
                        it.copy(
                            classifiedSmsList = classified,
                            smsMessages = classified.map { c -> c.sms },
                            isBatchClassifying = false
                        )
                    }
                }
        }
    }

    /**
     * Persists the merchant/note -> category mapping so repeat transactions skip the LLM entirely.
     * User saves are the strongest signal; failures here must never block a save.
     */
    private fun rememberClassification(transaction: Transaction) {
        val app = context.applicationContext as? AuraSpendApp ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                app.classificationMemory.learn(
                    transaction,
                    com.awbuilds.auraspend.data.local.entities.ClassificationMemoryEntity.SOURCE_USER_SAVE
                )
            }
        }
    }

    private fun SmsMessageEntity.toClassifiedSms(): ClassifiedSms {
        val parsed = if (amount != null && type != null) {
            ParsedBankMessage(
                amount = amount,
                type = TransactionType.valueOf(type!!),
                merchant = merchant,
                categoryId = categoryId,
                date = null,
                note = body.take(100),
                rawMessage = body
            )
        } else {
            TransactionClassifier.classify(body)
        }
        val uiStatus = when (SmsMessageStatus.valueOf(status)) {
            SmsMessageStatus.NEW -> SmsStatus.PENDING
            SmsMessageStatus.PROCESSED -> SmsStatus.CLASSIFYING
            SmsMessageStatus.SAVED -> SmsStatus.SAVED
            SmsMessageStatus.SKIPPED -> SmsStatus.CLASSIFIED
            SmsMessageStatus.FAILED -> SmsStatus.FAILED
        }
        return ClassifiedSms(
            sms = SmsInfo(id = id, address = address, body = body, timestamp = receivedAt),
            parsed = parsed,
            isSaved = status == SmsMessageStatus.SAVED.name,
            isSubscription = isSubscription,
            status = uiStatus
        )
    }

    private fun saveClassifiedSms(smsId: String) {
        viewModelScope.launch {
            try {
                val app = context.applicationContext as? AuraSpendApp ?: return@launch
                val dao = app.database.smsMessageDao()
                val classified = _state.value.classifiedSmsList.find { it.sms.id == smsId } ?: return@launch
                if (classified.status == SmsStatus.SAVED) return@launch

                // Never persist a message without an amount and direction — that would
                // create a meaningless ₹0 expense (OTP / promo / balance alerts).
                if (classified.parsed.amount == null || classified.parsed.type == null) {
                    _state.update { it.copy(error = UiError.NO_TRANSACTION_FOUND) }
                    return@launch
                }

                val categories = _state.value.availableCategories
                val categoryId = classified.parsed.categoryId
                val effectiveCategory = if (categories.any { it.id == categoryId }) categoryId else "cat_other"

                val transaction = SmsAutoClassifier.toTransaction(classified, effectiveCategory)
                saveTransactionUseCase(transaction)
                rememberClassification(transaction)

                dao.getById(smsId)?.let { row ->
                    dao.update(
                        row.copy(
                            status = SmsMessageStatus.SAVED.name,
                            amount = transaction.amount,
                            type = transaction.type.name,
                            merchant = transaction.merchant,
                            categoryId = transaction.categoryId,
                            isSubscription = transaction.isRecurring,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                AuraLog.e(TAG, "Saving all transactions failed", e)
                _state.update { it.copy(error = UiError.SAVE_FAILED) }
            }
        }
    }

    private fun saveAllClassified() {
        viewModelScope.launch {
            _state.update { it.copy(isSavingAll = true, error = null) }
            try {
                val app = context.applicationContext as? AuraSpendApp ?: return@launch
                val dao = app.database.smsMessageDao()
                val categories = _state.value.availableCategories
                // Only persist messages the classifier actually resolved; pending rows are
                // still being processed and skipped rows were rejected as OTP/promos.
                val toSave = _state.value.classifiedSmsList.filter {
                    it.status == SmsStatus.CLASSIFIED &&
                        it.parsed.amount != null &&
                        it.parsed.type != null
                }

                for (classified in toSave) {
                    val categoryId = classified.parsed.categoryId
                    val effectiveCategory = if (categories.any { it.id == categoryId }) categoryId else "cat_other"
                    val transaction = SmsAutoClassifier.toTransaction(classified, effectiveCategory)
                    saveTransactionUseCase(transaction)
                    rememberClassification(transaction)
                    dao.getById(classified.sms.id)?.let { row ->
                        dao.update(
                            row.copy(
                                status = SmsMessageStatus.SAVED.name,
                                amount = transaction.amount,
                                type = transaction.type.name,
                                merchant = transaction.merchant,
                                categoryId = transaction.categoryId,
                                isSubscription = transaction.isRecurring,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                _state.update { it.copy(isSavingAll = false, saveSuccess = true) }
            } catch (e: Exception) {
                AuraLog.e(TAG, "Batch save failed", e)
                _state.update { it.copy(isSavingAll = false, error = UiError.SAVE_FAILED) }
            }
        }
    }

    private fun dismissClassifiedSms(smsId: String) {
        viewModelScope.launch {
            val app = context.applicationContext as? AuraSpendApp ?: return@launch
            val dao = app.database.smsMessageDao()
            val row = dao.getById(smsId) ?: return@launch
            if (row.status != SmsMessageStatus.SAVED.name) {
                dao.update(row.copy(status = SmsMessageStatus.SKIPPED.name, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    companion object {}

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
    }
}

private const val TAG = "ClassificationViewModel"
