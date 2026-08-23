package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.SavingsGoalEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.CurrencyOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsGoalsViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoalEntity>> = repository.getAllSavingsGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val isBalanceHidden: StateFlow<Boolean> = repository.isBalanceHidden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun addGoal(
        name: String,
        targetAmount: Double,
        currentAmount: Double = 0.0,
        deadline: Long? = null,
        iconName: String = "Savings",
        colorHex: String = "#10B981"
    ) {
        viewModelScope.launch {
            repository.insertSavingsGoal(
                SavingsGoalEntity(
                    name = name.trim(),
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    deadline = deadline,
                    iconName = iconName,
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun deposit(goalId: Long, amount: Double) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.depositToSavingsGoal(goalId, amount)
        }
    }

    fun withdraw(goalId: Long, amount: Double) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.withdrawFromSavingsGoal(goalId, amount)
        }
    }
}
