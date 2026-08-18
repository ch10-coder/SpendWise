package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.BudgetWithSpending
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.OverallBudgetSummary
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    private val currentCalendar = Calendar.getInstance()

    val selectedYear = MutableStateFlow(currentCalendar.get(Calendar.YEAR))
    val selectedMonth = MutableStateFlow(currentCalendar.get(Calendar.MONTH) + 1)

    val currencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val expenseCategories: StateFlow<List<CategoryEntity>> = repository.getCategoriesByType(TransactionType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetsWithSpending: StateFlow<List<BudgetWithSpending>> = combine(
        selectedYear, selectedMonth
    ) { year, month ->
        Pair(year, month)
    }.flatMapLatest { (year, month) ->
        repository.getBudgetsWithSpendingForMonth(year, month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overallSummary: StateFlow<OverallBudgetSummary> = budgetsWithSpending.map { budgets ->
        val totalBudget = budgets.sumOf { it.limitAmount }
        val totalSpent = budgets.sumOf { it.spentAmount }
        val progress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 2f) else 0f
        OverallBudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remaining = totalBudget - totalSpent,
            progressFraction = progress,
            isOverBudget = totalSpent > totalBudget
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallBudgetSummary(0.0, 0.0, 0.0, 0f, false))

    fun getFormattedMonthYear(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear.value)
        cal.set(Calendar.MONTH, selectedMonth.value - 1)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun previousMonth() {
        if (selectedMonth.value == 1) {
            val y = selectedYear.value - 1
            selectedMonth.value = 12
            selectedYear.value = y
        } else {
            selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (selectedMonth.value == 12) {
            val y = selectedYear.value + 1
            selectedMonth.value = 1
            selectedYear.value = y
        } else {
            selectedMonth.value += 1
        }
    }

    fun setBudget(categoryId: Long, limitAmount: Double) {
        viewModelScope.launch {
            val monthYear = String.format(Locale.US, "%04d-%02d", selectedYear.value, selectedMonth.value)
            repository.setBudget(categoryId, limitAmount, monthYear)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun copyBudgetsFromPreviousMonth() {
        viewModelScope.launch {
            repository.getBudgetsFromPreviousMonth(selectedYear.value, selectedMonth.value)
        }
    }
}
