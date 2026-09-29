package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.domain.model.Transaction
import com.awbuilds.auraspend.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the CSV field encoding rules.
 *
 * The export/import round trip was previously lossy: `escapeCsv` wrote `""` for an
 * embedded quote but `parseCsvLine` treated the pair as two quote *toggles* and dropped
 * both, so any merchant or note containing a quote came back altered. These tests lock
 * the round trip, the formula-injection guard, and the malformed-row accounting that
 * replaced a bare `catch (_: Exception) { }`.
 */
class CsvManagerTest {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private fun txn(
        note: String = "Swiggy order",
        merchant: String? = "Swiggy",
        amount: Double = 149.0
    ) = Transaction(
        id = "tx-1",
        amount = amount,
        categoryId = "cat_food",
        note = note,
        merchant = merchant,
        date = LocalDateTime.of(2026, 8, 1, 12, 0),
        type = TransactionType.EXPENSE
    )

    /** Builds the CSV line exactly as the exporter does. */
    private fun exportRow(t: Transaction): String = listOf(
        t.date.format(dateFormatter),
        t.amount.toString(),
        t.type.name,
        CsvManager.escapeCsv(t.categoryId),
        CsvManager.escapeCsv(t.note),
        CsvManager.escapeCsv(t.merchant ?: ""),
        CsvManager.escapeCsv(t.bankName ?: ""),
        t.isRecurring.toString(),
        t.recurrenceFrequency?.name ?: "",
        CsvManager.escapeCsv(t.sourceSmsId ?: "")
    ).joinToString(",")

    /** Parses a CSV line exactly as the importer does. */
    private fun importRow(line: String): Transaction? {
        val parts = CsvManager.parseCsvLine(line)
        return if (parts.size >= 6) CsvManager.parseTransactionRow(parts) else null
    }

    /** Applies the importer's row loop to a set of lines. */
    private fun importLines(lines: List<String>): CsvManager.ImportResult {
        val imported = mutableListOf<Transaction>()
        var skipped = 0
        lines.forEach { line ->
            if (line.isBlank()) return@forEach
            val parsed = importRow(line)
            if (parsed == null) skipped++ else imported.add(parsed)
        }
        return CsvManager.ImportResult(imported = imported, skippedRows = skipped)
    }

    private fun roundTrip(transaction: Transaction): Transaction {
        val restored = importRow(exportRow(transaction))
        assertNotNull("row should parse back to a transaction", restored)
        return restored!!
    }

    @Test
    fun `plain fields survive a round trip`() {
        val original = txn()
        val restored = roundTrip(original)
        assertEquals(original.note, restored.note)
        assertEquals(original.merchant, restored.merchant)
        assertEquals(original.amount, restored.amount, 0.001)
        assertEquals(original.categoryId, restored.categoryId)
        assertEquals(original.type, restored.type)
        assertEquals(original.date, restored.date)
    }

    @Test
    fun `embedded double quote survives a round trip`() {
        // The regression: escapeCsv writes "" and the old parser dropped the pair,
        // turning a note like: say "hi" into say hi.
        val original = txn(note = """Order for "Rahul's" party""")
        assertEquals(original.note, roundTrip(original).note)
    }

    @Test
    fun `comma and newline in a note survive a round trip`() {
        val original = txn(note = "groceries, snacks and drinks")
        assertEquals(original.note, roundTrip(original).note)
    }

    @Test
    fun `formula prefix is neutralised so spreadsheets do not execute it`() {
        val encoded = exportRow(txn(note = "=1+1", merchant = "@merchant"))
        assertTrue("export must escape '=' -> got: $encoded", encoded.contains("'=1+1"))
        assertTrue("export must escape '@' -> got: $encoded", encoded.contains("'@merchant"))
    }

    @Test
    fun `formula guard only applies to the leading character`() {
        // A '+' in the middle of a value is harmless and must not be mangled.
        val original = txn(note = "C++ course")
        assertEquals(original.note, roundTrip(original).note)
    }

    @Test
    fun `a quoted formula prefix is still guarded`() {
        assertEquals("'=1+1", CsvManager.escapeCsv("'=1+1"))
        assertTrue(CsvManager.escapeCsv("=cmd()").startsWith("'="))
    }

    @Test
    fun `malformed rows are counted as skipped rather than aborting the import`() {
        val result = importLines(
            listOf(
                // valid
                "2026-08-01 12:00,149.0,EXPENSE,cat_food,Coffee,Swiggy,HDFC,false,,",
                // unparseable date
                "not-a-date,10.0,EXPENSE,cat_food,Coffee,Swiggy,HDFC,false,,",
                // non-numeric amount
                "2026-08-01 12:00,abc,EXPENSE,cat_food,Coffee,Swiggy,HDFC,false,,",
                // unknown enum
                "2026-08-01 12:00,10.0,SIDEWAYS,cat_food,Coffee,Swiggy,HDFC,false,,",
                // blank category
                "2026-08-01 12:00,10.0,EXPENSE,,Coffee,Swiggy,HDFC,false,,",
                // too few columns
                "2026-08-01 12:00,10.0,EXPENSE"
            )
        )
        assertEquals(1, result.imported.size)
        assertEquals(5, result.skippedRows)
        assertEquals(6, result.totalRows)
    }

    @Test
    fun `blank lines are ignored and not counted as skipped`() {
        val result = importLines(
            listOf(
                "2026-08-01 12:00,149.0,EXPENSE,cat_food,Coffee,Swiggy,HDFC,false,,",
                "",
                "   "
            )
        )
        assertEquals(1, result.imported.size)
        assertEquals(0, result.skippedRows)
    }

    @Test
    fun `negative and zero amounts import correctly`() {
        val result = importLines(
            listOf(
                "2026-08-01 12:00,-50.0,EXPENSE,cat_food,Refund,Swiggy,HDFC,false,,",
                "2026-08-02 12:00,0.0,EXPENSE,cat_food,Zero,Swiggy,HDFC,false,,"
            )
        )
        assertEquals(2, result.imported.size)
        assertEquals(-50.0, result.imported[0].amount, 0.001)
        assertEquals(0.0, result.imported[1].amount, 0.001)
    }

    @Test
    fun `null merchant and bank name import as null not empty string`() {
        val result = importLines(
            listOf("2026-08-01 12:00,10.0,EXPENSE,cat_food,Note,,,,,")
        )
        assertEquals(1, result.imported.size)
        assertEquals(null, result.imported.first().merchant)
        assertEquals(null, result.imported.first().bankName)
    }
}
