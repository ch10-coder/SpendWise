package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.MonthlyAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    private val currentCalendar = Calendar.getInstance()

    val selectedYear = MutableStateFlow(currentCalendar.get(Calendar.YEAR))
    val selectedMonth = MutableStateFlow(currentCalendar.get(Calendar.MONTH) + 1) // 1-12

    val currencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val monthlyAnalytics: StateFlow<MonthlyAnalytics> = selectedYear.flatMapLatest { year ->
        selectedMonth.flatMapLatest { month ->
            repository.getMonthlyAnalytics(year, month)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyAnalytics("", 0.0, 0.0, 0.0, emptyList(), emptyList())
    )

    fun getFormattedMonthYear(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear.value)
        cal.set(Calendar.MONTH, selectedMonth.value - 1)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun previousMonth() {
        if (selectedMonth.value == 1) {
            selectedMonth.value = 12
            selectedYear.value -= 1
        } else {
            selectedMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (selectedMonth.value == 12) {
            selectedMonth.value = 1
            selectedYear.value += 1
        } else {
            selectedMonth.value += 1
        }
    }
}
