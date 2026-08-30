package com.spendwise.app.ui.screens.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.data.local.entity.RecurringTransactionEntity
import com.spendwise.app.model.RecurrenceFrequency
import com.spendwise.app.model.TransactionType
import com.spendwise.app.ui.components.CategoryIconHelper
import com.spendwise.app.ui.components.EmptyState
import com.spendwise.app.ui.components.RecurringTransactionSheet
import com.spendwise.app.ui.theme.GoogleGreen
import com.spendwise.app.ui.theme.GoogleRed
import com.spendwise.app.ui.theme.GoogleYellow
import com.spendwise.app.viewmodel.RecurringTransactionOverview
import com.spendwise.app.viewmodel.RecurringTransactionsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.time.temporal.ChronoUnit

@Composable
fun RecurringTransactionsScreen(
    viewModel: RecurringTransactionsViewModel,
    modifier: Modifier = Modifier
) {
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val rules by viewModel.recurringRules.collectAsState()
    val overview by viewModel.overview.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRule = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add recurring rule")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                RecurringSummaryCard(
                    overview = overview,
                    currencySymbol = currencySymbol,
                    rules = rules
                )
            }

            if (rules.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Recurring Rules",
                        description = "Create subscriptions, bills, or repeating income so SpendWise can generate them automatically.",
                        icon = Icons.Default.Schedule,
                        actionButtonText = "Add Recurring Rule",
                        onActionClick = {
                            editingRule = null
                            showSheet = true
                        }
                    )
                }
            } else {
                item {
                    Text(
                        text = "All Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(rules, key = { it.id }) { rule ->
                    RecurringRuleCard(
                        rule = rule,
                        currencySymbol = currencySymbol,
                        categories = categories,
                        accounts = accounts,
                        onClick = {
                            editingRule = rule
                            showSheet = true
                        },
                        onToggleActive = { viewModel.setRecurringActive(rule.id, it) },
                        onDelete = { viewModel.deleteRecurringTransaction(rule) }
                    )
                }
            }
        }
    }

    if (showSheet) {
        RecurringTransactionSheet(
            initialRule = editingRule,
            categories = categories,
            accounts = accounts,
            currencySymbol = currencySymbol,
            onDismiss = {
                showSheet = false
                editingRule = null
            },
            onSave = { title, amount, type, categoryId, accountId, frequency, startDate, endDate, note, isActive ->
                val existing = editingRule
                if (existing == null) {
                    viewModel.addRecurringTransaction(
                        title = title,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        frequency = frequency,
                        startDate = startDate,
                        endDate = endDate,
                        note = note,
                        isActive = isActive
                    )
                } else {
                    viewModel.updateRecurringTransaction(
                        existing.copy(
                            title = title,
                            amount = amount,
                            type = type,
                            categoryId = categoryId,
                            accountId = accountId,
                            frequency = frequency,
                            startDate = startDate,
                            endDate = endDate,
                            nextOccurrence = startDate,
                            isActive = isActive,
                            note = note?.trim()?.takeIf { it.isNotEmpty() }
                        )
                    )
                }
                showSheet = false
                editingRule = null
            },
            onDelete = editingRule?.let { rule ->
                { viewModel.deleteRecurringTransaction(rule) }
            }
        )
    }
}

@Composable
private fun RecurringSummaryCard(
    overview: RecurringTransactionOverview,
    currencySymbol: String,
    rules: List<RecurringTransactionEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Recurring Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryStatCard(
                    title = "Total",
                    value = overview.totalRules.toString(),
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    title = "Active",
                    value = overview.activeRules.toString(),
                    accentColor = GoogleGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryStatCard(
                    title = "Paused",
                    value = overview.pausedRules.toString(),
                    accentColor = GoogleRed,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatCard(
                    title = "Due Soon",
                    value = overview.dueSoonRules.toString(),
                    accentColor = GoogleYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val nextDue = overview.nextDueRule
            if (nextDue != null) {
                val nextDueLabel = formatDate(nextDue.nextOccurrence)
                val color = if (!nextDue.isActive) MaterialTheme.colorScheme.onSurfaceVariant else when {
                    isOverdue(nextDue.nextOccurrence) -> GoogleRed
                    isDueToday(nextDue.nextOccurrence) -> GoogleYellow
                    else -> MaterialTheme.colorScheme.primary
                }

                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next due: ${nextDue.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$nextDueLabel • ${nextDue.frequency.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (nextDue.type == TransactionType.INCOME) "+$currencySymbol${formatAmount(nextDue.amount)}" else "-$currencySymbol${formatAmount(nextDue.amount)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            } else {
                Text(
                    text = if (rules.isEmpty()) "No recurring rules yet." else "No active rule is scheduled next.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun RecurringRuleCard(
    rule: RecurringTransactionEntity,
    currencySymbol: String,
    categories: List<com.spendwise.app.data.local.entity.CategoryEntity>,
    accounts: List<com.spendwise.app.data.local.entity.AccountEntity>,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val category = categories.firstOrNull { it.id == rule.categoryId }
    val account = accounts.firstOrNull { it.id == rule.accountId }
    val categoryColor = category?.let { CategoryIconHelper.parseColor(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val categoryIcon = category?.let { CategoryIconHelper.getIcon(it.iconName) } ?: Icons.Default.Schedule
    val amountColor = when (rule.type) {
        TransactionType.EXPENSE -> GoogleRed
        TransactionType.INCOME -> GoogleGreen
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (rule.type == TransactionType.INCOME) "+$currencySymbol${formatAmount(rule.amount)}" else "-$currencySymbol${formatAmount(rule.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (rule.isActive) "Active" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.isActive) GoogleGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Switch(
                        checked = rule.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${rule.frequency.displayName} • Next ${formatDate(rule.nextOccurrence)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (rule.endDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ends ${formatDate(rule.endDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val dueText = when {
                !rule.isActive -> "Paused"
                isOverdue(rule.nextOccurrence) -> "Overdue"
                isDueToday(rule.nextOccurrence) -> "Due today"
                else -> {
                    val days = daysUntil(rule.nextOccurrence)
                    if (days <= 7) "Due in $days day${if (days == 1L) "" else "s"}" else "Upcoming"
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        !rule.isActive -> MaterialTheme.colorScheme.onSurfaceVariant
                        isOverdue(rule.nextOccurrence) -> GoogleRed
                        isDueToday(rule.nextOccurrence) -> GoogleYellow
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit recurring rule",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete recurring rule",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (account != null || category != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = listOfNotNull(category?.name, account?.name).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String = String.format(Locale.US, "%,.2f", amount)

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun daysUntil(timestamp: Long): Long {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
    val target = java.time.Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    return ChronoUnit.DAYS.between(today, target).coerceAtLeast(0L)
}

private fun isOverdue(timestamp: Long): Boolean = timestamp < System.currentTimeMillis() && !isDueToday(timestamp)

private fun isDueToday(timestamp: Long): Boolean {
    val todayStart = startOfDay(System.currentTimeMillis())
    val tomorrowStart = todayStart + TimeUnit.DAYS.toMillis(1)
    return timestamp in todayStart until tomorrowStart
}

private fun startOfDay(timestamp: Long): Long {
    val zoneId = java.time.ZoneId.systemDefault()
    val date = java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
    return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
}
