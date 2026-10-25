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
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Embedded HTTP API server on 127.0.0.1:8080 for on-device AI / automation tools (e.g. Termux).
 * Features:
 * - Full App Access: Transactions, Accounts, Budgets, Categories, Tags, Summary.
 * - Secure Delete Protection: Requires the confirmation code in 'secretCode'.
 * - Privacy: listens on the loopback interface only and refuses browser requests, so neither
 *   other devices on the Wi-Fi nor websites open in the phone's browser can read your data.
 */
class SpendWiseApiServer(
    private val repository: SpendWiseRepository,
    private val scope: CoroutineScope,
    private val port: Int = 8080,
    private val appVersion: String = ""
) {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val threadPool = Executors.newFixedThreadPool(4)

    companion object {
        private const val TAG = "SpendWiseApiServer"
        private const val DELETE_SECURITY_CODE = "CONFIRM_DELETE"
        private const val SOCKET_TIMEOUT_MS = 10_000
        private const val MAX_HEADER_BYTES = 16 * 1024
        private const val MAX_BODY_BYTES = 1024 * 1024
        private val ALLOWED_HOSTS = setOf("127.0.0.1", "localhost", "[::1]")
    }

    private class HttpRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: String
    )

    fun start() {
        if (isRunning.get()) return
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port, 50, InetAddress.getLoopbackAddress())
                isRunning.set(true)
                Log.i(TAG, "SpendWise API Server started on 127.0.0.1:$port")

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
                client.soTimeout = SOCKET_TIMEOUT_MS
                val output = client.getOutputStream()
                val request = readRequest(BufferedInputStream(client.getInputStream()))
                if (request == null) {
                    sendError(output, 400, "Malformed request")
                    return
                }

                // Websites can make the phone's browser call localhost. Browsers always send an Origin
                // header on such requests, and a rebound DNS name shows up in Host, so refuse both.
                val host = request.headers["host"]?.let { hostWithoutPort(it) }
                if (request.headers.containsKey("origin") || (host != null && host !in ALLOWED_HOSTS)) {
                    sendError(output, 403, "Browser and remote requests are not allowed")
                    return
                }

                route(request, output)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client handling exception: ${e.message}", e)
        }
    }

    /** Reads the request line, headers and a body of exactly Content-Length bytes (decoded as UTF-8). */
    private fun readRequest(input: InputStream): HttpRequest? {
        // Headers end at the first blank line ("\r\n\r\n", or "\n\n" from lenient clients)
        val headerBytes = ByteArrayOutputStream()
        var lastFour = 0
        while (true) {
            val b = input.read()
            if (b == -1) return null
            headerBytes.write(b)
            lastFour = (lastFour shl 8) or b
            if (lastFour == 0x0D0A0D0A || (lastFour and 0xFFFF) == 0x0A0A) break
            if (headerBytes.size() > MAX_HEADER_BYTES) return null
        }

        val lines = headerBytes.toString(Charsets.ISO_8859_1.name()).split("\n").map { it.trimEnd('\r') }
        val requestLine = lines.firstOrNull()?.split(" ") ?: return null
        if (requestLine.size < 2) return null

        val headers = lines.drop(1)
            .filter { ':' in it }
            .associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }

        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        if (contentLength < 0 || contentLength > MAX_BODY_BYTES) return null
        val body = ByteArray(contentLength)
        var readTotal = 0
        while (readTotal < contentLength) {
            val read = input.read(body, readTotal, contentLength - readTotal)
            if (read == -1) break
            readTotal += read
        }

        return HttpRequest(
            method = requestLine[0].uppercase(),
            path = requestLine[1].substringBefore('?'),
            headers = headers,
            body = String(body, 0, readTotal, Charsets.UTF_8)
        )
    }

    private fun hostWithoutPort(host: String): String =
        if (host.startsWith("[")) host.substringBefore("]") + "]" else host.substringBefore(':')

    private fun route(request: HttpRequest, output: OutputStream) {
        val method = request.method
        val path = request.path
        val body = request.body

        when {
            // Status
            path == "/api/status" && method == "GET" -> {
                val res = JSONObject().apply {
                    put("status", "ok")
                    put("app", "SpendWise")
                    put("version", appVersion)
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

            // Tags: GET
            path == "/api/tags" && method == "GET" -> {
                try {
                    val tags = runBlocking(Dispatchers.IO) { repository.getAllTags().first() }
                    val array = JSONArray()
                    for (tag in tags) {
                        array.put(JSONObject().put("id", tag.id).put("name", tag.name).put("colorHex", tag.colorHex))
                    }
                    sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("count", tags.size).put("tags", array).toString())
                } catch (e: Exception) {
                    sendError(output, 500, e.message ?: "Failed to get tags")
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
                            put("openingBalance", acc.account.balance)
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
                    val openingBalance = json.optDouble("openingBalance", 0.0).takeUnless { it.isNaN() } ?: 0.0
                    val id = runBlocking(Dispatchers.IO) {
                        repository.insertAccount(AccountEntity(name = name, type = type, balance = openingBalance, isDefault = isDefault))
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
                    if (limitAmount <= 0.0) {
                        sendError(output, 400, "limitAmount must be greater than 0")
                        return
                    }
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
                            put("categoryId", t.categoryId ?: JSONObject.NULL)
                            put("categoryName", c?.name ?: JSONObject.NULL)
                            put("accountId", t.accountId ?: JSONObject.NULL)
                            put("toAccountId", t.toAccountId ?: JSONObject.NULL)
                            put("dateTimestamp", t.dateTimestamp)
                            put("note", t.note ?: JSONObject.NULL)
                            put("recurringRuleId", t.recurringRuleId ?: JSONObject.NULL)
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
                    if (title.isEmpty() || amount.isNaN() || amount <= 0.0) {
                        sendError(output, 400, "Invalid title or amount")
                        return
                    }
                    val type = try { TransactionType.valueOf(json.optString("type", "EXPENSE").uppercase()) } catch (_: Exception) { TransactionType.EXPENSE }
                    val accountId = json.longOrNull("accountId")
                    val toAccountId = json.longOrNull("toAccountId")
                    if (type == TransactionType.TRANSFER && (accountId == null || toAccountId == null || accountId == toAccountId)) {
                        sendError(output, 400, "A TRANSFER needs different accountId and toAccountId")
                        return
                    }

                    val newTransaction = TransactionEntity(
                        title = title,
                        amount = amount,
                        type = type,
                        categoryId = if (type == TransactionType.TRANSFER) null else json.longOrNull("categoryId"),
                        accountId = accountId,
                        toAccountId = if (type == TransactionType.TRANSFER) toAccountId else null,
                        note = json.stringOrNull("note"),
                        dateTimestamp = json.longOrNull("dateTimestamp") ?: System.currentTimeMillis()
                    )
                    val tagIds = json.longList("tagIds")
                    val id = runBlocking(Dispatchers.IO) {
                        val insertedId = repository.insertTransaction(newTransaction)
                        if (tagIds != null) repository.setTagsForTransaction(insertedId, tagIds)
                        insertedId
                    }
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
                    val amount = if (json.has("amount")) json.getDouble("amount") else existing.amount
                    if (amount <= 0.0) {
                        sendError(output, 400, "amount must be greater than 0")
                        return
                    }
                    val type = if (json.has("type")) {
                        try { TransactionType.valueOf(json.getString("type").uppercase()) } catch (_: Exception) { existing.type }
                    } else existing.type

                    // A key that is present but null clears the field; a missing key leaves it unchanged.
                    val updated = existing.copy(
                        title = json.stringOrNull("title")?.trim()?.takeIf { it.isNotEmpty() } ?: existing.title,
                        amount = amount,
                        type = type,
                        categoryId = if (json.has("categoryId")) json.longOrNull("categoryId") else existing.categoryId,
                        accountId = if (json.has("accountId")) json.longOrNull("accountId") else existing.accountId,
                        toAccountId = if (json.has("toAccountId")) json.longOrNull("toAccountId") else existing.toAccountId,
                        note = if (json.has("note")) json.stringOrNull("note") else existing.note,
                        dateTimestamp = json.longOrNull("dateTimestamp") ?: existing.dateTimestamp
                    )
                    val tagIds = json.longList("tagIds")
                    runBlocking(Dispatchers.IO) {
                        repository.updateTransaction(updated)
                        if (tagIds != null) repository.setTagsForTransaction(id, tagIds)
                    }
                    sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("message", "Transaction updated").toString())
                } catch (e: Exception) {
                    sendError(output, 400, "Error: ${e.message}")
                }
            }

            path.startsWith("/api/transactions/") && method == "DELETE" -> {
                val txId = path.substringAfterLast("/").toLongOrNull()
                if (txId == null) {
                    sendError(output, 400, "Invalid transaction ID")
                    return
                }
                val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                if (json.optString("secretCode", "") != DELETE_SECURITY_CODE) {
                    // The code is deliberately not echoed back: it is the user's explicit confirmation.
                    sendError(output, 403, "Protected action: deleting needs the confirmation code from the user in 'secretCode'")
                    return
                }
                runBlocking(Dispatchers.IO) { repository.deleteTransactionById(txId) }
                sendJsonResponse(output, 200, JSONObject().put("status", "ok").put("message", "Transaction $txId deleted securely").toString())
            }

            else -> sendError(output, 404, "Endpoint Not Found")
        }
    }

    private fun JSONObject.longOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.stringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    /** Reads an array of ids; null when the key is absent, so callers can leave tags untouched. */
    private fun JSONObject.longList(key: String): List<Long>? {
        if (!has(key) || isNull(key)) return null
        val array = getJSONArray(key)
        return (0 until array.length()).map { array.getLong(it) }
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
