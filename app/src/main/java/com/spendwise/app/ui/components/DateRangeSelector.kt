package com.spendwise.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendwise.app.model.DateRangeFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    selectedFilter: DateRangeFilter,
    onFilterSelected: (DateRangeFilter) -> Unit,
    customStartDate: Long? = null,
    customEndDate: Long? = null,
    onCustomRangeSelected: (Long, Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(DateRangeFilter.entries) { filter ->
            val isSelected = selectedFilter == filter
            
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (filter == DateRangeFilter.CUSTOM) {
                        showDatePicker = true
                    }
                    onFilterSelected(filter)
                },
                label = {
                    if (filter == DateRangeFilter.CUSTOM && isSelected && customStartDate != null && customEndDate != null) {
                        val format = SimpleDateFormat("MMM d", Locale.getDefault())
                        Text("${format.format(Date(customStartDate))} - ${format.format(Date(customEndDate))}")
                    } else {
                        Text(filter.displayName)
                    }
                },
                leadingIcon = if (filter == DateRangeFilter.CUSTOM) {
                    { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStartDate,
            initialSelectedEndDateMillis = customEndDate
        )
        
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                if (customStartDate == null || customEndDate == null) {
                    onFilterSelected(DateRangeFilter.THIS_MONTH)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = datePickerState.selectedStartDateMillis
                        val end = datePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onCustomRangeSelected(start, end)
                        } else {
                            onFilterSelected(DateRangeFilter.THIS_MONTH)
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedStartDateMillis != null && datePickerState.selectedEndDateMillis != null
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        if (customStartDate == null || customEndDate == null) {
                            onFilterSelected(DateRangeFilter.THIS_MONTH)
                        }
                    }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
