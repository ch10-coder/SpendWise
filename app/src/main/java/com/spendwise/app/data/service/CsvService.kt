package com.spendwise.app.data.service

import com.spendwise.app.data.local.dao.CategoryDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvService(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun exportTransactionsToCsv(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val transactionsWithCategory = transactionDao.getAllTransactionsWithCategory().first()
            val writer = OutputStreamWriter(outputStream)

            // CSV Header
            writer.write("ID,Date,Title,Type,Amount,Category,Note\n")

            for (item in transactionsWithCategory) {
                val t = item.toTransactionWithCategory()
                val dateStr = dateFormat.format(Date(t.transaction.dateTimestamp))
                val safeTitle = escapeCsvField(t.transaction.title)
                val typeStr = t.transaction.type.name
                val amountStr = String.format(Locale.US, "%.2f", t.transaction.amount)
                val catName = escapeCsvField(t.category?.name ?: "Uncategorized")
                val safeNote = escapeCsvField(t.transaction.note ?: "")

                writer.write("${t.transaction.id},\"$dateStr\",\"$safeTitle\",$typeStr,$amountStr,\"$catName\",\"$safeNote\"\n")
            }

            writer.flush()
            Result.success(transactionsWithCategory.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importTransactionsFromCsv(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            if (lines.isEmpty()) {
                return@withContext Result.success(0)
            }

            val existingCategories = categoryDao.getAllCategories().first().toMutableList()
            val categoryMap = existingCategories.associateBy { it.name.lowercase().trim() }.toMutableMap()

            val newTransactions = mutableListOf<TransactionEntity>()

            // Skip header if present
            val startIndex = if (lines[0].contains("Date", ignoreCase = true) || lines[0].contains("Title", ignoreCase = true)) 1 else 0

            for (i in startIndex until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                val tokens = parseCsvLine(line)
                if (tokens.size < 4) continue

                val isSevenTokens = tokens.size >= 7
                val dateStr = if (isSevenTokens) tokens[1] else tokens[0]
                val title = if (isSevenTokens) tokens[2] else tokens.getOrElse(1) { "Imported" }
                val typeStr = if (isSevenTokens) tokens[3] else tokens.getOrElse(2) { "EXPENSE" }
                val amountStr = if (isSevenTokens) tokens[4] else tokens.getOrElse(3) { "0" }
                val catName = if (isSevenTokens) tokens[5] else tokens.getOrElse(4) { "" }
                val noteStr = if (isSevenTokens) tokens[6] else tokens.getOrElse(5) { "" }

                val timestamp = try {
                    dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                val type = try {
                    TransactionType.valueOf(typeStr.uppercase().trim())
                } catch (e: Exception) {
                    TransactionType.EXPENSE
                }

                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount <= 0) continue

                val cleanCatName = catName.trim()
                var categoryId: Long? = null

                if (cleanCatName.isNotEmpty() && cleanCatName.lowercase() != "uncategorized") {
                    val catEntity = categoryMap[cleanCatName.lowercase()]
                    if (catEntity == null) {
                        val newCat = CategoryEntity(
                            name = cleanCatName,
                            type = type,
                            iconName = if (type == TransactionType.EXPENSE) "ShoppingCart" else "AccountBalanceWallet",
                            colorHex = if (type == TransactionType.EXPENSE) "#3B82F6" else "#10B981"
                        )
                        val insertedId = categoryDao.insertCategory(newCat)
                        val created = newCat.copy(id = insertedId)
                        categoryMap[cleanCatName.lowercase()] = created
                        categoryId = insertedId
                    } else {
                        categoryId = catEntity.id
                    }
                }

                val finalNote: String? = if (noteStr.isBlank()) null else noteStr

                newTransactions.add(
                    TransactionEntity(
                        title = if (title.isBlank()) "Imported Transaction" else title,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        dateTimestamp = timestamp,
                        note = finalNote
                    )
                )
            }

            if (newTransactions.isNotEmpty()) {
                transactionDao.insertTransactions(newTransactions)
            }

            Result.success(newTransactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsvField(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++ // Skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }
}
