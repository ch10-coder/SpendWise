package com.spendwise.app.ui.screens.timeline

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import com.spendwise.app.ui.components.AddEditTransactionSheet
import com.spendwise.app.ui.components.DateRangeSelector
import com.spendwise.app.ui.components.EmptyState
import com.spendwise.app.ui.components.SummaryCard
import com.spendwise.app.ui.components.TransactionItem
import com.spendwise.app.ui.theme.InflowGreen
import com.spendwise.app.ui.theme.OutflowRed
import com.spendwise.app.util.IndianCurrencyFormatter
import com.spendwise.app.viewmodel.TimelineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null
) {
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val dateGroupedTransactions by viewModel.dateGroupedTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val selectedDateRange by viewModel.selectedDateRange.collectAsStateWithLifecycle()
    val customStartDate by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEndDate by viewModel.customEndDate.collectAsStateWithLifecycle()

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
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction"
                )
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

            // Search Bar & Type Filter Section (Google Style Search Pill)
            item(key = "search_and_type_filter", contentType = "search") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("Search transactions, notes...", style = MaterialTheme.typography.bodyMedium) },
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
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
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
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
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
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
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
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Empty State or Grouped Transactions
            if (dateGroupedTransactions.isEmpty()) {
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
                // Grouped Transactions List using pre-aggregated day totals (120 FPS buttery smooth)
                dateGroupedTransactions.forEach { group ->
                    item(key = "header_${group.dateHeader}", contentType = "date_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.dateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val dayColor = if (group.isPositive) InflowGreen else OutflowRed

                            Surface(
                                color = dayColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = group.dayTotalFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = dayColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    items(
                        items = group.items,
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

                // Centered "Made with ❤️ in India • SpendWise Bharat" badge at the bottom of the list
                item(key = "bharat_footer_badge", contentType = "footer") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🇮🇳",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "Made with ❤️ in India • SpendWise Bharat",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Deletion Confirmation Dialog (Material 3 Styled)
    if (pendingDeleteTransaction != null) {
        val txToDelete = pendingDeleteTransaction!!
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete \"${txToDelete.title}\" (${IndianCurrencyFormatter.formatInr(txToDelete.amount, currencySymbol)})?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
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
                TextButton(onClick = { pendingDeleteTransaction = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
