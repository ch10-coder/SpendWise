package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.local.entity.TagEntity
import com.spendwise.app.data.repository.SpendWiseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagsViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    val tags: StateFlow<List<TagEntity>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTag(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertTag(
                TagEntity(
                    name = name.trim(),
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateTag(id: Long, name: String, colorHex: String) {
        viewModelScope.launch {
            repository.updateTag(
                TagEntity(
                    id = id,
                    name = name.trim(),
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }
}
