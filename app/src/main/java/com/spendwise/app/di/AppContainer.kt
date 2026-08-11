package com.spendwise.app.di

import android.content.Context
import com.spendwise.app.data.local.AppDatabase
import com.spendwise.app.data.preferences.UserPreferencesManager
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.data.service.CsvService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val repository: SpendWiseRepository
    val userPreferencesManager: UserPreferencesManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context, applicationScope)
    }

    override val userPreferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(context)
    }

    private val csvService: CsvService by lazy {
        CsvService(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao()
        )
    }

    override val repository: SpendWiseRepository by lazy {
        SpendWiseRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            preferencesManager = userPreferencesManager,
            csvService = csvService
        )
    }
}
