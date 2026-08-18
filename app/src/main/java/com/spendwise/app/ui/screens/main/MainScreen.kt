package com.spendwise.app.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.spendwise.app.ui.components.BottomNavDestination
import com.spendwise.app.ui.components.DrawerDestination
import com.spendwise.app.ui.components.SpendWiseBottomNavBar
import com.spendwise.app.ui.components.SpendWiseDrawerContent
import com.spendwise.app.ui.screens.accounts.AccountsScreen
import com.spendwise.app.ui.screens.analytics.AnalyticsScreen
import com.spendwise.app.ui.screens.budget.BudgetScreen
import com.spendwise.app.ui.screens.categories.CategoriesScreen
import com.spendwise.app.ui.screens.data.DataToolsScreen
import com.spendwise.app.ui.screens.settings.SettingsScreen
import com.spendwise.app.ui.screens.tags.TagsScreen
import com.spendwise.app.ui.screens.timeline.TimelineScreen
import com.spendwise.app.viewmodel.AccountsViewModel
import com.spendwise.app.viewmodel.AnalyticsViewModel
import com.spendwise.app.viewmodel.BudgetViewModel
import com.spendwise.app.viewmodel.CategoriesViewModel
import com.spendwise.app.viewmodel.DataToolsViewModel
import com.spendwise.app.viewmodel.SettingsViewModel
import com.spendwise.app.viewmodel.TagsViewModel
import com.spendwise.app.viewmodel.TimelineViewModel
import com.spendwise.app.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

/**
 * Represents which screen is currently active.
 * Bottom nav screens have their own enum, drawer screens have theirs.
 */
sealed class ActiveScreen {
    data class BottomNav(val destination: BottomNavDestination) : ActiveScreen()
    data class Drawer(val destination: DrawerDestination) : ActiveScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: SpendWiseRepository,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.BottomNav(BottomNavDestination.TIMELINE)) }

    val factory = remember(repository) { ViewModelFactory(repository) }

    // ViewModels — lazily instantiated by Compose's viewModel()
    val timelineViewModel: TimelineViewModel = viewModel(factory = factory)
    val analyticsViewModel: AnalyticsViewModel = viewModel(factory = factory)
    val budgetViewModel: BudgetViewModel = viewModel(factory = factory)
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = factory)
    val dataToolsViewModel: DataToolsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val accountsViewModel: AccountsViewModel = viewModel(factory = factory)
    val tagsViewModel: TagsViewModel = viewModel(factory = factory)

    val currencySymbol by timelineViewModel.currencySymbol.collectAsState()

    // Derive current title from active screen
    val currentTitle = when (val screen = activeScreen) {
        is ActiveScreen.BottomNav -> screen.destination.title
        is ActiveScreen.Drawer -> screen.destination.title
    }

    // Back handler: if not on Timeline, go back to Timeline
    BackHandler(
        enabled = activeScreen !is ActiveScreen.BottomNav ||
                (activeScreen as? ActiveScreen.BottomNav)?.destination != BottomNavDestination.TIMELINE
    ) {
        activeScreen = ActiveScreen.BottomNav(BottomNavDestination.TIMELINE)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SpendWiseDrawerContent(
                currentDestination = (activeScreen as? ActiveScreen.Drawer)?.destination,
                onDestinationSelected = { destination ->
                    activeScreen = ActiveScreen.Drawer(destination)
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
                            text = currentTitle,
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
            bottomBar = {
                SpendWiseBottomNavBar(
                    currentDestination = (activeScreen as? ActiveScreen.BottomNav)?.destination
                        ?: BottomNavDestination.TIMELINE,
                    onDestinationSelected = { destination ->
                        activeScreen = ActiveScreen.BottomNav(destination)
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(
                    targetState = activeScreen,
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is ActiveScreen.BottomNav -> when (screen.destination) {
                            BottomNavDestination.TIMELINE -> TimelineScreen(
                                viewModel = timelineViewModel,
                                snackbarHostState = snackbarHostState
                            )
                            BottomNavDestination.ANALYTICS -> AnalyticsScreen(viewModel = analyticsViewModel)
                            BottomNavDestination.BUDGETS -> BudgetScreen(viewModel = budgetViewModel)
                            BottomNavDestination.ACCOUNTS -> AccountsScreen(viewModel = accountsViewModel)
                        }
                        is ActiveScreen.Drawer -> when (screen.destination) {
                            DrawerDestination.CATEGORIES -> CategoriesScreen(viewModel = categoriesViewModel)
                            DrawerDestination.TAGS -> TagsScreen(viewModel = tagsViewModel)
                            DrawerDestination.DATA_TOOLS -> DataToolsScreen(viewModel = dataToolsViewModel)
                            DrawerDestination.SETTINGS -> SettingsScreen(viewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}
