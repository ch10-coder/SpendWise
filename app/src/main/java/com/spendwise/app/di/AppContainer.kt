package com.spendwise.app.di

import android.content.Context
import com.spendwise.app.data.local.AppDatabase
import com.spendwise.app.data.preferences.UserPreferencesManager
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.data.service.CsvService
import com.spendwise.app.data.service.RecurringTransactionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val repository: SpendWiseRepository
    val userPreferencesManager: UserPreferencesManager
    val recurringTransactionService: RecurringTransactionService
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context.applicationContext, applicationScope)
    }

    override val userPreferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(context.applicationContext)
    }

    private val csvService: CsvService by lazy {
        CsvService(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            accountDao = database.accountDao()
        )
    }

    override val recurringTransactionService: RecurringTransactionService by lazy {
        RecurringTransactionService(
            transactionDao = database.transactionDao(),
            recurringTransactionDao = database.recurringTransactionDao()
        )
    }

    override val repository: SpendWiseRepository by lazy {
        SpendWiseRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            accountDao = database.accountDao(),
            tagDao = database.tagDao(),
            recurringTransactionDao = database.recurringTransactionDao(),
            savingsGoalDao = database.savingsGoalDao(),
            preferencesManager = userPreferencesManager,
            csvService = csvService,
            recurringTransactionService = recurringTransactionService
        )
    }
}
