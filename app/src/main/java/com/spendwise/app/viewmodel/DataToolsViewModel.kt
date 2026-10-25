package com.spendwise.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendwise.app.data.repository.SpendWiseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _resetDataState = MutableStateFlow<DataToolState>(DataToolState.Idle)
    val resetDataState = _resetDataState.asStateFlow()

    private var autoDismissJob: Job? = null

    fun exportToCsv(outputStream: OutputStream) {
        viewModelScope.launch {
            _exportState.value = DataToolState.Processing
            val result = repository.exportTransactionsToCsv(outputStream)
            result.onSuccess { count ->
                _exportState.value = DataToolState.Success("Successfully exported $count transactions to CSV file.")
                startAutoDismiss()
            }.onFailure { error ->
                _exportState.value = DataToolState.Error(error.localizedMessage ?: "Failed to export data")
                startAutoDismiss()
            }
        }
    }

    fun importFromCsv(inputStream: InputStream) {
        viewModelScope.launch {
            _importState.value = DataToolState.Processing
            val result = repository.importTransactionsFromCsv(inputStream)
            result.onSuccess { summary ->
                val skipped = buildList {
                    if (summary.skippedDuplicates > 0) add("${summary.skippedDuplicates} already in the app")
                    if (summary.skippedInvalid > 0) add("${summary.skippedInvalid} without a valid amount")
                }
                val skippedText = if (skipped.isEmpty()) "" else " Skipped ${skipped.joinToString(" and ")}."
                _importState.value = DataToolState.Success("Imported ${summary.imported} transactions from the CSV file.$skippedText")
                startAutoDismiss()
            }.onFailure { error ->
                _importState.value = DataToolState.Error(error.localizedMessage ?: "Failed to import data")
                startAutoDismiss()
            }
        }
    }

    fun resetData() {
        viewModelScope.launch {
            _resetDataState.value = DataToolState.Processing
            repository.deleteAllTransactions()
            _resetDataState.value = DataToolState.Success("All transactions have been successfully cleared.")
            startAutoDismiss()
        }
    }

    fun resetState() {
        _exportState.value = DataToolState.Idle
        _importState.value = DataToolState.Idle
        _resetDataState.value = DataToolState.Idle
    }
    
    private fun startAutoDismiss() {
        autoDismissJob?.cancel()
        autoDismissJob = viewModelScope.launch {
            delay(5000)
            resetState()
        }
    }
}
