package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.repository.MonthlySummary
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimelineViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val currentYear = calendar.get(Calendar.YEAR)
    private val currentMonth = calendar.get(Calendar.MONTH) + 1

    val currencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<MonthlySummary> = repository.getMonthlySummary(currentYear, currentMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary(0.0, 0.0, 0.0))

    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedCategoryFilter = MutableStateFlow<Long?>(null)

    private val allTransactions = repository.getAllTransactionsWithCategory()

    val filteredTransactions: StateFlow<List<TransactionWithCategory>> = combine(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter
    ) { transactions, query, typeFilter, catFilter ->
        transactions.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.transaction.title.contains(query, ignoreCase = true) ||
                    (item.transaction.note?.contains(query, ignoreCase = true) == true) ||
                    (item.category?.name?.contains(query, ignoreCase = true) == true)

            val matchesType = typeFilter == null || item.transaction.type == typeFilter
            val matchesCategory = catFilter == null || item.transaction.categoryId == catFilter

            matchesQuery && matchesType && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedTransactions: StateFlow<Map<String, List<TransactionWithCategory>>> = filteredTransactions.combine(
        MutableStateFlow(Unit)
    ) { list, _ ->
        groupTransactionsByDate(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onTypeFilterSelected(type: TransactionType?) {
        selectedTypeFilter.value = type
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        selectedCategoryFilter.value = categoryId
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        dateTimestamp: Long,
        note: String?
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    dateTimestamp = dateTimestamp,
                    note = note
                )
            )
        }
    }

    fun updateTransaction(
        id: Long,
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        dateTimestamp: Long,
        note: String?
    ) {
        viewModelScope.launch {
            repository.updateTransaction(
                TransactionEntity(
                    id = id,
                    title = title,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    dateTimestamp = dateTimestamp,
                    note = note
                )
            )
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    private fun groupTransactionsByDate(list: List<TransactionWithCategory>): Map<String, List<TransactionWithCategory>> {
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val todayDate = formatDateOnly(todayCal.time)
        val yesterdayDate = formatDateOnly(yesterdayCal.time)

        val fullHeaderFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        return list.groupBy { item ->
            val itemDate = Date(item.transaction.dateTimestamp)
            val dateOnlyStr = formatDateOnly(itemDate)
            when (dateOnlyStr) {
                todayDate -> "Today"
                yesterdayDate -> "Yesterday"
                else -> fullHeaderFormat.format(itemDate)
            }
        }
    }

    private fun formatDateOnly(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }
}
