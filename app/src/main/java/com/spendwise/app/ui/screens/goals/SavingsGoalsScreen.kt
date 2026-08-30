package com.spendwise.app.ui.screens.goals

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.data.local.entity.SavingsGoalEntity
import com.spendwise.app.ui.components.CategoryIconHelper
import com.spendwise.app.ui.components.EmptyState
import com.spendwise.app.ui.theme.GoogleGreen
import com.spendwise.app.ui.theme.GoogleRed
import com.spendwise.app.viewmodel.SavingsGoalsViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsGoalsViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.goals.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsState()

    SavingsGoalsScreen(
        goals = goals,
        currencySymbol = currencySymbol,
        isBalanceHidden = isBalanceHidden,
        onAddGoal = viewModel::addGoal,
        onUpdateGoal = viewModel::updateGoal,
        onDeleteGoal = viewModel::deleteGoal,
        onDeposit = viewModel::deposit,
        onWithdraw = viewModel::withdraw,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    goals: List<SavingsGoalEntity>,
    currencySymbol: String,
    isBalanceHidden: Boolean,
    onAddGoal: (name: String, targetAmount: Double, currentAmount: Double, deadline: Long?, iconName: String, colorHex: String) -> Unit,
    onUpdateGoal: (SavingsGoalEntity) -> Unit,
    onDeleteGoal: (SavingsGoalEntity) -> Unit,
    onDeposit: (goalId: Long, amount: Double) -> Unit,
    onWithdraw: (goalId: Long, amount: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var adjustActionGoal by remember { mutableStateOf<Pair<SavingsGoalEntity, Boolean>?>(null) }

    val totalSaved = goals.sumOf { it.currentAmount }
    val totalTarget = goals.sumOf { it.targetAmount }
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Savings Goal")
            }
        }
    ) { paddingValues ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No Savings Goals Yet",
                    description = "Set financial targets for vacations, emergency funds, or big purchases to stay motivated.",
                    actionButtonText = "Create Goal",
                    onActionClick = { showAddDialog = true }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
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
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Savings Progress",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(overallProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val animatedOverall by animateFloatAsState(
                                targetValue = overallProgress,
                                animationSpec = tween(durationMillis = 800),
                                label = "overall_progress"
                            )

                            LinearProgressIndicator(
                                progress = { animatedOverall },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Current Saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val savedStr = if (isBalanceHidden) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", totalSaved)}"
                                    Text(
                                        text = savedStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoogleGreen
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val targetStr = if (isBalanceHidden) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", totalTarget)}"
                                    Text(
                                        text = targetStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Individual Goals
                items(goals, key = { it.id }) { goal ->
                    SavingsGoalItem(
                        goal = goal,
                        currencySymbol = currencySymbol,
                        isBalanceHidden = isBalanceHidden,
                        onDepositClick = { adjustActionGoal = Pair(goal, true) },
                        onWithdrawClick = { adjustActionGoal = Pair(goal, false) },
                        onEditClick = { goalToEdit = goal },
                        onDeleteClick = { onDeleteGoal(goal) }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || goalToEdit != null) {
        AddEditSavingsGoalDialog(
            initialGoal = goalToEdit,
            currencySymbol = currencySymbol,
            onDismiss = {
                showAddDialog = false
                goalToEdit = null
            },
            onSave = { name, targetAmount, currentAmount, deadline, iconName, colorHex ->
                if (goalToEdit == null) {
                    onAddGoal(name, targetAmount, currentAmount, deadline, iconName, colorHex)
                } else {
                    onUpdateGoal(
                        goalToEdit!!.copy(
                            name = name,
                            targetAmount = targetAmount,
                            currentAmount = currentAmount,
                            deadline = deadline,
                            iconName = iconName,
                            colorHex = colorHex
                        )
                    )
                }
                showAddDialog = false
                goalToEdit = null
            }
        )
    }

    // Deposit / Withdraw Dialog
    adjustActionGoal?.let { (goal, isDeposit) ->
        DepositWithdrawDialog(
            goal = goal,
            isDeposit = isDeposit,
            currencySymbol = currencySymbol,
            onDismiss = { adjustActionGoal = null },
            onConfirm = { amount ->
                if (isDeposit) {
                    onDeposit(goal.id, amount)
                } else {
                    onWithdraw(goal.id, amount)
                }
                adjustActionGoal = null
            }
        )
    }
}

@Composable
fun SavingsGoalItem(
    goal: SavingsGoalEntity,
    currencySymbol: String,
    isBalanceHidden: Boolean,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isCompleted = goal.currentAmount >= goal.targetAmount
    val goalColor = CategoryIconHelper.parseColor(goal.colorHex, defaultColor = MaterialTheme.colorScheme.primary)
    val goalIcon = CategoryIconHelper.getIcon(goal.iconName)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "goal_item_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
            // Header with Icon, Title, Deadline & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(goalColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else goalIcon,
                        contentDescription = goal.name,
                        tint = goalColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = GoogleGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Reached! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoogleGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    goal.deadline?.let { deadlineEpoch ->
                        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        val deadlineDate = Date(deadlineEpoch)
                        val daysRemaining = ChronoUnit.DAYS.between(
                            LocalDate.now(),
                            Instant.ofEpochMilli(deadlineEpoch).atZone(ZoneId.systemDefault()).toLocalDate()
                        )

                        val deadlineLabel = when {
                            daysRemaining < 0 -> "Target passed (${dateFormatter.format(deadlineDate)})"
                            daysRemaining == 0L -> "Target is today!"
                            daysRemaining < 30 -> "$daysRemaining days left"
                            else -> "${daysRemaining / 30} months left"
                        }

                        Text(
                            text = deadlineLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (daysRemaining < 0) GoogleRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Edit & Delete actions
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Goal",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = goalColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentStr = if (isBalanceHidden) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", goal.currentAmount)}"
                val targetStr = if (isBalanceHidden) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", goal.targetAmount)}"

                Text(
                    text = "$currentStr of $targetStr",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = goalColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Deposit & Withdraw Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Withdraw", fontSize = 13.sp)
                }

                Button(
                    onClick = onDepositClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goalColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deposit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSavingsGoalDialog(
    initialGoal: SavingsGoalEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: Double, currentAmount: Double, deadline: Long?, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialGoal?.name ?: "") }
    var targetAmountText by remember { mutableStateOf(initialGoal?.targetAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var currentAmountText by remember { mutableStateOf(initialGoal?.currentAmount?.let { if (it > 0) String.format(Locale.US, "%.2f", it) else "" } ?: "") }
    var deadlineTimestamp by remember { mutableStateOf(initialGoal?.deadline) }
    var selectedIcon by remember { mutableStateOf(initialGoal?.iconName ?: "Savings") }
    var selectedColor by remember { mutableStateOf(initialGoal?.colorHex ?: "#146C2E") }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetColors = listOf(
        "#146C2E", "#0B57D0", "#6750A4", "#00677F",
        "#984061", "#B3261E", "#7D5700", "#006A6A"
    )

    val presetIcons = listOf(
        "Savings", "AccountBalance", "DirectionsCar", "Home",
        "Flight", "Laptop", "Celebration", "Star", "Favorite"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (initialGoal == null) "Create Savings Goal" else "Edit Goal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. Vacation, Car)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = { targetAmountText = it },
                    label = { Text("Target Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentAmountText,
                    onValueChange = { currentAmountText = it },
                    label = { Text("Initial Saved Amount (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Date Selector
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = deadlineTimestamp?.let {
                                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
                                } ?: "Set Target Date (Optional)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (deadlineTimestamp != null) {
                            TextButton(onClick = { deadlineTimestamp = null }) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Icon Picker Row
                Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetIcons) { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(iconName),
                                contentDescription = iconName,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Color Picker Row
                Text("Select Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors) { colorHex ->
                        val color = CategoryIconHelper.parseColor(colorHex)
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetAmountText.toDoubleOrNull()
                    val current = currentAmountText.toDoubleOrNull() ?: 0.0
                    if (name.isBlank()) {
                        errorMessage = "Please enter a goal name"
                    } else if (target == null || target <= 0) {
                        errorMessage = "Please enter a valid target amount"
                    } else {
                        onSave(name, target, current, deadlineTimestamp, selectedIcon, selectedColor)
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadlineTimestamp ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineTimestamp = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
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
}

@Composable
fun DepositWithdrawDialog(
    goal: SavingsGoalEntity,
    isDeposit: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (isDeposit) "Deposit to ${goal.name}" else "Withdraw from ${goal.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current saved: $currencySymbol${String.format(Locale.US, "%,.2f", goal.currentAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid amount"
                    } else if (!isDeposit && amount > goal.currentAmount) {
                        errorMessage = "Cannot withdraw more than current saved amount"
                    } else {
                        onConfirm(amount)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDeposit) GoogleGreen else GoogleRed
                )
            ) {
                Text(if (isDeposit) "Deposit" else "Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
