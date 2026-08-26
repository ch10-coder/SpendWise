package com.spendwise.app.ui.screens.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import com.spendwise.app.ui.components.AddEditTransactionSheet
import com.spendwise.app.ui.components.DateRangeSelector
import com.spendwise.app.ui.components.EmptyState
import com.spendwise.app.ui.components.SummaryCard
import com.spendwise.app.ui.components.TransactionItem
import com.spendwise.app.ui.theme.Emerald500
import com.spendwise.app.ui.theme.Rose500
import com.spendwise.app.viewmodel.TimelineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null
) {
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val groupedTransactions by viewModel.groupedTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsState()
    val selectedDateRange by viewModel.selectedDateRange.collectAsState()
    val customStartDate by viewModel.customStartDate.collectAsState()
    val customEndDate by viewModel.customEndDate.collectAsState()

    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val scope = rememberCoroutineScope()
    var showAddEditSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<com.spendwise.app.data.local.entity.TransactionEntity?>(null) }

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
            // Balance Summary Card with Privacy Toggle
            item(key = "summary_card", contentType = "summary") {
                SummaryCard(
                    totalBalance = summary.netBalance,
                    totalIncome = summary.totalIncome,
                    totalExpense = summary.totalExpense,
                    currencySymbol = currencySymbol,
                    isBalanceHidden = isBalanceHidden,
                    onToggleHidden = viewModel::toggleBalanceHidden
                )
            }

            // Date Range Filter Chips
            item(key = "date_range_selector", contentType = "filter") {
                DateRangeSelector(
                    selectedFilter = selectedDateRange,
                    onFilterSelected = { viewModel.selectedDateRange.value = it },
                    customStartDate = customStartDate,
                    customEndDate = customEndDate,
                    onCustomRangeSelected = { start, end ->
                        viewModel.customStartDate.value = start
                        viewModel.customEndDate.value = end
                    }
                )
            }

            // Search Bar & Type Filter Section
            item(key = "search_and_type_filter", contentType = "search") {
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

            // Empty State or Grouped Transactions
            if (groupedTransactions.isEmpty()) {
                item(key = "empty_state", contentType = "empty") {
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
                    item(key = "header_$dateHeader", contentType = "date_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val dayTotal = remember(items) {
                                items.sumOf {
                                    if (it.transaction.type == TransactionType.INCOME) it.transaction.amount else -it.transaction.amount
                                }
                            }
                            val formattedDayTotal = remember(dayTotal) {
                                String.format(java.util.Locale.US, "%,.2f", dayTotal)
                            }
                            val dayPrefix = if (dayTotal >= 0) "+$currencySymbol" else "-$currencySymbol"
                            val dayColor = if (dayTotal >= 0) Emerald500 else Rose500

                            Surface(
                                color = dayColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$dayPrefix${formattedDayTotal.replace("-", "")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = dayColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    items(
                        items = items,
                        key = { it.transaction.id },
                        contentType = { "transaction_item" }
                    ) { item ->
                        val accountName = accountMap[item.transaction.accountId]?.name
                        TransactionItem(
                            item = item,
                            currencySymbol = currencySymbol,
                            accountName = accountName,
                            isRecurring = item.transaction.recurringRuleId != null,
                            hasReceipt = !item.transaction.receiptPath.isNullOrBlank(),
                            onClick = {
                                itemToEdit = item
                                showAddEditSheet = true
                            },
                            onDelete = {
                                pendingDeleteTransaction = item.transaction
                            }
                        )
                    }
                }
            }
        }
    }

    // Deletion Confirmation Dialog
    if (pendingDeleteTransaction != null) {
        val txToDelete = pendingDeleteTransaction!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text("Delete Transaction") },
            text = {
                Text(
                    "Are you sure you want to delete \"${txToDelete.title}\" (${currencySymbol}${String.format(java.util.Locale.US, "%,.2f", txToDelete.amount)})?"
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.deleteTransaction(txToDelete.id)
                        pendingDeleteTransaction = null
                        snackbarHostState?.let { host ->
                            scope.launch {
                                val result = host.showSnackbar(
                                    message = "Transaction deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.addTransaction(
                                        title = txToDelete.title,
                                        amount = txToDelete.amount,
                                        type = txToDelete.type,
                                        categoryId = txToDelete.categoryId,
                                        dateTimestamp = txToDelete.dateTimestamp,
                                        note = txToDelete.note,
                                        accountId = txToDelete.accountId,
                                        toAccountId = txToDelete.toAccountId
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Add / Edit Sheet
    if (showAddEditSheet) {
        AddEditTransactionSheet(
            initialItem = itemToEdit,
            categories = categories,
            currencySymbol = currencySymbol,
            accounts = accounts,
            selectedAccountId = itemToEdit?.transaction?.accountId ?: accounts.firstOrNull { it.isDefault }?.id,
            tags = tags,
            onDismiss = { showAddEditSheet = false },
            onSave = { title, amount, type, categoryId, dateTimestamp, note ->
                if (itemToEdit == null) {
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        dateTimestamp = dateTimestamp,
                        note = note
                    )
                } else {
                    viewModel.updateTransaction(
                        id = itemToEdit!!.transaction.id,
                        title = title,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        dateTimestamp = dateTimestamp,
                        note = note,
                        accountId = itemToEdit!!.transaction.accountId,
                        toAccountId = itemToEdit!!.transaction.toAccountId
                    )
                }
                showAddEditSheet = false
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id)
                showAddEditSheet = false
            }
        )
    }
}
