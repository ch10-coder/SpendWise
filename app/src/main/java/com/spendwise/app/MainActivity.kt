package com.spendwise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spendwise.app.ui.screens.main.MainScreen
import com.spendwise.app.ui.theme.SpendWiseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SpendWiseApplication
        val repository = app.container.repository

        lifecycleScope.launch {
            repository.processRecurringTransactions()
        }

        setContent {
            val themeMode by repository.themeMode.collectAsStateWithLifecycle(initialValue = repository.initialThemeMode)

            SpendWiseTheme(
                themeMode = themeMode
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(repository = repository)
                }
            }
        }
    }
}
