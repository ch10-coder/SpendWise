package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.repository.SpendWiseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

sealed class DataToolState {
    object Idle : DataToolState()
    object Processing : DataToolState()
    data class Success(val message: String) : DataToolState()
    data class Error(val errorMessage: String) : DataToolState()
}

class DataToolsViewModel(
    private val repository: SpendWiseRepository
) : ViewModel() {

    private val _exportState = MutableStateFlow<DataToolState>(DataToolState.Idle)
    val exportState = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<DataToolState>(DataToolState.Idle)
    val importState = _importState.asStateFlow()

    fun exportToCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            _exportState.value = DataToolState.Processing
            val result = repository.exportTransactionsToCsv(outputStream)
            result.onSuccess { count ->
                _exportState.value = DataToolState.Success("Successfully exported $count transactions to CSV file.")
            }.onFailure { error ->
                _exportState.value = DataToolState.Error(error.localizedMessage ?: "Failed to export data")
            }
        }
    }

    fun importFromCsv(inputStream: InputStream) {
        viewModelScope.launch {
            _importState.value = DataToolState.Processing
            val result = repository.importTransactionsFromCsv(inputStream)
            result.onSuccess { count ->
                _importState.value = DataToolState.Success("Successfully imported $count transactions from CSV file.")
            }.onFailure { error ->
                _importState.value = DataToolState.Error(error.localizedMessage ?: "Failed to import data")
            }
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
            _importState.value = DataToolState.Success("All transactions have been successfully cleared.")
        }
    }

    fun resetState() {
        _exportState.value = DataToolState.Idle
        _importState.value = DataToolState.Idle
    }
}
