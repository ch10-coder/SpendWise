package com.spendwise.app.ui.screens.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import com.spendwise.app.ui.components.AddEditTransactionSheet
import com.spendwise.app.ui.components.EmptyState
import com.spendwise.app.ui.components.SummaryCard
import com.spendwise.app.ui.components.TransactionItem
import com.spendwise.app.ui.theme.Emerald500
import com.spendwise.app.ui.theme.Rose500
import com.spendwise.app.viewmodel.TimelineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val groupedTransactions by viewModel.groupedTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()

    var showAddEditSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddEditSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Balance Summary Card
            item {
                SummaryCard(
                    totalBalance = summary.netBalance,
                    totalIncome = summary.totalIncome,
                    totalExpense = summary.totalExpense,
                    currencySymbol = currencySymbol
                )
            }

            // Search Bar & Filter Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Type Filter Chips (All, Expense, Income)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == null,
                                onClick = { viewModel.onTypeFilterSelected(null) },
                                label = { Text("All") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.EXPENSE,
                                onClick = {
                                    viewModel.onTypeFilterSelected(
                                        if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                                    )
                                },
                                label = { Text("Expenses") },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Rose500.copy(alpha = 0.2f),
                                    selectedLabelColor = Rose500
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.INCOME,
                                onClick = {
                                    viewModel.onTypeFilterSelected(
                                        if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                                    )
                                },
                                label = { Text("Income") },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald500
                                )
                            )
                        }
                    }
                }
            }

            // Empty State
            if (groupedTransactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Transactions Found",
                        description = if (searchQuery.isNotBlank() || selectedTypeFilter != null) {
                            "Try clearing your search or filters."
                        } else {
                            "Tap the '+' button below to log your first income or expense."
                        },
                        actionButtonText = if (searchQuery.isNotBlank() || selectedTypeFilter != null) "Clear Filters" else "Add Transaction",
                        onActionClick = {
                            if (searchQuery.isNotBlank() || selectedTypeFilter != null) {
                                viewModel.onSearchQueryChanged("")
                                viewModel.onTypeFilterSelected(null)
                            } else {
                                itemToEdit = null
                                showAddEditSheet = true
                            }
                        }
                    )
                }
            } else {
                // Grouped Transactions List
                groupedTransactions.forEach { (dateHeader, items) ->
                    // Sticky / Section Date Header
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val dayTotal = items.sumOf {
                                    if (it.transaction.type == TransactionType.INCOME) it.transaction.amount else -it.transaction.amount
                                }
                                val formattedDayTotal = String.format(java.util.Locale.US, "%,.2f", dayTotal)
                                val dayPrefix = if (dayTotal >= 0) "+$currencySymbol" else "-$currencySymbol"
                                val dayColor = if (dayTotal >= 0) Emerald500 else Rose500

                                Text(
                                    text = "$dayPrefix${formattedDayTotal.replace("-", "")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = dayColor
                                )
                            }
                        }
                    }

                    items(items, key = { it.transaction.id }) { item ->
                        TransactionItem(
                            item = item,
                            currencySymbol = currencySymbol,
                            onClick = {
                                itemToEdit = item
                                showAddEditSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Add / Edit Sheet
    if (showAddEditSheet) {
        AddEditTransactionSheet(
            initialItem = itemToEdit,
            categories = categories,
            currencySymbol = currencySymbol,
            onDismiss = { showAddEditSheet = false },
            onSave = { title, amount, type, categoryId, dateTimestamp, note ->
                if (itemToEdit == null) {
                    viewModel.addTransaction(title, amount, type, categoryId, dateTimestamp, note)
                } else {
                    viewModel.updateTransaction(
                        itemToEdit!!.transaction.id,
                        title,
                        amount,
                        type,
                        categoryId,
                        dateTimestamp,
                        note
                    )
                }
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
            }
        )
    }
}
