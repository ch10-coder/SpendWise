package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spendwise.app.data.repository.SpendWiseRepository

class ViewModelFactory(
    private val repository: SpendWiseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(TimelineViewModel::class.java) -> {
                TimelineViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> {
                AnalyticsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(BudgetViewModel::class.java) -> {
                BudgetViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) -> {
                CategoriesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(DataToolsViewModel::class.java) -> {
                DataToolsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(AccountsViewModel::class.java) -> {
                AccountsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(TagsViewModel::class.java) -> {
                TagsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
