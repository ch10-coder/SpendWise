package com.spendwise.app.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spendwise.app.data.repository.SpendWiseRepository
import com.spendwise.app.ui.components.DrawerDestination
import com.spendwise.app.ui.components.SpendWiseDrawerContent
import com.spendwise.app.ui.screens.analytics.AnalyticsScreen
import com.spendwise.app.ui.screens.budget.BudgetScreen
import com.spendwise.app.ui.screens.categories.CategoriesScreen
import com.spendwise.app.ui.screens.data.DataToolsScreen
import com.spendwise.app.ui.screens.settings.SettingsScreen
import com.spendwise.app.ui.screens.timeline.TimelineScreen
import com.spendwise.app.viewmodel.AnalyticsViewModel
import com.spendwise.app.viewmodel.BudgetViewModel
import com.spendwise.app.viewmodel.CategoriesViewModel
import com.spendwise.app.viewmodel.DataToolsViewModel
import com.spendwise.app.viewmodel.SettingsViewModel
import com.spendwise.app.viewmodel.TimelineViewModel
import com.spendwise.app.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: SpendWiseRepository,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(DrawerDestination.TIMELINE) }

    val factory = remember(repository) { ViewModelFactory(repository) }

    val timelineViewModel: TimelineViewModel = viewModel(factory = factory)
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)
    val budgetViewModel: BudgetViewModel = viewModel(factory = factory)
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = factory)
    val dataToolsViewModel: DataToolsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val currencySymbol by timelineViewModel.currencySymbol.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SpendWiseDrawerContent(
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    currentDestination = destination
                    scope.launch { drawerState.close() }
                },
                currencySymbol = currencySymbol
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDestination.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Drawer"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentDestination) {
                    DrawerDestination.TIMELINE -> TimelineScreen(viewModel = timelineViewModel)
                    DrawerDestination.ANALYTICS -> AnalyticsScreen(viewModel = analyticsViewModel)
                    DrawerDestination.BUDGETS -> BudgetScreen(viewModel = budgetViewModel)
                    DrawerDestination.CATEGORIES -> CategoriesScreen(viewModel = categoriesViewModel)
                    DrawerDestination.DATA_TOOLS -> DataToolsScreen(viewModel = dataToolsViewModel)
                    DrawerDestination.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}
