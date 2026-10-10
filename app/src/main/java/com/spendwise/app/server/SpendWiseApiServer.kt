package com.spendwise.app.server

import android.util.Log
import com.spendwise.app.data.local.dao.TransactionWithCategoryTuple
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.TransactionType
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * Embedded HTTP API server running on localhost:8080.
 * Allows AI agents and local tools to query summary, transactions, categories,
 * accounts, and insert new transactions directly into SpendWise.
 */
class SpendWiseApiServer(
    private val repository: SpendWiseRepository,
    private val scope: CoroutineScope,
    private val port: Int = 8080
) {
    private var server: HttpServer? = null

    companion object {
        private const val TAG = "SpendWiseApiServer"
    }

    fun start() {
        if (server != null) return
        try {
            val s = HttpServer.create(InetSocketAddress(port), 0)
            s.executor = Executors.newFixedThreadPool(4)

            // Health & Status
            s.createContext("/api/status", StatusHandler())

            // Transactions: GET (list/filter), POST (create)
            s.createContext("/api/transactions", TransactionsHandler())

            // Monthly Summary
            s.createContext("/api/summary", SummaryHandler())

            // Categories
            s.createContext("/api/categories", CategoriesHandler())

            // Accounts
            s.createContext("/api/accounts", AccountsHandler())

            s.start()
            server = s
            Log.i(TAG, "SpendWise API Server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpendWise API Server: ${e.message}", e)
        }
    }

    fun stop() {
        server?.stop(1)
        server = null
        Log.i(TAG, "SpendWise API Server stopped")
    }

    private fun sendJsonResponse(exchange: HttpExchange, statusCode: Int, jsonString: String) {
        val bytes = jsonString.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun sendError(exchange: HttpExchange, statusCode: Int, message: String) {
        val errorJson = JSONObject().apply {
            put("status", "error")
            put("message", message)
        }
        sendJsonResponse(exchange, statusCode, errorJson.toString())
    }

    private inner class StatusHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (exchange.requestMethod.equals("GET", ignoreCase = true)) {
                val res = JSONObject().apply {
                    put("status", "ok")
                    put("app", "SpendWise")
                    put("version", "2.4.0")
                    put("timestamp", System.currentTimeMillis())
                }
                sendJsonResponse(exchange, 200, res.toString())
            } else {
                sendError(exchange, 405, "Method Not Allowed")
            }
        }
    }

    private inner class SummaryHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (!exchange.requestMethod.equals("GET", ignoreCase = true)) {
                sendError(exchange, 405, "Method Not Allowed")
                return
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val summary = repository.getCurrentMonthSummary().first()
                    val res = JSONObject().apply {
                        put("status", "ok")
                        put("totalIncome", summary.totalIncome)
                        put("totalExpense", summary.totalExpense)
                        put("netBalance", summary.netBalance)
                    }
                    sendJsonResponse(exchange, 200, res.toString())
                } catch (e: Exception) {
                    sendError(exchange, 500, e.message ?: "Internal error")
                }
            }
        }
    }

    private inner class CategoriesHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (!exchange.requestMethod.equals("GET", ignoreCase = true)) {
                sendError(exchange, 405, "Method Not Allowed")
                return
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val categories = repository.getAllCategories().first()
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
                    sendJsonResponse(exchange, 200, res.toString())
                } catch (e: Exception) {
                    sendError(exchange, 500, e.message ?: "Internal error")
                }
            }
        }
    }

    private inner class AccountsHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (!exchange.requestMethod.equals("GET", ignoreCase = true)) {
                sendError(exchange, 405, "Method Not Allowed")
                return
            }

            scope.launch(Dispatchers.IO) {
                try {
                    val accountsWithBalance = repository.getAllAccountsWithBalance().first()
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
                    sendJsonResponse(exchange, 200, res.toString())
                } catch (e: Exception) {
                    sendError(exchange, 500, e.message ?: "Internal error")
                }
            }
        }
    }

    private inner class TransactionsHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            when (exchange.requestMethod.uppercase()) {
                "GET" -> handleGetTransactions(exchange)
                "POST" -> handlePostTransaction(exchange)
                else -> sendError(exchange, 405, "Method Not Allowed")
            }
        }

        private fun handleGetTransactions(exchange: HttpExchange) {
            scope.launch(Dispatchers.IO) {
                try {
                    val items = repository.getAllTransactionsWithCategory().first()
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
                    sendJsonResponse(exchange, 200, res.toString())
                } catch (e: Exception) {
                    sendError(exchange, 500, e.message ?: "Internal error")
                }
            }
        }

        private fun handlePostTransaction(exchange: HttpExchange) {
            scope.launch(Dispatchers.IO) {
                try {
                    val body = BufferedReader(InputStreamReader(exchange.requestBody, Charsets.UTF_8)).use { it.readText() }
                    val json = JSONObject(body)

                    val title = json.optString("title", "").trim()
                    if (title.isEmpty()) {
                        sendError(exchange, 400, "Field 'title' is required")
                        return@launch
                    }

                    val amount = json.optDouble("amount", -1.0)
                    if (amount <= 0.0) {
                        sendError(exchange, 400, "Field 'amount' must be greater than 0")
                        return@launch
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

                    val id = repository.insertTransaction(newTransaction)

                    val res = JSONObject().apply {
                        put("status", "ok")
                        put("message", "Transaction created successfully")
                        put("id", id)
                    }
                    sendJsonResponse(exchange, 201, res.toString())
                } catch (e: Exception) {
                    sendError(exchange, 400, "Invalid JSON or payload: ${e.message}")
                }
            }
        }
    }
}
