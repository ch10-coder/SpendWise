package com.spendwise.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.ui.theme.Emerald500
import com.spendwise.app.ui.theme.Rose500
import java.util.Locale

@Composable
fun SummaryCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false,
    onToggleHidden: () -> Unit = {}
) {
    val isPositive = totalBalance >= 0
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val cardGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    )

    val totalFlow = totalIncome + totalExpense
    val incomeRatio = if (totalFlow > 0) (totalIncome / totalFlow).toFloat().coerceIn(0f, 1f) else 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: Card Branding + Eye Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wallet,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Net Cash Balance",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onToggleHidden,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Balance Visibility",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Headline Balance
                val balanceColor = when {
                    isBalanceHidden -> MaterialTheme.colorScheme.onSurface
                    !isPositive -> Rose500
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val balanceText = if (isBalanceHidden) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", totalBalance)}"

                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = balanceColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Income & Expense Dual Metric Glass Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Metric Pill
                    FintechMetricPill(
                        title = "Cash Inflow",
                        amount = totalIncome,
                        currencySymbol = currencySymbol,
                        isIncome = true,
                        isBalanceHidden = isBalanceHidden,
                        modifier = Modifier.weight(1f)
                    )

                    // Expense Metric Pill
                    FintechMetricPill(
                        title = "Cash Outflow",
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
                        animationSpec = tween(durationMillis = 600),
                        label = "cashflow_ratio"
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Inflow ${(incomeRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Emerald500,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Outflow ${((1f - incomeRatio) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Rose500,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Rose500.copy(alpha = 0.35f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedIncomeRatio)
                                    .clip(CircleShape)
                                    .background(Emerald500)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FintechMetricPill(
    title: String,
    amount: Double,
    currencySymbol: String,
    isIncome: Boolean,
    isBalanceHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isIncome) Emerald500 else Rose500
    val icon = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
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
