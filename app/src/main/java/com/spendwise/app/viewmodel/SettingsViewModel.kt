package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    val currentCurrencyCode: StateFlow<String> = repository.currencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.code)

    val currentCurrencySymbol: StateFlow<String> = repository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurrencyOption.DEFAULT.symbol)

    val currentThemeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val currentThemePalette: StateFlow<com.spendwise.app.model.ThemePalette> = repository.themePalette
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.spendwise.app.model.ThemePalette.EMERALD)

    val isDynamicColor: StateFlow<Boolean> = repository.isDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // New properties based on UserPreferencesManager enhancements
    val isAppLockEnabled: StateFlow<Boolean> = repository.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAmoledBlack: StateFlow<Boolean> = repository.isAmoledBlack
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val dailyReminderTime: StateFlow<String?> = repository.dailyReminderTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectCurrency(currency: CurrencyOption) {
        viewModelScope.launch {
            repository.setCurrency(currency)
        }
    }

    fun selectThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun selectThemePalette(palette: com.spendwise.app.model.ThemePalette) {
        viewModelScope.launch {
            repository.setThemePalette(palette)
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDynamicColor(enabled)
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppLockEnabled(enabled)
        }
    }

    fun toggleAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAmoledBlack(enabled)
        }
    }

    fun setDailyReminderTime(time: String?) {
        viewModelScope.launch {
            repository.setDailyReminderTime(time)
        }
    }
}
