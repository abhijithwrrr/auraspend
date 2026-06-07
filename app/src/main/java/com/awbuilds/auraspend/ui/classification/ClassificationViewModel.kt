package com.awbuilds.auraspend.ui.classification

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                if (intent.granted) loadSmsMessages()
            }
            is ClassificationViewIntent.LoadSmsMessages -> loadSmsMessages()
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

                val transaction = com.awbuilds.auraspend.data.classification.TransactionClassifier.toTransaction(
                    parsed = parsed ?: com.awbuilds.auraspend.domain.model.ParsedBankMessage(rawMessage = s.rawMessage),
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

    private fun loadSmsMessages() {
        viewModelScope.launch {
            try {
                val messages = queryBankSms()
                _state.update { it.copy(smsMessages = messages) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to load SMS: ${e.message}") }
            }
        }
    }

    private fun queryBankSms(): List<SmsInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return emptyList()

        val bankKeywords = listOf(
            "HDFC", "ICICI", "SBI", "Axis", "Kotak", "Yes Bank",
            "PNB", "Canara", "BOB", "UPI", "credited", "debited",
            "A/c", "account", "transaction", "spent", "paid"
        )

        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )
        val selection = bankKeywords.joinToString(" OR ") {
            "${Telephony.Sms.Inbox.BODY} LIKE '%$it%'"
        }
        val sortOrder = "${Telephony.Sms.Inbox.DATE} DESC LIMIT 50"

        val cursor: Cursor? = context.contentResolver.query(
            uri, projection, selection, null, sortOrder
        )

        val messages = mutableListOf<SmsInfo>()
        cursor?.use { c ->
            val idIdx = c.getColumnIndex(Telephony.Sms.Inbox._ID)
            val addrIdx = c.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIdx = c.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIdx = c.getColumnIndex(Telephony.Sms.Inbox.DATE)

            while (c.moveToNext()) {
                val id = if (idIdx >= 0) c.getString(idIdx) else ""
                val addr = if (addrIdx >= 0) c.getString(addrIdx) else ""
                val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L

                // Filter to likely bank messages
                val isBankSms = bankKeywords.any { keyword ->
                    body.contains(keyword, ignoreCase = true) ||
                            addr.contains(keyword, ignoreCase = true)
                }

                if (isBankSms) {
                    messages.add(SmsInfo(id = id, address = addr, body = body, timestamp = date))
                }
            }
        }

        return messages.sortedByDescending { it.timestamp }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            String.format("%.2f", amount)
        }
    }
}
