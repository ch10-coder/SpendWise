package com.spendwise.app.data.repository

import com.spendwise.app.data.local.dao.AccountDao
import com.spendwise.app.data.local.dao.BudgetDao
import com.spendwise.app.data.local.dao.CategoryDao
import com.spendwise.app.data.local.dao.RecurringTransactionDao
import com.spendwise.app.data.local.dao.TagDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.data.local.entity.BudgetEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.RecurringTransactionEntity
import com.spendwise.app.data.local.entity.TagEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.local.entity.TransactionTagCrossRef
import com.spendwise.app.data.preferences.UserPreferencesManager
import com.spendwise.app.data.service.CsvImportSummary
import com.spendwise.app.data.service.CsvService
import com.spendwise.app.data.service.RecurringTransactionService
import com.spendwise.app.model.AccountWithBalance
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

import com.spendwise.app.data.local.dao.SavingsGoalDao
import com.spendwise.app.data.local.entity.SavingsGoalEntity

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double
)

class SpendWiseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val accountDao: AccountDao,
    private val tagDao: TagDao,
    private val recurringTransactionDao: RecurringTransactionDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val preferencesManager: UserPreferencesManager,
    private val csvService: CsvService,
    private val recurringTransactionService: RecurringTransactionService
) {

    // Preferences - Preloaded initial values
    val initialThemeMode: ThemeMode get() = preferencesManager.initialThemeMode
    val initialThemePalette: com.spendwise.app.model.ThemePalette get() = preferencesManager.initialThemePalette
    val initialIsDynamicColor: Boolean get() = preferencesManager.initialIsDynamicColor
    val initialIsAmoledBlack: Boolean get() = preferencesManager.initialIsAmoledBlack
    val initialCurrencySymbol: String get() = preferencesManager.initialCurrencySymbol
    val initialIsAppLockEnabled: Boolean get() = preferencesManager.initialIsAppLockEnabled

    // Preferences Flows
    val currencyCode: Flow<String> = preferencesManager.currencyCodeFlow
    val currencySymbol: Flow<String> = preferencesManager.currencySymbolFlow
    val themeMode: Flow<ThemeMode> = preferencesManager.themeModeFlow
    val themePalette: Flow<com.spendwise.app.model.ThemePalette> = preferencesManager.themePaletteFlow
    val isDynamicColor: Flow<Boolean> = preferencesManager.isDynamicColorFlow
    val isBalanceHidden: Flow<Boolean> = preferencesManager.isBalanceHiddenFlow
    val isAppLockEnabled: Flow<Boolean> = preferencesManager.isAppLockEnabledFlow
    val isAmoledBlack: Flow<Boolean> = preferencesManager.isAmoledBlackFlow
    val dailyReminderTime: Flow<String?> = preferencesManager.dailyReminderTimeFlow

    suspend fun setCurrency(currencyOption: CurrencyOption) = preferencesManager.setCurrency(currencyOption)
    suspend fun setThemeMode(mode: ThemeMode) = preferencesManager.setThemeMode(mode)
    suspend fun setThemePalette(palette: com.spendwise.app.model.ThemePalette) = preferencesManager.setThemePalette(palette)
    suspend fun setDynamicColor(enabled: Boolean) = preferencesManager.setDynamicColor(enabled)
    suspend fun setBalanceHidden(hidden: Boolean) = preferencesManager.setBalanceHidden(hidden)
    suspend fun setAppLockEnabled(enabled: Boolean) = preferencesManager.setAppLockEnabled(enabled)
    suspend fun setAmoledBlack(enabled: Boolean) = preferencesManager.setAmoledBlack(enabled)
    suspend fun setDailyReminderTime(time: String?) = preferencesManager.setDailyReminderTime(time)

    // Transactions
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>> {
        return transactionDao.getAllTransactionsWithCategory().map { list ->
            list.map { it.toTransactionWithCategory() }
        }
    }

    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<TransactionWithCategory>> {
        val (start, end) = getMonthTimestamps(year, month)
        return getTransactionsInRange(start, end)
    }

    fun getTransactionsInRange(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithCategory>> {
        return transactionDao.getTransactionsBetweenDates(startTimestamp, endTimestamp).map { list ->
            list.map { it.toTransactionWithCategory() }
        }
    }

    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionWithCategory>> {
        return transactionDao.getTransactionsForAccount(accountId).map { list ->
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

    /** Deletes a transaction and returns its tag ids, so an undo can bring it back exactly as it was. */
    suspend fun deleteTransactionKeepingTags(transaction: TransactionEntity): List<Long> {
        val tagIds = getTagIdsForTransaction(transaction.id)
        transactionDao.deleteTransaction(transaction)
        return tagIds
    }

    /** Re-inserts a deleted transaction with its original id, recurring link and tags. */
    suspend fun restoreTransaction(transaction: TransactionEntity, tagIds: List<Long>) {
        transactionDao.insertTransaction(transaction)
        val existingTagIds = tagDao.getAllTags().first().map { it.id }.toSet()
        setTagsForTransaction(transaction.id, tagIds.filter { it in existingTagIds })
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    // Accounts
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    /** Accounts with their live balance: opening balance + income - expenses - transfers out + transfers in. */
    fun getAccountsWithBalance(): Flow<List<AccountWithBalance>> {
        return accountDao.getAccountsWithBalance().map { list ->
            list.map { AccountWithBalance(it.account, it.calculatedBalance) }
        }
    }

    suspend fun insertAccount(account: AccountEntity): Long {
        val id = accountDao.insertAccount(account)
        if (account.isDefault) accountDao.clearDefaultExcept(id)
        return id
    }
    suspend fun updateAccount(account: AccountEntity) {
        if (account.isDefault) accountDao.clearDefaultExcept(account.id)
        accountDao.updateAccount(account)
    }
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)
    suspend fun getDefaultAccount(): AccountEntity? = accountDao.getDefaultAccount()

    // Tags
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()
    
    suspend fun insertTag(tag: TagEntity): Long = tagDao.insertTag(tag)
    suspend fun updateTag(tag: TagEntity) = tagDao.updateTag(tag)
    suspend fun deleteTag(tag: TagEntity) = tagDao.deleteTag(tag)

    fun getTagsForTransaction(transactionId: Long): Flow<List<TagEntity>> = tagDao.getTagsForTransaction(transactionId)

    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long> =
        tagDao.getTagsForTransactionList(transactionId).map { it.id }
    
    suspend fun setTagsForTransaction(transactionId: Long, tagIds: List<Long>) {
        tagDao.deleteAllTagsForTransaction(transactionId)
        val refs = tagIds.map { TransactionTagCrossRef(transactionId, it) }
        tagDao.insertTransactionTags(refs)
    }

    // Recurring Transactions
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>> = recurringTransactionDao.getAllRecurringTransactions()
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>> = recurringTransactionDao.getActiveRecurringTransactions()
    
    suspend fun addRecurringTransaction(rule: RecurringTransactionEntity): Long = recurringTransactionDao.insertRecurringTransaction(rule)
    /**
     * Saves an edited rule. Its schedule position is kept unless the start date or frequency changed;
     * then it restarts from the first occurrence that is not in the past, so editing a rule never
     * re-creates transactions that were already generated.
     */
    suspend fun updateRecurringTransaction(rule: RecurringTransactionEntity) {
        val previous = recurringTransactionDao.getRecurringTransactionById(rule.id)
        val now = System.currentTimeMillis()
        val scheduleChanged = previous == null ||
            previous.startDate != rule.startDate ||
            previous.frequency != rule.frequency
        val resumed = previous != null && !previous.isActive && rule.isActive
        val nextOccurrence = if (scheduleChanged || (resumed && previous!!.nextOccurrence < now)) {
            recurringTransactionService.firstOccurrenceFrom(rule, now)
        } else {
            previous!!.nextOccurrence
        }
        recurringTransactionDao.updateRecurringTransaction(rule.copy(nextOccurrence = nextOccurrence))
    }
    suspend fun deleteRecurringTransaction(rule: RecurringTransactionEntity) = recurringTransactionDao.deleteRecurringTransaction(rule)
    
    suspend fun toggleRecurring(id: Long, isActive: Boolean) {
        if (isActive) {
            val rule = recurringTransactionDao.getRecurringTransactionById(id)
            val now = System.currentTimeMillis()
            if (rule != null && rule.nextOccurrence < now) {
                // Resuming skips the occurrences that fell inside the pause instead of back-filling them
                recurringTransactionDao.updateNextOccurrence(id, recurringTransactionService.firstOccurrenceFrom(rule, now))
            }
        }
        recurringTransactionDao.setActive(id, isActive)
    }

    suspend fun processRecurringTransactions() {
        recurringTransactionService.processRecurringTransactions()
    }

    // Savings Goals
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long = savingsGoalDao.insertGoal(goal)
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = savingsGoalDao.updateGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = savingsGoalDao.deleteGoal(goal)
    suspend fun deleteSavingsGoalById(id: Long) = savingsGoalDao.deleteGoalById(id)
    suspend fun depositToSavingsGoal(id: Long, amount: Double) = savingsGoalDao.deposit(id, amount)
    suspend fun withdrawFromSavingsGoal(id: Long, amount: Double) = savingsGoalDao.withdraw(id, amount)

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Budgets
    fun getBudgetsWithSpendingForMonth(year: Int, month: Int): Flow<List<BudgetWithSpending>> {
        val monthYear = String.format(Locale.US, "%04d-%02d", year, month)
        val (start, end) = getMonthTimestamps(year, month)

        val budgetsFlow = budgetDao.getBudgetsForMonth(monthYear)
        val transactionsFlow = transactionDao.getTransactionsBetweenDates(start, end)

        return combine(budgetsFlow, transactionsFlow) { budgets, transactionsList ->
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

    suspend fun getBudgetsFromPreviousMonth(year: Int, month: Int) {
        val currentMonthYear = String.format(Locale.US, "%04d-%02d", year, month)
        val prevDate = LocalDate.of(year, month, 1).minusMonths(1)
        val prevMonthYear = String.format(Locale.US, "%04d-%02d", prevDate.year, prevDate.monthValue)
        
        val prevBudgets = budgetDao.getBudgetsForMonth(prevMonthYear).first()
        for (budgetTuple in prevBudgets) {
            setBudget(budgetTuple.categoryId, budgetTuple.limitAmount, currentMonthYear)
        }
    }

    // Analytics
    fun getMonthlyAnalytics(year: Int, month: Int): Flow<MonthlyAnalytics> {
        val monthYear = String.format(Locale.US, "%04d-%02d", year, month)
        val (start, end) = getMonthTimestamps(year, month)

        val transactionsFlow = transactionDao.getTransactionsBetweenDates(start, end)

        return transactionsFlow.map { tuples ->
            var totalIncome = 0.0
            var totalExpense = 0.0
            val expenseByCategory = mutableMapOf<CategoryEntity, Double>()
            val dailyMap = mutableMapOf<Int, Pair<Double, Double>>() // Day -> (Expense, Income)

            val zoneId = ZoneId.systemDefault()

            for (tuple in tuples) {
                val item = tuple.toTransactionWithCategory()
                val t = item.transaction
                val date = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(t.dateTimestamp), zoneId)
                val day = date.dayOfMonth
                val currentDaily = dailyMap[day] ?: Pair(0.0, 0.0)

                when (t.type) {
                    TransactionType.INCOME -> {
                        totalIncome += t.amount
                        dailyMap[day] = Pair(currentDaily.first, currentDaily.second + t.amount)
                    }
                    TransactionType.EXPENSE -> {
                        totalExpense += t.amount
                        dailyMap[day] = Pair(currentDaily.first + t.amount, currentDaily.second)
                        if (item.category != null) {
                            val currentCatAmount = expenseByCategory[item.category] ?: 0.0
                            expenseByCategory[item.category] = currentCatAmount + t.amount
                        }
                    }
                    // Moving money between your own accounts is neither income nor spending
                    TransactionType.TRANSFER -> Unit
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

            val maxDays = LocalDate.of(year, month, 1).lengthOfMonth()

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

    suspend fun importTransactionsFromCsv(inputStream: InputStream): Result<CsvImportSummary> =
        csvService.importTransactionsFromCsv(inputStream)

    // Helper: compute month timestamps using java.time
    private fun getMonthTimestamps(year: Int, month: Int): Pair<Long, Long> {
        val startOfMonth = LocalDate.of(year, month, 1).atStartOfDay(ZoneId.systemDefault())
        val endOfMonth = startOfMonth.plusMonths(1).minusNanos(1)

        val start = startOfMonth.toInstant().toEpochMilli()
        val end = endOfMonth.toInstant().toEpochMilli()

        return Pair(start, end)
    }
}
