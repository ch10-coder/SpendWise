package com.spendwise.app.data.repository

import com.spendwise.app.data.local.dao.BudgetDao
import com.spendwise.app.data.local.dao.CategoryDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.BudgetEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.preferences.UserPreferencesManager
import com.spendwise.app.data.service.CsvService
import com.spendwise.app.model.BudgetWithSpending
import com.spendwise.app.model.CategorySpending
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.DailySpending
import com.spendwise.app.model.MonthlyAnalytics
import com.spendwise.app.model.ThemeMode
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double
)

class SpendWiseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val preferencesManager: UserPreferencesManager,
    private val csvService: CsvService
) {

    // Preferences
    val currencyCode: Flow<String> = preferencesManager.currencyCodeFlow
    val currencySymbol: Flow<String> = preferencesManager.currencySymbolFlow
    val themeMode: Flow<ThemeMode> = preferencesManager.themeModeFlow

    suspend fun setCurrency(currencyOption: CurrencyOption) = preferencesManager.setCurrency(currencyOption)
    suspend fun setThemeMode(mode: ThemeMode) = preferencesManager.setThemeMode(mode)

    // Transactions
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>> {
        return transactionDao.getAllTransactionsWithCategory().map { list ->
            list.map { it.toTransactionWithCategory() }
        }
    }

    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<TransactionWithCategory>> {
        val (start, end) = getMonthTimestamps(year, month)
        return transactionDao.getTransactionsBetweenDates(start, end).map { list ->
            list.map { it.toTransactionWithCategory() }
        }
    }

    fun getMonthlySummary(year: Int, month: Int): Flow<MonthlySummary> {
        val (start, end) = getMonthTimestamps(year, month)
        val incomeFlow = transactionDao.getTotalAmountByTypeBetweenDates(TransactionType.INCOME, start, end)
        val expenseFlow = transactionDao.getTotalAmountByTypeBetweenDates(TransactionType.EXPENSE, start, end)

        return combine(incomeFlow, expenseFlow) { income, expense ->
            val inc = income ?: 0.0
            val exp = expense ?: 0.0
            MonthlySummary(
                totalIncome = inc,
                totalExpense = exp,
                netBalance = inc - exp
            )
        }
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Budgets
    fun getBudgetsWithSpendingForMonth(year: Int, month: Int): Flow<List<BudgetWithSpending>> {
        val monthYear = String.format("%04d-%02d", year, month)
        val (start, end) = getMonthTimestamps(year, month)

        val budgetsFlow = budgetDao.getBudgetsForMonth(monthYear)
        val transactionsFlow = transactionDao.getTransactionsBetweenDates(start, end)

        return combine(budgetsFlow, transactionsFlow) { budgets, transactionsList ->
            // Map categoryId -> total expense spent
            val spendingMap = mutableMapOf<Long, Double>()
            for (t in transactionsList) {
                if (t.type == TransactionType.EXPENSE && t.categoryId != null) {
                    val current = spendingMap[t.categoryId] ?: 0.0
                    spendingMap[t.categoryId] = current + t.amount
                }
            }

            budgets.map { tuple ->
                val spent = spendingMap[tuple.categoryId] ?: 0.0
                BudgetWithSpending(
                    budgetId = tuple.id,
                    category = tuple.toCategory(),
                    limitAmount = tuple.limitAmount,
                    spentAmount = spent,
                    monthYear = tuple.monthYear
                )
            }
        }
    }

    suspend fun setBudget(categoryId: Long, limitAmount: Double, monthYear: String) {
        val existing = budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear)
        if (existing != null) {
            budgetDao.updateBudget(existing.copy(limitAmount = limitAmount))
        } else {
            budgetDao.insertBudget(
                BudgetEntity(
                    categoryId = categoryId,
                    limitAmount = limitAmount,
                    monthYear = monthYear
                )
            )
        }
    }

    suspend fun deleteBudget(id: Long) = budgetDao.deleteBudgetById(id)

    // Analytics
    fun getMonthlyAnalytics(year: Int, month: Int): Flow<MonthlyAnalytics> {
        val monthYear = String.format("%04d-%02d", year, month)
        val (start, end) = getMonthTimestamps(year, month)

        val transactionsFlow = transactionDao.getTransactionsBetweenDates(start, end)

        return transactionsFlow.map { tuples ->
            var totalIncome = 0.0
            var totalExpense = 0.0
            val expenseByCategory = mutableMapOf<CategoryEntity, Double>()
            val dailyMap = mutableMapOf<Int, Pair<Double, Double>>() // Day -> (Expense, Income)

            val cal = Calendar.getInstance()

            for (tuple in tuples) {
                val item = tuple.toTransactionWithCategory()
                val t = item.transaction
                cal.timeInMillis = t.dateTimestamp
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val currentDaily = dailyMap[day] ?: Pair(0.0, 0.0)

                if (t.type == TransactionType.INCOME) {
                    totalIncome += t.amount
                    dailyMap[day] = Pair(currentDaily.first, currentDaily.second + t.amount)
                } else {
                    totalExpense += t.amount
                    dailyMap[day] = Pair(currentDaily.first + t.amount, currentDaily.second)
                    if (item.category != null) {
                        val currentCatAmount = expenseByCategory[item.category] ?: 0.0
                        expenseByCategory[item.category] = currentCatAmount + t.amount
                    }
                }
            }

            val categorySpendings = expenseByCategory.map { (cat, amount) ->
                val pct = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
                CategorySpending(
                    category = cat,
                    totalAmount = amount,
                    percentage = pct
                )
            }.sortedByDescending { it.totalAmount }

            // Max days in this month
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val dailySpendings = (1..maxDays).map { d ->
                val pair = dailyMap[d] ?: Pair(0.0, 0.0)
                DailySpending(
                    dayOfMonth = d,
                    totalExpense = pair.first,
                    totalIncome = pair.second
                )
            }

            MonthlyAnalytics(
                monthYear = monthYear,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netSavings = totalIncome - totalExpense,
                categorySpendings = categorySpendings,
                dailySpendings = dailySpendings
            )
        }
    }

    // CSV Tools
    suspend fun exportTransactionsToCsv(outputStream: OutputStream): Result<Int> =
        csvService.exportTransactionsToCsv(outputStream)

    suspend fun importTransactionsFromCsv(inputStream: InputStream): Result<Int> =
        csvService.importTransactionsFromCsv(inputStream)

    // Helper: compute month timestamps
    private fun getMonthTimestamps(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}
