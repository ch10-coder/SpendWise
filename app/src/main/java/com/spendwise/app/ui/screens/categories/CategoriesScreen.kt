package com.spendwise.app.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.model.TransactionType
import com.spendwise.app.ui.components.CategoryIconHelper
import com.spendwise.app.ui.theme.GoogleGreen
import com.spendwise.app.ui.theme.GoogleRed
import com.spendwise.app.viewmodel.CategoriesViewModel

private val PRESET_COLORS = listOf(
    "#B3261E", "#E29900", "#146C2E", "#0B57D0", "#6750A4",
    "#00677F", "#984061", "#4A6363", "#D93025", "#1E8E3E"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    modifier: Modifier = Modifier
) {
    val allCategories by viewModel.allCategories.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    val filteredCategories = allCategories.filter { it.type == selectedType }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Type Segmented Toggle
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { viewModel.onTypeSelected(TransactionType.EXPENSE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = GoogleRed.copy(alpha = 0.15f),
                            activeContentColor = GoogleRed,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Expense (${allCategories.count { it.type == TransactionType.EXPENSE }})", fontWeight = FontWeight.Bold)
                    }

                    SegmentedButton(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { viewModel.onTypeSelected(TransactionType.INCOME) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = GoogleGreen.copy(alpha = 0.15f),
                            activeContentColor = GoogleGreen,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Income (${allCategories.count { it.type == TransactionType.INCOME }})", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(filteredCategories, key = { it.id }) { category ->
                val catColor = CategoryIconHelper.parseColor(category.colorHex)
                val catIcon = CategoryIconHelper.getIcon(category.iconName)

                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = category.name,
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (category.isDefault) {
                                Text(
                                    text = "System Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                editingCategory = category
                                showDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Category",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!category.isDefault) {
                            IconButton(
                                onClick = { viewModel.deleteCategory(category) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Category",
                                    tint = GoogleRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddEditCategoryDialog(
            initialCategory = editingCategory,
            defaultType = selectedType,
            onDismiss = { showDialog = false },
            onSave = { name, type, iconName, colorHex ->
                if (editingCategory == null) {
                    viewModel.addCategory(name, type, iconName, colorHex)
                } else {
                    viewModel.updateCategory(
                        editingCategory!!.id,
                        name,
                        type,
                        iconName,
                        colorHex,
                        editingCategory!!.isDefault
                    )
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryDialog(
    initialCategory: CategoryEntity?,
    defaultType: TransactionType,
    onDismiss: () -> Unit,
    onSave: (name: String, type: TransactionType, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var type by remember { mutableStateOf(initialCategory?.type ?: defaultType) }
    var selectedIcon by remember { mutableStateOf(initialCategory?.iconName ?: "ShoppingCart") }
    var selectedColor by remember { mutableStateOf(initialCategory?.colorHex ?: PRESET_COLORS[0]) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val iconKeys = CategoryIconHelper.AVAILABLE_ICONS.keys.toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (initialCategory == null) "New Category" else "Edit Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Color Selector
                Text("Pick Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PRESET_COLORS) { hex ->
                        val color = CategoryIconHelper.parseColor(hex)
                        val isSelected = selectedColor == hex

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Icon Selector Grid
                Text("Pick Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(iconKeys) { iconKey ->
                        val isSelected = selectedIcon == iconKey
                        val currentTint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CategoryIconHelper.parseColor(selectedColor) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .clickable { selectedIcon = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(iconKey),
                                contentDescription = iconKey,
                                tint = currentTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Category name cannot be empty"
                        return@Button
                    }
                    onSave(name.trim(), type, selectedIcon, selectedColor)
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
