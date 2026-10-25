package com.spendwise.app.data.service

import com.spendwise.app.data.local.dao.AccountDao
import com.spendwise.app.data.local.dao.CategoryDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Outcome of a CSV import. */
data class CsvImportSummary(
    val imported: Int,
    val skippedDuplicates: Int,
    val skippedInvalid: Int
)

class CsvService(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao
) {
    private val exportFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

    /** Date-time layouts accepted on import, including common Indian bank statement formats. */
    private val dateTimeFormats = listOf(
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm",
        "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm"
    ).map { DateTimeFormatter.ofPattern(it, Locale.US) }
    private val dateFormats = listOf(
        "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "dd MMM yyyy", "dd-MMM-yyyy", "d/M/yyyy"
    ).map { DateTimeFormatter.ofPattern(it, Locale.US) }

    suspend fun exportTransactionsToCsv(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val transactionsWithCategory = transactionDao.getAllTransactionsWithCategory().first()
            val accountNames = accountDao.getAllAccounts().first().associate { it.id to it.name }
            val zone = ZoneId.systemDefault()

            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                // BOM so Excel opens ₹ and Hindi text correctly
                writer.write("﻿")
                writer.write("ID,Date,Title,Type,Amount,Category,Account,Note,ToAccount\n")

                for (item in transactionsWithCategory) {
                    val t = item.toTransactionWithCategory()
                    val tx = t.transaction
                    val dateStr = java.time.Instant.ofEpochMilli(tx.dateTimestamp).atZone(zone).format(exportFormat)
                    val fields = listOf(
                        tx.id.toString(),
                        dateStr,
                        tx.title,
                        tx.type.name,
                        String.format(Locale.US, "%.2f", tx.amount),
                        t.category?.name ?: "Uncategorized",
                        tx.accountId?.let { accountNames[it] } ?: "",
                        tx.note ?: "",
                        tx.toAccountId?.let { accountNames[it] } ?: ""
                    )
                    writer.write(fields.joinToString(",") { csvField(it) })
                    writer.write("\n")
                }
                writer.flush()
            }
            Result.success(transactionsWithCategory.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importTransactionsFromCsv(inputStream: InputStream): Result<CsvImportSummary> = withContext(Dispatchers.IO) {
        try {
            val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.removePrefix("﻿")
            val rows = parseCsv(text).filter { row -> row.any { it.isNotBlank() } }
            if (rows.isEmpty()) {
                return@withContext Result.success(CsvImportSummary(0, 0, 0))
            }

            val categoryMap = categoryDao.getAllCategories().first()
                .associateBy { it.name.lowercase().trim() }
                .toMutableMap()
            val accounts = accountDao.getAllAccounts().first()
            val accountsByName = accounts.associateBy { it.name.lowercase().trim() }
            val accountIds = accounts.map { it.id }.toSet()

            // Skip rows that already exist, so importing the same file twice does not double everything
            val existingKeys = transactionDao.getAllTransactionsList()
                .map { duplicateKey(it.dateTimestamp, it.amount, it.title, it.type) }
                .toMutableSet()

            val header = rows.first().map { it.trim().lowercase() }
            val hasHeader = "date" in header || "title" in header || "amount" in header
            val columns = if (hasHeader) header else emptyList()
            val dataRows = if (hasHeader) rows.drop(1) else rows

            fun column(row: List<String>, name: String, positional: Int?): String {
                if (hasHeader) {
                    val index = columns.indexOf(name)
                    return if (index >= 0) row.getOrElse(index) { "" }.trim() else ""
                }
                return positional?.let { row.getOrElse(it) { "" }.trim() } ?: ""
            }

            fun resolveAccount(value: String): Long? {
                if (value.isBlank()) return null
                accountsByName[value.lowercase()]?.let { return it.id }
                // Older exports wrote the account id instead of its name
                return value.toLongOrNull()?.takeIf { it in accountIds }
            }

            val newTransactions = mutableListOf<TransactionEntity>()
            var skippedDuplicates = 0
            var skippedInvalid = 0

            for (row in dataRows) {
                // Without a header, support both "ID,Date,Title,..." exports and the short "Date,Title,Type,Amount,..." layout
                val offset = if (!hasHeader && row.size >= 7) 1 else 0
                val dateStr = column(row, "date", offset)
                val title = column(row, "title", offset + 1)
                val typeStr = column(row, "type", offset + 2)
                val amountStr = column(row, "amount", offset + 3)
                val catName = column(row, "category", offset + 4)
                val accountStr = column(row, "account", if (offset == 1) 6 else null)
                val noteStr = column(row, "note", if (offset == 1) 7 else 5)
                val toAccountStr = column(row, "toaccount", if (offset == 1) 8 else null)

                val amount = amountStr.replace(Regex("[^0-9.\\-]"), "").toDoubleOrNull()?.let { abs(it) }
                if (amount == null || amount == 0.0) {
                    skippedInvalid++
                    continue
                }

                val type = TransactionType.entries.firstOrNull { it.name.equals(typeStr.trim(), ignoreCase = true) }
                    ?: TransactionType.EXPENSE
                val timestamp = parseTimestamp(dateStr) ?: System.currentTimeMillis()
                val finalTitle = title.ifBlank { "Imported Transaction" }

                val key = duplicateKey(timestamp, amount, finalTitle, type)
                if (key in existingKeys) {
                    skippedDuplicates++
                    continue
                }
                existingKeys += key

                val accountId = resolveAccount(accountStr)
                val toAccountId = if (type == TransactionType.TRANSFER) resolveAccount(toAccountStr) else null

                var categoryId: Long? = null
                val cleanCatName = catName.trim()
                if (type != TransactionType.TRANSFER && cleanCatName.isNotEmpty() && !cleanCatName.equals("uncategorized", ignoreCase = true)) {
                    val existing = categoryMap[cleanCatName.lowercase()]
                    categoryId = if (existing != null) {
                        existing.id
                    } else {
                        val newCat = CategoryEntity(
                            name = cleanCatName,
                            type = type,
                            iconName = if (type == TransactionType.EXPENSE) "ShoppingCart" else "AccountBalanceWallet",
                            colorHex = if (type == TransactionType.EXPENSE) "#3B82F6" else "#10B981"
                        )
                        val insertedId = categoryDao.insertCategory(newCat)
                        categoryMap[cleanCatName.lowercase()] = newCat.copy(id = insertedId)
                        insertedId
                    }
                }

                newTransactions += TransactionEntity(
                    title = finalTitle,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    dateTimestamp = timestamp,
                    note = noteStr.ifBlank { null }
                )
            }

            if (newTransactions.isNotEmpty()) {
                transactionDao.insertTransactions(newTransactions)
            }

            Result.success(CsvImportSummary(newTransactions.size, skippedDuplicates, skippedInvalid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun duplicateKey(timestamp: Long, amount: Double, title: String, type: TransactionType): String =
        "${timestamp / 1000}|${String.format(Locale.US, "%.2f", amount)}|${title.trim().lowercase()}|${type.name}"

    private fun parseTimestamp(value: String): Long? {
        val text = value.trim()
        if (text.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        for (format in dateTimeFormats) {
            runCatching { LocalDateTime.parse(text, format) }.getOrNull()?.let {
                return it.atZone(zone).toInstant().toEpochMilli()
            }
        }
        for (format in dateFormats) {
            runCatching { LocalDate.parse(text, format) }.getOrNull()?.let {
                // Date-only rows land at noon so they never slip into the neighbouring day
                return it.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun csvField(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    /** RFC 4180 parser: handles quoted fields, escaped quotes and line breaks inside quotes. */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> {
                    row += field.toString()
                    field.setLength(0)
                }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += field.toString()
                    field.setLength(0)
                    rows += row
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows
    }
}
