package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedType = MutableStateFlow(TransactionType.EXPENSE)

    private val _pendingDeleteCategory = MutableStateFlow<CategoryEntity?>(null)
    val pendingDeleteCategory = _pendingDeleteCategory.asStateFlow()

    fun onTypeSelected(type: TransactionType) {
        selectedType.value = type
    }

    fun addCategory(name: String, type: TransactionType, iconName: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    type = type,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = false
                )
            )
        }
    }

    fun updateCategory(id: Long, name: String, type: TransactionType, iconName: String, colorHex: String, isDefault: Boolean) {
        viewModelScope.launch {
            repository.updateCategory(
                CategoryEntity(
                    id = id,
                    name = name.trim(),
                    type = type,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = isDefault
                )
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        _pendingDeleteCategory.value = category
    }

    fun confirmDeleteCategory() {
        val category = _pendingDeleteCategory.value ?: return
        viewModelScope.launch {
            repository.deleteCategory(category)
            _pendingDeleteCategory.value = null
        }
    }

    fun cancelDeleteCategory() {
        _pendingDeleteCategory.value = null
    }
}
