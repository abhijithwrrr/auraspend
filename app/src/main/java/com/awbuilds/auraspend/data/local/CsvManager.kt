package com.awbuilds.auraspend.data.local

import android.content.Context
import android.net.Uri
import com.awbuilds.auraspend.core.AuraLog
import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object CsvManager {

    private const val TAG = "CsvManager"

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private const val CSV_HEADER =
        "Date,Amount,Type,CategoryId,Note,Merchant,Bank,IsRecurring,RecurrenceFrequency,SourceSmsId\n"

    /** Outcome of an import so the UI can report what actually happened. */
    data class ImportResult(
        val imported: List<Transaction>,
        val skippedRows: Int
    ) {
        val totalRows: Int get() = imported.size + skippedRows
    }

    /**
     * Writes the export straight to the user-picked SAF [uri].
     *
     * There is deliberately no public-Downloads fallback: on API 30+ an app
     * cannot write there without MANAGE_EXTERNAL_STORAGE, so that path threw
     * FileNotFoundException on every launch configuration. If [uri] cannot be
     * opened the caller is told via the return value instead of a crash.
     *
     * @return the number of rows written, or -1 if the stream could not be opened.
     */
    fun exportToCsv(context: Context, transactions: List<Transaction>, uri: Uri): Int {
        val stream = try {
            context.contentResolver.openOutputStream(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuraLog.e(TAG, "Could not open export destination", e)
            null
        }
        if (stream == null) {
            AuraLog.e(TAG, "Export destination unavailable: $uri")
            return -1
        }

        return try {
            OutputStreamWriter(stream, Charsets.UTF_8).buffered().use { writer ->
                writer.write(CSV_HEADER)
                transactions.forEach { t ->
                    writer.write(
                        buildString {
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
                            append(",")
                            append(escapeCsv(t.sourceSmsId ?: ""))
                            append("\n")
                        }
                    )
                }
                writer.flush()
            }
            transactions.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuraLog.e(TAG, "CSV export failed", e)
            -1
        }
    }

    /**
     * Parses a CSV previously produced by [exportToCsv]. Malformed rows are
     * counted and skipped rather than aborting the whole import, and the result
     * reports both numbers so the UI can tell the user what was dropped.
     */
    fun importFromCsv(context: Context, uri: Uri): ImportResult {
        val transactions = mutableListOf<Transaction>()
        var skipped = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    reader.readLine()

                    reader.forEachLine { line ->
                        if (line.isBlank()) return@forEachLine
                        val parts = parseCsvLine(line)
                        if (parts.size < 6) {
                            skipped++
                            return@forEachLine
                        }
                        val parsed = parseTransactionRow(parts)
                        if (parsed == null) skipped++ else transactions.add(parsed)
                    }
                }
            } ?: throw IllegalStateException("Could not open import file: $uri")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep whatever parsed before the failure; the count is reported to the user.
            AuraLog.e(TAG, "CSV import failed", e)
        }

        return ImportResult(imported = transactions, skippedRows = skipped)
    }

    /** Returns null for any row that cannot be represented as a [Transaction]. */
    internal fun parseTransactionRow(parts: List<String>): Transaction? {
        val date = try {
            LocalDateTime.parse(parts[0], DATE_FORMATTER)
        } catch (_: Exception) {
            return null
        }
        val amount = parts[1].toDoubleOrNull() ?: return null
        val type = try {
            TransactionType.valueOf(parts[2])
        } catch (_: Exception) {
            return null
        }
        if (parts[3].isBlank()) return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            categoryId = parts[3],
            note = parts[4],
            merchant = parts[5].ifBlank { null },
            bankName = parts.getOrElse(6) { "" }.ifBlank { null },
            date = date,
            type = type,
            isRecurring = parts.getOrElse(7) { "false" }.toBoolean(),
            sourceSmsId = parts.getOrElse(9) { "" }.ifBlank { null }
        )
    }

    /**
     * Quotes a field when it contains a delimiter, quote or newline, and
     * neutralises spreadsheet formula injection (`=`, `+`, `-`, `@`).
     */
    internal fun escapeCsv(value: String): String {
        val guarded = if (value.isNotEmpty() && value.first() in FORMULA_PREFIXES) "'$value" else value
        return if (guarded.contains(",") || guarded.contains("\"") || guarded.contains("\n") || guarded.contains("\r")) {
            "\"${guarded.replace("\"", "\"\"")}\""
        } else {
            guarded
        }
    }

    /**
     * RFC 4180 field splitter. A doubled quote inside a quoted field is a
     * literal quote, so `""` must not be treated as two quote toggles — the
     * previous implementation dropped it, which broke the export/import
     * round trip for any merchant containing a quote.
     */
    internal fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        result.add(current.toString())
        return result
    }

    private val FORMULA_PREFIXES = charArrayOf('=', '+', '-', '@', '\t', '\r')
}
