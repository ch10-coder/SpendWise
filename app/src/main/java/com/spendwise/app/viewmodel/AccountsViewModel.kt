package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.AccountType
import com.spendwise.app.model.AccountWithBalance
import com.spendwise.app.model.CurrencyOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountWithBalance>> = repository.getAccountsWithBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val isBalanceHidden: StateFlow<Boolean> = repository.isBalanceHidden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun addAccount(
        name: String,
        type: AccountType,
        balance: Double,
        iconName: String,
        colorHex: String,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name.trim(),
                    type = type,
                    balance = balance,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = isDefault
                )
            )
        }
    }

    fun updateAccount(
        id: Long,
        name: String,
        type: AccountType,
        balance: Double,
        iconName: String,
        colorHex: String,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            repository.updateAccount(
                AccountEntity(
                    id = id,
                    name = name.trim(),
                    type = type,
                    balance = balance,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = isDefault
                )
            )
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }
}
