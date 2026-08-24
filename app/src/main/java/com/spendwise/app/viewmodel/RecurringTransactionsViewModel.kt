package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.RecurringTransactionEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.RecurrenceFrequency
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DUE_SOON_WINDOW_MS = 7L * 24L * 60L * 60L * 1000L

data class RecurringTransactionOverview(
    val totalRules: Int = 0,
    val activeRules: Int = 0,
    val pausedRules: Int = 0,
    val dueSoonRules: Int = 0,
    val nextDueRule: RecurringTransactionEntity? = null
)

class RecurringTransactionsViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    val currencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringTransactionEntity>> = repository.getAllRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overview: StateFlow<RecurringTransactionOverview> = recurringRules.map { rules ->
        val now = System.currentTimeMillis()
        val activeRules = rules.filter { it.isActive }
        val nextDueRule = activeRules.minByOrNull { it.nextOccurrence }
        val dueSoonRules = activeRules.count { rule ->
            rule.nextOccurrence in now..(now + DUE_SOON_WINDOW_MS)
        }

        RecurringTransactionOverview(
            totalRules = rules.size,
            activeRules = activeRules.size,
            pausedRules = rules.count { !it.isActive },
            dueSoonRules = dueSoonRules,
            nextDueRule = nextDueRule
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RecurringTransactionOverview()
    )

    fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: Long?,
        accountId: Long?,
        frequency: RecurrenceFrequency,
        startDate: Long,
        endDate: Long?,
        note: String?,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            repository.addRecurringTransaction(
                RecurringTransactionEntity(
                    title = title.trim(),
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate,
                    nextOccurrence = startDate,
                    isActive = isActive,
                    note = note?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
    }

    fun updateRecurringTransaction(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(rule.copy(title = rule.title.trim(), note = rule.note?.trim()?.takeIf { it.isNotEmpty() }))
        }
    }

    fun setRecurringActive(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleRecurring(id, isActive)
        }
    }

    fun deleteRecurringTransaction(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(rule)
        }
    }
}
