package com.spendwise.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.ui.theme.GoogleBlue
import com.spendwise.app.ui.theme.GoogleGreen
import com.spendwise.app.ui.theme.GoogleGreenDark
import com.spendwise.app.ui.theme.GoogleRed
import com.spendwise.app.ui.theme.GoogleRedDark
import java.util.Locale

@Composable
fun SummaryCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false,
    onToggleHidden: () -> Unit = {},
    onQuickAddExpense: (() -> Unit)? = null,
    onQuickAddIncome: (() -> Unit)? = null
) {
    val isPositive = totalBalance >= 0
    val primaryColor = MaterialTheme.colorScheme.primary

    val totalFlow = totalIncome + totalExpense
    val incomeRatio = if (totalFlow > 0) (totalIncome / totalFlow).toFloat().coerceIn(0f, 1f) else 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Google Wallet Style Chip & Privacy Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Net Cash Balance",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                IconButton(
                    onClick = onToggleHidden,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Balance Visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Big Google Headline Balance
            val balanceColor = when {
                isBalanceHidden -> MaterialTheme.colorScheme.onSurface
                !isPositive -> GoogleRed
                else -> MaterialTheme.colorScheme.onSurface
            }
            val balanceText = if (isBalanceHidden) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", totalBalance)}"

            Text(
                text = balanceText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = balanceColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Income & Expense Metric Cards (Google Pastel Pill Layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoogleMetricPill(
                    title = "Inflow",
                    amount = totalIncome,
                    currencySymbol = currencySymbol,
                    isIncome = true,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier.weight(1f)
                )

                GoogleMetricPill(
                    title = "Outflow",
                    amount = totalExpense,
                    currencySymbol = currencySymbol,
                    isIncome = false,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier.weight(1f)
                )
            }

            // Cashflow Ratio Strip
            if (totalFlow > 0 && !isBalanceHidden) {
                Spacer(modifier = Modifier.height(16.dp))
                val animatedIncomeRatio by animateFloatAsState(
                    targetValue = incomeRatio,
                    animationSpec = tween(durationMillis = 700),
                    label = "cashflow_ratio"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Inflow ${(incomeRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = GoogleGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Outflow ${((1f - incomeRatio) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = GoogleRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(GoogleRed.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedIncomeRatio)
                                .clip(CircleShape)
                                .background(GoogleGreen)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleMetricPill(
    title: String,
    amount: Double,
    currencySymbol: String,
    isIncome: Boolean,
    isBalanceHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isIncome) GoogleGreen else GoogleRed
    val icon = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val amountText = if (isBalanceHidden) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", amount)}"
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}
