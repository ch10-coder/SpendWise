package com.spendwise.app.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.model.AccountType
import com.spendwise.app.model.AccountWithBalance
import com.spendwise.app.ui.components.CategoryIconHelper
import com.spendwise.app.ui.components.ConfirmDeleteDialog
import com.spendwise.app.ui.theme.GoogleGreen
import com.spendwise.app.ui.theme.GoogleRed
import com.spendwise.app.ui.theme.HeroGradientDark
import com.spendwise.app.ui.theme.HeroGradientLight
import com.spendwise.app.util.IndianCurrencyFormatter
import com.spendwise.app.viewmodel.AccountsViewModel
import java.util.Locale

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()

    AccountsScreen(
        accounts = accounts,
        currencySymbol = currencySymbol,
        isBalanceHidden = isBalanceHidden,
        onSaveAccount = viewModel::saveAccount,
        onDeleteAccount = viewModel::deleteAccount,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accounts: List<AccountWithBalance>,
    currencySymbol: String,
    isBalanceHidden: Boolean,
    onSaveAccount: (
        existing: AccountEntity?,
        name: String,
        type: AccountType,
        iconName: String,
        colorHex: String,
        openingBalance: Double,
        isDefault: Boolean
    ) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAccount = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "net_worth") {
                NetWorthCard(
                    netWorth = accounts.sumOf { it.calculatedBalance },
                    accountCount = accounts.size,
                    currencySymbol = currencySymbol,
                    isBalanceHidden = isBalanceHidden
                )
            }

            item(key = "accounts_header") {
                Text(
                    text = "Your Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                Text(
                    text = "Tap an account to edit it or set its opening balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(accounts, key = { it.account.id }) { item ->
                AccountCard(
                    item = item,
                    currencySymbol = currencySymbol,
                    isBalanceHidden = isBalanceHidden,
                    onClick = {
                        editingAccount = item.account
                        showDialog = true
                    },
                    onDelete = { pendingDelete = item.account }
                )
            }
        }
    }

    if (showDialog) {
        AddEditAccountDialog(
            initialAccount = editingAccount,
            onDismiss = { showDialog = false },
            onSave = { name, type, icon, color, openingBalance, isDefault ->
                onSaveAccount(editingAccount, name, type, icon, color, openingBalance, isDefault)
                showDialog = false
            }
        )
    }

    pendingDelete?.let { account ->
        ConfirmDeleteDialog(
            title = "Delete Account",
            message = "Delete \"${account.name}\"? Its transactions will be kept, but they will no longer be linked to any account.",
            onConfirm = { onDeleteAccount(account) },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun NetWorthCard(
    netWorth: Double,
    accountCount: Int,
    currencySymbol: String,
    isBalanceHidden: Boolean
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val brush = remember(isDark) { Brush.linearGradient(if (isDark) HeroGradientDark else HeroGradientLight) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(20.dp)
        ) {
            Text(
                text = "Net worth across $accountCount ${if (accountCount == 1) "account" else "accounts"}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isBalanceHidden) "••••••" else IndianCurrencyFormatter.formatInr(netWorth, currencySymbol),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, letterSpacing = (-0.6).sp),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCard(
    item: AccountWithBalance,
    currencySymbol: String,
    isBalanceHidden: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val accountColor = CategoryIconHelper.parseColor(item.account.colorHex)
    val accountIcon = CategoryIconHelper.getIcon(item.account.iconName)

    val balanceColor = if (item.calculatedBalance < 0 && !isBalanceHidden) GoogleRed else if (item.calculatedBalance > 0 && !isBalanceHidden) GoogleGreen else MaterialTheme.colorScheme.onSurface
    val balanceText = if (isBalanceHidden) "••••••" else IndianCurrencyFormatter.formatInr(item.calculatedBalance, currencySymbol)

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.6f },
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                // Ask first; the card springs back and the confirmation dialog decides
                onDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSwiping) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accountColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = accountIcon,
                            contentDescription = item.account.name,
                            tint = accountColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.account.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (item.account.isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Default account",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = item.account.type.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = balanceText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    initialAccount: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, iconName: String, colorHex: String, openingBalance: Double, isDefault: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialAccount?.type ?: AccountType.BANK) }
    var expanded by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableStateOf(initialAccount?.iconName ?: "AccountBalance") }
    var selectedColor by remember { mutableStateOf(initialAccount?.colorHex ?: "#0B57D0") }
    var openingBalanceText by remember {
        mutableStateOf(
            initialAccount?.balance?.takeIf { it != 0.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }
    var isDefault by remember { mutableStateOf(initialAccount?.isDefault ?: false) }

    val presetColors = listOf("#B3261E", "#E29900", "#146C2E", "#0B57D0", "#6750A4", "#00677F")
    val presetIcons = listOf("AccountBalance", "AccountBalanceWallet", "Payments", "CreditCard", "Savings", "QrCode")
    val openingBalance = openingBalanceText.toDoubleOrNull() ?: 0.0
    val isOpeningBalanceValid = openingBalanceText.isEmpty() || openingBalanceText == "-" || openingBalanceText.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (initialAccount == null) "Add Account" else "Edit Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AccountType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = openingBalanceText,
                    onValueChange = { input ->
                        val cleaned = input.replace(",", "")
                        if (cleaned.isEmpty() || cleaned.matches(Regex("^-?\\d*(\\.\\d{0,2})?$"))) {
                            openingBalanceText = cleaned
                        }
                    },
                    label = { Text("Opening Balance") },
                    supportingText = { Text("Money already in this account before you started tracking. Use a minus for card dues.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetIcons.forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(iconName),
                                contentDescription = iconName,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CategoryIconHelper.parseColor(colorHex))
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Default account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Pre-selected when you add a transaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && isOpeningBalanceValid) {
                        onSave(name.trim(), selectedType, selectedIcon, selectedColor, openingBalance, isDefault)
                    }
                },
                enabled = name.isNotBlank() && isOpeningBalanceValid,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (initialAccount == null) "Save Account" else "Update Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
