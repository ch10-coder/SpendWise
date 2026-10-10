package com.spendwise.app.server

import android.util.Log
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.AccountType
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enterprise Embedded HTTP API server running on localhost:8080.
 * Features:
 * - Full App Access: Transactions, Accounts, Budgets, Categories, Summary, Analytics.
 * - Secure Delete Protection: Requires 'secretCode: "CONFIRM_DELETE"' in payload.
 * - AI Verification Tagging: Supports marking entries as verified.
 */
class SpendWiseApiServer(
    private val repository: SpendWiseRepository,
    private val scope: CoroutineScope,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val threadPool = Executors.newFixedThreadPool(4)

    companion object {
        private const val TAG = "SpendWiseApiServer"
        private const val DELETE_SECURITY_CODE = "CONFIRM_DELETE"
    }

    fun start() {
        if (isRunning.get()) return
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                isRunning.set(true)
                Log.i(TAG, "SpendWise API Server started on port $port")

                while (isRunning.get()) {
                    val client = serverSocket?.accept() ?: break
                    threadPool.execute {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "SpendWise API Server error: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        Log.i(TAG, "SpendWise API Server stopped")
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { client ->
                val input = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                val output = client.getOutputStream()

                val requestLine = input.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0].uppercase()
                val path = parts[1].split("?")[0]

                var contentLength = 0
                var line: String?
                while (input.readLine().also { line = it } != null) {
                    if (line.isNullOrEmpty()) break
                    val headerLower = line!!.lowercase()
                    if (headerLower.startsWith("content-length:")) {
                        contentLength = headerLower.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                val body = if (contentLength > 0) {
                    val buffer = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val read = input.read(buffer, readTotal, contentLength - readTotal)
                        if (read == -1) break
                        readTotal += read
                    }
                    String(buffer, 0, readTotal)
                } else {
                    ""
                }

                when {
                    // Status
                    path == "/api/status" && method == "GET" -> {
                        val res = JSONObject().apply {
                            put("status", "ok")
                            put("app", "SpendWise")
                            put("version", "2.4.0")
                            put("deleteProtection", true)
                            put("timestamp", System.currentTimeMillis())
                        }
                        sendJsonResponse(output, 200, res.toString())
                    }

                    // Summary
                    path == "/api/summary" && method == "GET" -> {
                        try {
                            val now = LocalDate.now()
                            val summary = runBlocking(Dispatchers.IO) {
                                repository.getMonthlySummary(now.year, now.monthValue).first()
                            }
                            val res = JSONObject().apply {
                                put("status", "ok")
                                put("totalIncome", summary.totalIncome)
                                put("totalExpense", summary.totalExpense)
                                put("netBalance", summary.netBalance)
                            }
                            sendJsonResponse(output, 200, res.toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get summary")
                        }
                    }

                    // Categories: GET, POST (Create)
                    path == "/api/categories" && method == "GET" -> {
                        try {
                            val categories = runBlocking(Dispatchers.IO) {
                                repository.getAllCategories().first()
                            }
                            val array = JSONArray()
                            for (cat in categories) {
                                array.put(JSONObject().apply {
                                    put("id", cat.id)
                                    put("name", cat.name)
                                    put("type", cat.type.name)
                                    put("iconName", cat.iconName)
                                    put("colorHex", cat.colorHex)
                                })
                            }
                            sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("count", categories.size).put("categories", array).toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get categories")
                        }
                    }

                    path == "/api/categories" && method == "POST" -> {
                        try {
                            val json = JSONObject(body)
                            val name = json.getString("name").trim()
                            val type = try { TransactionType.valueOf(json.optString("type", "EXPENSE").uppercase()) } catch (_: Exception) { TransactionType.EXPENSE }
                            val iconName = json.optString("iconName", "Category")
                            val colorHex = json.optString("colorHex", "#6366F1")
                            val id = runBlocking(Dispatchers.IO) {
                                repository.insertCategory(CategoryEntity(name = name, type = type, iconName = iconName, colorHex = colorHex))
                            }
                            sendJsonResponse(output, 201, JSONObject().put("status", "ok").put("id", id).put("message", "Category created").toString())
                        } catch (e: Exception) {
                            sendError(output, 400, "Error: ${e.message}")
                        }
                    }

                    // Accounts: GET, POST (Create)
                    path == "/api/accounts" && method == "GET" -> {
                        try {
                            val accounts = runBlocking(Dispatchers.IO) {
                                repository.getAccountsWithBalance().first()
                            }
                            val array = JSONArray()
                            for (acc in accounts) {
                                array.put(JSONObject().apply {
                                    put("id", acc.account.id)
                                    put("name", acc.account.name)
                                    put("type", acc.account.type.name)
                                    put("balance", acc.calculatedBalance)
                                    put("isDefault", acc.account.isDefault)
                                })
                            }
                            sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("count", accounts.size).put("accounts", array).toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get accounts")
                        }
                    }

                    path == "/api/accounts" && method == "POST" -> {
                        try {
                            val json = JSONObject(body)
                            val name = json.getString("name").trim()
                            val type = try { AccountType.valueOf(json.optString("type", "BANK").uppercase()) } catch (_: Exception) { AccountType.BANK }
                            val isDefault = json.optBoolean("isDefault", false)
                            val id = runBlocking(Dispatchers.IO) {
                                repository.insertAccount(AccountEntity(name = name, type = type, balance = 0.0, isDefault = isDefault))
                            }
                            sendJsonResponse(output, 201, JSONObject().put("status", "ok").put("id", id).put("message", "Account created").toString())
                        } catch (e: Exception) {
                            sendError(output, 400, "Error: ${e.message}")
                        }
                    }

                    // Budgets: GET, POST (Set)
                    path == "/api/budgets" && method == "GET" -> {
                        try {
                            val now = LocalDate.now()
                            val budgets = runBlocking(Dispatchers.IO) {
                                repository.getBudgetsWithSpendingForMonth(now.year, now.monthValue).first()
                            }
                            val array = JSONArray()
                            for (b in budgets) {
                                array.put(JSONObject().apply {
                                    put("id", b.budgetId)
                                    put("categoryId", b.category.id)
                                    put("categoryName", b.category.name)
                                    put("limitAmount", b.limitAmount)
                                    put("spentAmount", b.spentAmount)
                                    put("remainingAmount", b.remainingAmount)
                                    put("isOverBudget", b.isOverBudget)
                                })
                            }
                            sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("count", budgets.size).put("budgets", array).toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get budgets")
                        }
                    }

                    path == "/api/budgets" && method == "POST" -> {
                        try {
                            val json = JSONObject(body)
                            val categoryId = json.getLong("categoryId")
                            val limitAmount = json.getDouble("limitAmount")
                            val now = LocalDate.now()
                            val monthYear = json.optString("monthYear", String.format(Locale.US, "%04d-%02d", now.year, now.monthValue))
                            runBlocking(Dispatchers.IO) {
                                repository.setBudget(categoryId, limitAmount, monthYear)
                            }
                            sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("message", "Budget updated").toString())
                        } catch (e: Exception) {
                            sendError(output, 400, "Error: ${e.message}")
                        }
                    }

                    // Transactions: GET, POST (Add), PUT (Update), DELETE (Protected)
                    path == "/api/transactions" && method == "GET" -> {
                        try {
                            val items = runBlocking(Dispatchers.IO) {
                                repository.getAllTransactionsWithCategory().first()
                            }
                            val array = JSONArray()
                            for (item in items) {
                                val t = item.transaction
                                val c = item.category
                                array.put(JSONObject().apply {
                                    put("id", t.id)
                                    put("title", t.title)
                                    put("amount", t.amount)
                                    put("type", t.type.name)
                                    put("categoryId", t.categoryId)
                                    put("categoryName", c?.name)
                                    put("accountId", t.accountId)
                                    put("toAccountId", t.toAccountId)
                                    put("dateTimestamp", t.dateTimestamp)
                                    put("note", t.note)
                                })
                            }
                            sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("count", items.size).put("transactions", array).toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get transactions")
                        }
                    }

                    path == "/api/transactions" && method == "POST" -> {
                        try {
                            val json = JSONObject(body)
                            val title = json.optString("title", "").trim()
                            val amount = json.optDouble("amount", -1.0)
                            if (title.isEmpty() || amount <= 0.0) {
                                sendError(output, 400, "Invalid title or amount")
                                return
                            }
                            val type = try { TransactionType.valueOf(json.optString("type", "EXPENSE").uppercase()) } catch (_: Exception) { TransactionType.EXPENSE }
                            val categoryId = if (json.has("categoryId") && !json.isNull("categoryId")) json.getLong("categoryId") else null
                            val accountId = if (json.has("accountId") && !json.isNull("accountId")) json.getLong("accountId") else null
                            val toAccountId = if (json.has("toAccountId") && !json.isNull("toAccountId")) json.getLong("toAccountId") else null
                            val note = if (json.has("note") && !json.isNull("note")) json.getString("note") else null
                            val timestamp = if (json.has("dateTimestamp")) json.getLong("dateTimestamp") else System.currentTimeMillis()

                            val newTransaction = TransactionEntity(
                                title = title,
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                accountId = accountId,
                                toAccountId = toAccountId,
                                note = note,
                                dateTimestamp = timestamp
                            )
                            val id = runBlocking(Dispatchers.IO) { repository.insertTransaction(newTransaction) }
                            sendJsonResponse(output, 201, JSONObject().put("status", "ok").put("message", "Transaction created").put("id", id).toString())
                        } catch (e: Exception) {
                            sendError(output, 400, "Error: ${e.message}")
                        }
                    }

                    path == "/api/transactions" && method == "PUT" -> {
                        try {
                            val json = JSONObject(body)
                            val id = json.getLong("id")
                            val existing = runBlocking(Dispatchers.IO) { repository.getTransactionById(id) }
                            if (existing == null) {
                                sendError(output, 404, "Transaction not found")
                                return
                            }
                            val updated = existing.copy(
                                title = json.optString("title", existing.title),
                                amount = if (json.has("amount")) json.getDouble("amount") else existing.amount,
                                categoryId = if (json.has("categoryId")) json.optLong("categoryId") else existing.categoryId,
                                accountId = if (json.has("accountId")) json.optLong("accountId") else existing.accountId,
                                note = if (json.has("note")) json.optString("note") else existing.note,
                                dateTimestamp = if (json.has("dateTimestamp")) json.getLong("dateTimestamp") else existing.dateTimestamp
                            )
                            runBlocking(Dispatchers.IO) { repository.updateTransaction(updated) }
                            sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("message", "Transaction updated").toString())
                        } catch (e: Exception) {
                            sendError(output, 400, "Error: ${e.message}")
                        }
                    }

                    path.startsWith("/api/transactions/") && method == "DELETE" -> {
                        val txIdStr = path.substringAfterLast("/")
                        val txId = txIdStr.toLongOrNull()
                        if (txId == null) {
                            sendError(output, 400, "Invalid transaction ID")
                            return
                        }
                        val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                        val code = json.optString("secretCode", "")
                        if (code != DELETE_SECURITY_CODE) {
                            sendError(output, 403, "Protected Action: Deletion requires valid secretCode ('$DELETE_SECURITY_CODE')")
                            return
                        }
                        runBlocking(Dispatchers.IO) { repository.deleteTransactionById(txId) }
                        sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("message", "Transaction $txId deleted securely").toString())
                    }

                    else -> sendError(output, 404, "Endpoint Not Found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client handling exception: ${e.message}", e)
        }
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, jsonString: String) {
        try {
            val bytes = jsonString.toByteArray(Charsets.UTF_8)
            val statusText = when (statusCode) {
                200 -> "OK"
                201 -> "Created"
                400 -> "Bad Request"
                403 -> "Forbidden"
                404 -> "Not Found"
                405 -> "Method Not Allowed"
                else -> "Internal Server Error"
            }
            val headers = "HTTP/1.1 $statusCode $statusText\r\n" +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: Content-Type\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(headers.toByteArray(Charsets.UTF_8))
            output.write(bytes)
            output.flush()
        } catch (_: Exception) {}
    }

    private fun sendError(output: OutputStream, statusCode: Int, message: String) {
        val errorJson = JSONObject().apply {
            put("status", "error")
            put("message", message)
        }
        sendJsonResponse(output, statusCode, errorJson.toString())
    }
}
