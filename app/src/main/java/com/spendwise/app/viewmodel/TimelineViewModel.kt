package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.repository.MonthlySummary
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.DateRangeFilter
import com.spendwise.app.model.TransactionDraft
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import com.spendwise.app.util.IndianCurrencyFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    val currencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<com.spendwise.app.data.local.entity.AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<com.spendwise.app.data.local.entity.TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isBalanceHidden: StateFlow<Boolean> = repository.isBalanceHidden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedDateRange = MutableStateFlow(DateRangeFilter.THIS_MONTH)
    val customStartDate = MutableStateFlow<Long?>(null)
    val customEndDate = MutableStateFlow<Long?>(null)
    
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedCategoryFilter = MutableStateFlow<Long?>(null)

    private data class FilterParams(
        val dateRange: DateRangeFilter,
        val customStart: Long?,
        val customEnd: Long?,
        val query: String,
        val typeFilter: TransactionType?,
        val categoryFilter: Long?
    )

    private val filterParams: kotlinx.coroutines.flow.Flow<FilterParams> = combine(
        combine(selectedDateRange, customStartDate, customEndDate) { range, start, end -> Triple(range, start, end) },
        combine(searchQuery, selectedTypeFilter, selectedCategoryFilter) { q, t, c -> Triple(q, t, c) }
    ) { (range, start, end), (q, t, c) ->
        FilterParams(range, start, end, q, t, c)
    }

    private val allTransactions = repository.getAllTransactionsWithCategory()

    val filteredTransactions: StateFlow<List<TransactionWithCategory>> = combine(
        allTransactions,
        filterParams
    ) { transactions, filters ->
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        val (rangeStart, rangeEnd) = when (filters.dateRange) {
            DateRangeFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            DateRangeFilter.THIS_MONTH -> {
                val s = now.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val e = now.withDayOfMonth(now.lengthOfMonth()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(s, e)
            }
            DateRangeFilter.LAST_MONTH -> {
                val lastMonth = now.minusMonths(1)
                val s = lastMonth.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val e = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(s, e)
            }
            DateRangeFilter.LAST_3_MONTHS -> {
                val startMonth = now.minusMonths(2).withDayOfMonth(1)
                val s = startMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                val e = now.withDayOfMonth(now.lengthOfMonth()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(s, e)
            }
            DateRangeFilter.THIS_YEAR -> {
                val s = now.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val e = now.withDayOfYear(now.lengthOfYear()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(s, e)
            }
            DateRangeFilter.CUSTOM -> {
                // The range picker reports UTC-midnight dates; turn them into full local days
                // so the start day and the whole end day are included.
                val s = filters.customStart?.let { utcDateToLocalDate(it).atStartOfDay(zone).toInstant().toEpochMilli() } ?: 0L
                val e = filters.customEnd?.let { utcDateToLocalDate(it).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1 } ?: Long.MAX_VALUE
                Pair(s, e)
            }
        }

        transactions.filter { item ->
            val d = item.transaction.dateTimestamp
            val matchesDate = d in rangeStart..rangeEnd

            val matchesQuery = filters.query.isBlank() ||
                    item.transaction.title.contains(filters.query, ignoreCase = true) ||
                    (item.transaction.note?.contains(filters.query, ignoreCase = true) == true) ||
                    (item.category?.name?.contains(filters.query, ignoreCase = true) == true)

            val matchesType = filters.typeFilter == null || item.transaction.type == filters.typeFilter
            val matchesCategory = filters.categoryFilter == null || item.transaction.categoryId == filters.categoryFilter

            matchesDate && matchesQuery && matchesType && matchesCategory
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<MonthlySummary> = filteredTransactions.map { transactions ->
        var income = 0.0
        var expense = 0.0
        for (t in transactions) {
            when (t.transaction.type) {
                TransactionType.INCOME -> income += t.transaction.amount
                TransactionType.EXPENSE -> expense += t.transaction.amount
                else -> {}
            }
        }
        MonthlySummary(
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary(0.0, 0.0, 0.0))

    val dateGroupedTransactions: StateFlow<List<TransactionDateGroup>> = combine(
        filteredTransactions,
        currencySymbol
    ) { transactions, symbol ->
        groupTransactionsByDate(transactions, symbol)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedTransactions: StateFlow<Map<String, List<TransactionWithCategory>>> = filteredTransactions.map { list ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        list.groupBy { item ->
            val itemDate = java.time.Instant.ofEpochMilli(item.transaction.dateTimestamp).atZone(zone).toLocalDate()
            when (itemDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> itemDate.format(FULL_HEADER_FORMAT)
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onTypeFilterSelected(type: TransactionType?) {
        selectedTypeFilter.value = type
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        selectedCategoryFilter.value = categoryId
    }

    fun toggleBalanceHidden() {
        viewModelScope.launch {
            repository.setBalanceHidden(!isBalanceHidden.value)
        }
    }

    /** Adds a new transaction, or updates [existingId] while keeping fields the sheet doesn't edit
     *  (recurring link, receipt, split group). */
    fun saveTransaction(existingId: Long?, draft: TransactionDraft) {
        viewModelScope.launch {
            if (existingId == null) {
                val insertedId = repository.insertTransaction(
                    TransactionEntity(
                        title = draft.title,
                        amount = draft.amount,
                        type = draft.type,
                        categoryId = draft.categoryId,
                        dateTimestamp = draft.dateTimestamp,
                        note = draft.note,
                        accountId = draft.accountId,
                        toAccountId = draft.toAccountId
                    )
                )
                repository.setTagsForTransaction(insertedId, draft.tagIds)
            } else {
                val existing = repository.getTransactionById(existingId) ?: return@launch
                repository.updateTransaction(
                    existing.copy(
                        title = draft.title,
                        amount = draft.amount,
                        type = draft.type,
                        categoryId = draft.categoryId,
                        dateTimestamp = draft.dateTimestamp,
                        note = draft.note,
                        accountId = draft.accountId,
                        toAccountId = draft.toAccountId
                    )
                )
                repository.setTagsForTransaction(existingId, draft.tagIds)
            }
        }
    }

    suspend fun getTagIdsForTransaction(transactionId: Long): List<Long> =
        repository.getTagIdsForTransaction(transactionId)

    /** Deletes the transaction; the returned value holds its tag ids for [undoDelete]. */
    fun deleteTransaction(transaction: TransactionEntity): Deferred<List<Long>> =
        viewModelScope.async { repository.deleteTransactionKeepingTags(transaction) }

    fun undoDelete(transaction: TransactionEntity, deletion: Deferred<List<Long>>) {
        viewModelScope.launch {
            repository.restoreTransaction(transaction, deletion.await())
        }
    }

    private fun utcDateToLocalDate(utcMillis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(utcMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()

    companion object {
        private val FULL_HEADER_FORMAT = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
    }

    private fun groupTransactionsByDate(list: List<TransactionWithCategory>, symbol: String): List<TransactionDateGroup> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)

        val groups = list.groupBy { item ->
            val itemDate = java.time.Instant.ofEpochMilli(item.transaction.dateTimestamp)
                .atZone(zone)
                .toLocalDate()
            when (itemDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> itemDate.format(FULL_HEADER_FORMAT)
            }
        }

        return groups.map { (header, items) ->
            val dayTotal = items.sumOf {
                when (it.transaction.type) {
                    TransactionType.INCOME -> it.transaction.amount
                    TransactionType.EXPENSE -> -it.transaction.amount
                    // Transfers only move money between your own accounts
                    TransactionType.TRANSFER -> 0.0
                }
            }
            val formatted = IndianCurrencyFormatter.formatInr(Math.abs(dayTotal), symbol)
            val prefix = if (dayTotal >= 0) "+$formatted" else "-$formatted"
            TransactionDateGroup(
                dateHeader = header,
                items = items,
                dayTotalFormatted = prefix,
                isPositive = dayTotal >= 0
            )
        }
    }
}

data class TransactionDateGroup(
    val dateHeader: String,
    val items: List<TransactionWithCategory>,
    val dayTotalFormatted: String,
    val isPositive: Boolean
)
