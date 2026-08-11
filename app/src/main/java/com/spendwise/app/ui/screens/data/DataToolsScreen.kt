package com.spendwise.app.ui.screens.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.app.ui.theme.Emerald500
import com.spendwise.app.ui.theme.Rose500
import com.spendwise.app.viewmodel.DataToolState
import com.spendwise.app.viewmodel.DataToolsViewModel

@Composable
fun DataToolsScreen(
    viewModel: DataToolsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    // CSV Exporter File Picker Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            if (outputStream != null) {
                viewModel.exportToCsv(outputStream)
            }
        }
    }

    // CSV Importer File Picker Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            if (inputStream != null) {
                viewModel.importFromCsv(inputStream)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Alerts
        item {
            val currentState = if (exportState !is DataToolState.Idle) exportState else importState

            when (currentState) {
                is DataToolState.Processing -> {
                    DataStatusCard(
                        title = "Processing...",
                        message = "Working with your CSV transaction data.",
                        icon = Icons.Default.FileDownload,
                        tint = MaterialTheme.colorScheme.primary,
                        showProgress = true
                    )
                }
                is DataToolState.Success -> {
                    DataStatusCard(
                        title = "Success",
                        message = currentState.message,
                        icon = Icons.Default.CheckCircle,
                        tint = Emerald500
                    )
                }
                is DataToolState.Error -> {
                    DataStatusCard(
                        title = "Error",
                        message = currentState.errorMessage,
                        icon = Icons.Default.Error,
                        tint = Rose500
                    )
                }
                else -> {}
            }
        }

        // Export Card
        item {
            DataActionCard(
                title = "Export to CSV",
                description = "Download a CSV backup containing all your transactions, dates, amounts, categories, and notes.",
                buttonText = "Export Transactions",
                icon = Icons.Default.FileUpload,
                accentColor = Emerald500,
                onClick = {
                    val defaultFileName = "SpendWise_Transactions_${System.currentTimeMillis()}.csv"
                    exportLauncher.launch(defaultFileName)
                }
            )
        }

        // Import Card
        item {
            DataActionCard(
                title = "Import from CSV",
                description = "Restore your transactions from a previously exported CSV file or formatted spreadsheet.",
                buttonText = "Import Transactions",
                icon = Icons.Default.FileDownload,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "application/csv", "*/*"))
                }
            )
        }

        // Danger Zone / Clear Data Card
        item {
            DataActionCard(
                title = "Clear All Transactions",
                description = "Permanently delete all logged transactions and reset your balance to zero. This action cannot be undone.",
                buttonText = "Clear All Data",
                icon = Icons.Default.DeleteForever,
                accentColor = Rose500,
                isDestructive = true,
                onClick = { showClearDialog = true }
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete All Transactions?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all your transactions and reset all balance summaries. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DataActionCard(
    title: String,
    description: String,
    buttonText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isDestructive) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DataStatusCard(
    title: String,
    message: String,
    icon: ImageVector,
    tint: Color,
    showProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = tint
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
