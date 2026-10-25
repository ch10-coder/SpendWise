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

    /** Creates a new account, or updates [existing] in place (keeping its id and creation date). */
    fun saveAccount(
        existing: AccountEntity?,
        name: String,
        type: AccountType,
        iconName: String,
        colorHex: String,
        openingBalance: Double,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            if (existing == null) {
                repository.insertAccount(
                    AccountEntity(
                        name = name.trim(),
                        type = type,
                        balance = openingBalance,
                        iconName = iconName,
                        colorHex = colorHex,
                        isDefault = isDefault
                    )
                )
            } else {
                repository.updateAccount(
                    existing.copy(
                        name = name.trim(),
                        type = type,
                        balance = openingBalance,
                        iconName = iconName,
                        colorHex = colorHex,
                        isDefault = isDefault
                    )
                )
            }
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }
}
