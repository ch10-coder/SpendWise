package com.spendwise.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TagEntity
import com.spendwise.app.model.TransactionDraft
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import com.spendwise.app.ui.theme.GoogleGreen
import com.spendwise.app.ui.theme.GoogleRed
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    initialItem: TransactionWithCategory?,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (TransactionDraft) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    accounts: List<AccountEntity> = emptyList(),
    defaultAccountId: Long? = null,
    tags: List<TagEntity> = emptyList(),
    initialTagIds: Set<Long> = emptySet(),
    initialType: TransactionType = TransactionType.EXPENSE
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(initialItem?.transaction?.type ?: initialType) }
    var amountText by remember { mutableStateOf(initialItem?.transaction?.amount?.let { if (it > 0) String.format(Locale.US, "%.2f", it) else "" } ?: "") }
    var title by remember { mutableStateOf(initialItem?.transaction?.title ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialItem?.transaction?.categoryId) }
    var dateTimestamp by remember { mutableLongStateOf(initialItem?.transaction?.dateTimestamp ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(initialItem?.transaction?.note ?: "") }
    var accountId by remember { mutableStateOf(if (initialItem != null) initialItem.transaction.accountId else defaultAccountId) }
    var toAccountId by remember { mutableStateOf(initialItem?.transaction?.toAccountId) }
    var selectedTagIds by remember { mutableStateOf(initialTagIds) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == type }
    val canTransfer = accounts.size >= 2 || type == TransactionType.TRANSFER
    val typeOptions = if (canTransfer) {
        listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
    } else {
        listOf(TransactionType.EXPENSE, TransactionType.INCOME)
    }
    val actionColor = when (type) {
        TransactionType.EXPENSE -> GoogleRed
        TransactionType.INCOME -> GoogleGreen
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
    }
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    // Update selected category if type changes and current is invalid
    LaunchedEffect(type) {
        if (selectedCategoryId != null && filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialItem == null) "New Transaction" else "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (initialItem != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initialItem.transaction.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = GoogleRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Transaction Type Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                typeOptions.forEachIndexed { index, option ->
                    val optionColor = when (option) {
                        TransactionType.EXPENSE -> GoogleRed
                        TransactionType.INCOME -> GoogleGreen
                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                    }
                    SegmentedButton(
                        selected = type == option,
                        onClick = {
                            type = option
                            errorMessage = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = typeOptions.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = optionColor.copy(alpha = 0.15f),
                            activeContentColor = optionColor,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = when (option) {
                                TransactionType.EXPENSE -> "Expense"
                                TransactionType.INCOME -> "Income"
                                TransactionType.TRANSFER -> "Transfer"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val cleaned = input.replace(",", "")
                    if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                        amountText = cleaned
                        errorMessage = null
                    }
                },
                label = { Text("Amount") },
                prefix = { Text(currencySymbol, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMessage = null },
                label = { Text(if (type == TransactionType.TRANSFER) "Title (Optional)" else "Title / Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Account Selector
            if (accounts.isNotEmpty()) {
                if (type == TransactionType.TRANSFER) {
                    AccountChipRow(
                        label = "From Account",
                        accounts = accounts,
                        selectedAccountId = accountId,
                        disabledAccountId = toAccountId,
                        onAccountClick = { accountId = it; errorMessage = null }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AccountChipRow(
                        label = "To Account",
                        accounts = accounts,
                        selectedAccountId = toAccountId,
                        disabledAccountId = accountId,
                        onAccountClick = { toAccountId = it; errorMessage = null }
                    )
                } else {
                    AccountChipRow(
                        label = "Account",
                        accounts = accounts,
                        selectedAccountId = accountId,
                        disabledAccountId = null,
                        // Tapping the selected account again leaves the transaction unassigned
                        onAccountClick = { accountId = if (accountId == it) null else it }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category Selector (transfers move money between your own accounts, so they have no category)
            if (type != TransactionType.TRANSFER) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCategories, key = { it.id }) { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            val catColor = CategoryIconHelper.parseColor(cat.colorHex)

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = if (isSelected) null else cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                        contentDescription = cat.name,
                                        tint = if (isSelected) Color.White else catColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tag Selector
            if (tags.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags, key = { it.id }) { tag ->
                            val isSelected = tag.id in selectedTagIds
                            val tagColor = CategoryIconHelper.parseColor(tag.colorHex)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTagIds = if (isSelected) selectedTagIds - tag.id else selectedTagIds + tag.id
                                },
                                label = { Text(tag.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tagColor.copy(alpha = 0.2f),
                                    selectedLabelColor = tagColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Date Picker Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatter.format(Date(dateTimestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Time Picker Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable { showTimePicker = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Select Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeFormatter.format(Date(dateTimestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0) {
                        errorMessage = "Please enter a valid amount greater than 0"
                        return@Button
                    }

                    val isTransfer = type == TransactionType.TRANSFER
                    val fromAccount = accounts.firstOrNull { it.id == accountId }
                    val toAccount = accounts.firstOrNull { it.id == toAccountId }
                    if (isTransfer) {
                        if (fromAccount == null || toAccount == null) {
                            errorMessage = "Please choose both the From and To accounts"
                            return@Button
                        }
                        if (fromAccount.id == toAccount.id) {
                            errorMessage = "From and To accounts must be different"
                            return@Button
                        }
                    }

                    val finalTitle = title.trim().ifBlank {
                        if (isTransfer) "Transfer: ${fromAccount?.name} → ${toAccount?.name}" else ""
                    }
                    if (finalTitle.isBlank()) {
                        errorMessage = "Please enter a title or description"
                        return@Button
                    }

                    onSave(
                        TransactionDraft(
                            title = finalTitle,
                            amount = parsedAmount,
                            type = type,
                            categoryId = if (isTransfer) null else selectedCategoryId,
                            dateTimestamp = dateTimestamp,
                            note = note.trim().ifBlank { null },
                            accountId = accountId,
                            toAccountId = if (isTransfer) toAccountId else null,
                            tagIds = selectedTagIds.filter { id -> tags.any { it.id == id } }
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionColor
                )
            ) {
                Text(
                    text = if (initialItem == null) "Add Transaction" else "Update Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                            val localTime = Instant.ofEpochMilli(dateTimestamp).atZone(ZoneId.systemDefault()).toLocalTime()
                            val localDateTime = LocalDateTime.of(utcDate, localTime)
                            dateTimestamp = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val localTime = Instant.ofEpochMilli(dateTimestamp).atZone(ZoneId.systemDefault()).toLocalTime()
        val timePickerState = rememberTimePickerState(
            initialHour = localTime.hour,
            initialMinute = localTime.minute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Select Time", fontWeight = FontWeight.Bold) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val localDate = Instant.ofEpochMilli(dateTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        val newLocalTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val localDateTime = LocalDateTime.of(localDate, newLocalTime)
                        dateTimestamp = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountChipRow(
    label: String,
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    disabledAccountId: Long?,
    onAccountClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts, key = { it.id }) { acc ->
                val isSelected = acc.id == selectedAccountId
                val accColor = CategoryIconHelper.parseColor(acc.colorHex)

                FilterChip(
                    selected = isSelected,
                    onClick = { onAccountClick(acc.id) },
                    enabled = acc.id != disabledAccountId,
                    label = { Text(acc.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(acc.iconName),
                            contentDescription = acc.name,
                            tint = if (isSelected) Color.White else accColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accColor,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}
