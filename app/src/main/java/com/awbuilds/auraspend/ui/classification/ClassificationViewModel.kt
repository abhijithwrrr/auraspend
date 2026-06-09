package com.awbuilds.auraspend.ui.classification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awbuilds.auraspend.data.classification.ClassifiedSms
import com.awbuilds.auraspend.data.classification.SmsAutoClassifier
import com.awbuilds.auraspend.data.classification.TransactionClassifier
import com.awbuilds.auraspend.domain.model.ParsedBankMessage
import com.awbuilds.auraspend.domain.model.TransactionType
import com.awbuilds.auraspend.domain.usecase.ClassifyMessageUseCase
import com.awbuilds.auraspend.domain.usecase.SaveTransactionUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ClassificationViewModel(
    private val classifyMessageUseCase: ClassifyMessageUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ClassificationViewState())
    val state: StateFlow<ClassificationViewState> = _state.asStateFlow()

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
                if (intent.granted) loadAndClassifyAll()
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
            is ClassificationViewIntent.SetCategories -> {
                _state.update { it.copy(availableCategories = intent.categories) }
            }
            is ClassificationViewIntent.LoadAndClassifyAll -> loadAndClassifyAll()
            is ClassificationViewIntent.SaveClassifiedSms -> saveClassifiedSms(intent.smsId)
            is ClassificationViewIntent.SaveAllClassified -> saveAllClassified()
            is ClassificationViewIntent.DismissClassifiedSms -> dismissClassifiedSms(intent.smsId)
        }
    }

    private fun classify() {
        val message = _state.value.rawMessage
        if (message.isBlank()) return

        val parsed = classifyMessageUseCase(message)
        _state.update {
            it.copy(
                parsedMessage = parsed,
                selectedCategoryId = parsed.categoryId ?: "cat_other",
                manualAmount = parsed.amount?.let { formatAmount(it) } ?: "",
                manualNote = parsed.note ?: "",
                manualType = parsed.type ?: TransactionType.EXPENSE
            )
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

                saveTransactionUseCase(transaction)
                _state.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save") }
            }
        }
    }

    private fun loadAndClassifyAll() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        viewModelScope.launch {
            _state.update { it.copy(isBatchClassifying = true) }

            val classified = SmsAutoClassifier.readAndClassify(
                context = context,
                maxMessages = 50
            )

            val now = System.currentTimeMillis()
            context.getSharedPreferences("auraspend_prefs", Context.MODE_PRIVATE)
                .edit().putLong("last_sms_scan", now).apply()

            _state.update {
                it.copy(
                    classifiedSmsList = classified,
                    smsMessages = classified.map { c -> c.sms },
                    isBatchClassifying = false
                )
            }
        }
    }

    private fun saveClassifiedSms(smsId: String) {
        viewModelScope.launch {
            try {
                val classified = _state.value.classifiedSmsList.find { it.sms.id == smsId } ?: return@launch
                val categories = _state.value.availableCategories
                val categoryId = classified.parsed.categoryId
                val effectiveCategory = if (categories.any { it.id == categoryId }) categoryId else "cat_other"

                val transaction = SmsAutoClassifier.toTransaction(classified, effectiveCategory)
                saveTransactionUseCase(transaction)

                _state.update {
                    it.copy(
                        classifiedSmsList = it.classifiedSmsList.map { c ->
                            if (c.sms.id == smsId) c.copy(isSaved = true) else c
                        }
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to save") }
            }
        }
    }

    private fun saveAllClassified() {
        viewModelScope.launch {
            _state.update { it.copy(isSavingAll = true, error = null) }
            try {
                val categories = _state.value.availableCategories
                val unsaved = _state.value.classifiedSmsList.filter { !it.isSaved }

                for (classified in unsaved) {
                    val categoryId = classified.parsed.categoryId
                    val effectiveCategory = if (categories.any { it.id == categoryId }) categoryId else "cat_other"
                    val transaction = SmsAutoClassifier.toTransaction(classified, effectiveCategory)
                    saveTransactionUseCase(transaction)
                }

                _state.update {
                    it.copy(
                        isSavingAll = false,
                        classifiedSmsList = it.classifiedSmsList.map { c -> c.copy(isSaved = true) },
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSavingAll = false, error = e.message ?: "Failed to save") }
            }
        }
    }

    private fun dismissClassifiedSms(smsId: String) {
        _state.update {
            it.copy(classifiedSmsList = it.classifiedSmsList.filter { c -> c.sms.id != smsId })
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
    }
}
