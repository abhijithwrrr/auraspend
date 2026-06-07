package com.awbuilds.auraspend.data.local

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object CsvManager {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val CSV_HEADER = "Date,Amount,Type,CategoryId,Note,Merchant,Bank,IsRecurring,RecurrenceFrequency\n"

    fun exportToCsv(context: Context, transactions: List<Transaction>): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "AuraSpend_export_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.csv"
        val file = File(downloadsDir, fileName)

        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.write(CSV_HEADER)
            transactions.forEach { t ->
                val line = buildString {
                    append(t.date.format(DATE_FORMATTER))
                    append(",")
                    append(t.amount)
                    append(",")
                    append(t.type.name)
                    append(",")
                    append(escapeCsv(t.categoryId))
                    append(",")
                    append(escapeCsv(t.note))
                    append(",")
                    append(escapeCsv(t.merchant ?: ""))
                    append(",")
                    append(escapeCsv(t.bankName ?: ""))
                    append(",")
                    append(t.isRecurring)
                    append(",")
                    append(t.recurrenceFrequency?.name ?: "")
                    append("\n")
                }
                writer.write(line)
            }
        }

        return file
    }

    fun importFromCsv(context: Context, uri: Uri): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                // Skip header
                reader.readLine()

                reader.forEachLine { line ->
                    val parts = parseCsvLine(line)
                    if (parts.size >= 6) {
                        try {
                            val date = LocalDateTime.parse(parts[0], DATE_FORMATTER)
                            val amount = parts[1].toDoubleOrNull() ?: return@forEachLine
                            val type = try { TransactionType.valueOf(parts[2]) } catch (_: Exception) { return@forEachLine }

                            transactions.add(
                                Transaction(
                                    id = UUID.randomUUID().toString(),
                                    amount = amount,
                                    categoryId = parts[3],
                                    note = parts[4],
                                    merchant = parts[5].ifBlank { null },
                                    bankName = parts.getOrElse(6) { "" }.ifBlank { null },
                                    date = date,
                                    type = type,
                                    isRecurring = parts.getOrElse(7) { "false" }.toBoolean()
                                )
                            )
                        } catch (_: Exception) { }
                    }
                }
            }
        }

        return transactions
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
