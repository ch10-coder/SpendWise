package com.spendwise.app.server

import android.util.Log
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.repository.SpendWiseRepository
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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight Zero-Dependency Embedded HTTP API server running on localhost:8080.
 * Synchronous client handling with runBlocking for Room queries to prevent premature socket close.
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

                // Read HTTP Headers
                var contentLength = 0
                var line: String?
                while (input.readLine().also { line = it } != null) {
                    if (line.isNullOrEmpty()) break
                    val headerLower = line!!.lowercase()
                    if (headerLower.startsWith("content-length:")) {
                        contentLength = headerLower.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                // Read body if POST
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

                // Route handling synchronously so socket stays open until response is sent
                when {
                    path == "/api/status" && method == "GET" -> {
                        val res = JSONObject().apply {
                            put("status", "ok")
                            put("app", "SpendWise")
                            put("version", "2.4.0")
                            put("timestamp", System.currentTimeMillis())
                        }
                        sendJsonResponse(output, 200, res.toString())
                    }

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
                            val res = JSONObject().apply {
                                put("status", "ok")
                                put("count", categories.size)
                                put("categories", array)
                            }
                            sendJsonResponse(output, 200, res.toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get categories")
                        }
                    }

                    path == "/api/accounts" && method == "GET" -> {
                        try {
                            val accountsWithBalance = runBlocking(Dispatchers.IO) {
                                repository.getAccountsWithBalance().first()
                            }
                            val array = JSONArray()
                            for (acc in accountsWithBalance) {
                                array.put(JSONObject().apply {
                                    put("id", acc.account.id)
                                    put("name", acc.account.name)
                                    put("type", acc.account.type.name)
                                    put("balance", acc.calculatedBalance)
                                    put("isDefault", acc.account.isDefault)
                                })
                            }
                            val res = JSONObject().apply {
                                put("status", "ok")
                                put("count", accountsWithBalance.size)
                                put("accounts", array)
                            }
                            sendJsonResponse(output, 200, res.toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get accounts")
                        }
                    }

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
                            val res = JSONObject().apply {
                                put("status", "ok")
                                put("count", items.size)
                                put("transactions", array)
                            }
                            sendJsonResponse(output, 200, res.toString())
                        } catch (e: Exception) {
                            sendError(output, 500, e.message ?: "Failed to get transactions")
                        }
                    }

                    path == "/api/transactions" && method == "POST" -> {
                        try {
                            val json = JSONObject(body)
                            val title = json.optString("title", "").trim()
                            if (title.isEmpty()) {
                                sendError(output, 400, "Field 'title' is required")
                                return
                            }

                            val amount = json.optDouble("amount", -1.0)
                            if (amount <= 0.0) {
                                sendError(output, 400, "Field 'amount' must be greater than 0")
                                return
                            }

                            val typeStr = json.optString("type", "EXPENSE").uppercase()
                            val type = try {
                                TransactionType.valueOf(typeStr)
                            } catch (e: Exception) {
                                TransactionType.EXPENSE
                            }

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

                            val id = runBlocking(Dispatchers.IO) {
                                repository.insertTransaction(newTransaction)
                            }

                            val res = JSONObject().apply {
                                put("status", "ok")
                                put("message", "Transaction created successfully")
                                put("id", id)
                            }
                            sendJsonResponse(output, 201, res.toString())
                        } catch (e: Exception) {
                            sendError(output, 400, "Invalid JSON payload: ${e.message}")
                        }
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
                404 -> "Not Found"
                405 -> "Method Not Allowed"
                else -> "Internal Server Error"
            }
            val headers = "HTTP/1.1 $statusCode $statusText\r\n" +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
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
