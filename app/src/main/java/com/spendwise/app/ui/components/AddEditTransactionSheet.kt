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
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import com.spendwise.app.ui.theme.Emerald500
import com.spendwise.app.ui.theme.Rose500
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
    onSave: (title: String, amount: Double, type: TransactionType, categoryId: Long?, dateTimestamp: Long, note: String?) -> Unit,
    onDelete: ((Long) -> Unit)? = null,
    accounts: List<AccountEntity> = emptyList(),
    selectedAccountId: Long? = null,
    onAccountSelected: (Long) -> Unit = {},
    tags: List<TagEntity> = emptyList(),
    selectedTagIds: Set<Long> = emptySet(),
    onTagToggled: (Long) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(initialItem?.transaction?.type ?: TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf(initialItem?.transaction?.amount?.let { if (it > 0) String.format(Locale.US, "%.2f", it) else "" } ?: "") }
    var title by remember { mutableStateOf(initialItem?.transaction?.title ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialItem?.transaction?.categoryId) }
    var dateTimestamp by remember { mutableLongStateOf(initialItem?.transaction?.dateTimestamp ?: System.currentTimeMillis()) }
    var note by remember { mutableStateOf(initialItem?.transaction?.note ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == type }
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
                    fontWeight = FontWeight.Bold
                )

                if (initialItem != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initialItem.transaction.id); onDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Rose500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Transaction Type Selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Rose500.copy(alpha = 0.15f),
                        activeContentColor = Rose500,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Expense", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Emerald500.copy(alpha = 0.15f),
                        activeContentColor = Emerald500,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Income", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                        amountText = input
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
                label = { Text("Title / Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Account Selector
            if (accounts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.labelMedium,
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
                                onClick = { onAccountSelected(acc.id) },
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
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Category Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
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
                            onClick = { selectedCategoryId = cat.id },
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
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Tag Selector
            if (tags.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags, key = { it.id }) { tag ->
                            val isSelected = selectedTagIds.contains(tag.id)
                            val tagColor = CategoryIconHelper.parseColor(tag.colorHex)

                            FilterChip(
                                selected = isSelected,
                                onClick = { onTagToggled(tag.id) },
                                label = { Text(tag.name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tagColor.copy(alpha = 0.2f),
                                    selectedLabelColor = tagColor
                                ),
                                shape = RoundedCornerShape(12.dp)
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                    if (title.isBlank()) {
                        errorMessage = "Please enter a title or description"
                        return@Button
                    }

                    onSave(
                        title.trim(),
                        parsedAmount,
                        type,
                        selectedCategoryId,
                        dateTimestamp,
                        note.trim().ifBlank { null }
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.EXPENSE) Rose500 else Emerald500
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
                            // Convert UTC millis to local date to avoid timezone shift
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
            title = { Text("Select Time") },
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
