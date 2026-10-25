package com.spendwise.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.ui.theme.HeroGradientDark
import com.spendwise.app.ui.theme.HeroGradientLight
import com.spendwise.app.ui.theme.HeroInflowTint
import com.spendwise.app.ui.theme.HeroOutflowTint
import com.spendwise.app.util.IndianCurrencyFormatter
import kotlin.math.roundToInt

/**
 * Hero cashflow card at the top of Home: net cashflow for the selected period,
 * a one-line savings insight, income vs spending, and one-tap quick actions.
 */
@Composable
fun SummaryCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier,
    periodLabel: String = "This Month",
    isBalanceHidden: Boolean = false,
    onToggleHidden: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddIncome: () -> Unit = {},
    onAddTransfer: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val heroBrush = remember(isDark) {
        Brush.linearGradient(if (isDark) HeroGradientDark else HeroGradientLight)
    }
    val onHero = Color.White
    val onHeroMuted = Color.White.copy(alpha = 0.72f)
    val glass = Color.White.copy(alpha = 0.12f)

    val totalFlow = totalIncome + totalExpense
    val incomeRatio = if (totalFlow > 0) (totalIncome / totalFlow).toFloat().coerceIn(0f, 1f) else 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .padding(20.dp)
        ) {
            // Period label & privacy toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = glass) {
                    Text(
                        text = "Net cashflow • $periodLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onHero,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = onToggleHidden,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(glass)
                ) {
                    Icon(
                        imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isBalanceHidden) "Show amounts" else "Hide amounts",
                        tint = onHero,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val balanceText = if (isBalanceHidden) "••••••" else IndianCurrencyFormatter.formatInr(totalBalance, currencySymbol)
            Text(
                text = balanceText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.8).sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = onHero,
                maxLines = 1
            )

            if (!isBalanceHidden) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cashflowInsight(totalIncome, totalExpense, currencySymbol),
                    style = MaterialTheme.typography.bodySmall,
                    color = onHeroMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income & spending
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetric(
                    title = "Income",
                    amount = totalIncome,
                    currencySymbol = currencySymbol,
                    icon = Icons.Default.ArrowDownward,
                    accent = HeroInflowTint,
                    isHidden = isBalanceHidden,
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    title = "Spent",
                    amount = totalExpense,
                    currencySymbol = currencySymbol,
                    icon = Icons.Default.ArrowUpward,
                    accent = HeroOutflowTint,
                    isHidden = isBalanceHidden,
                    modifier = Modifier.weight(1f)
                )
            }

            // Income vs spending split
            if (totalFlow > 0 && !isBalanceHidden) {
                Spacer(modifier = Modifier.height(14.dp))
                val animatedIncomeRatio by animateFloatAsState(
                    targetValue = incomeRatio,
                    animationSpec = tween(durationMillis = 600),
                    label = "cashflow_ratio"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(HeroOutflowTint.copy(alpha = 0.55f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedIncomeRatio)
                            .clip(CircleShape)
                            .background(HeroInflowTint)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroAction(
                    text = "Expense",
                    icon = Icons.Default.Remove,
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f)
                )
                HeroAction(
                    text = "Income",
                    icon = Icons.Default.Add,
                    onClick = onAddIncome,
                    modifier = Modifier.weight(1f)
                )
                if (onAddTransfer != null) {
                    HeroAction(
                        text = "Transfer",
                        icon = Icons.Default.SwapHoriz,
                        onClick = onAddTransfer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** A short plain-language read of the period: how much was saved, or how far spending went over. */
private fun cashflowInsight(income: Double, expense: Double, currencySymbol: String): String {
    return when {
        income <= 0 && expense <= 0 -> "No activity yet. Add your first entry below."
        income <= 0 -> "No income logged for this period yet"
        expense <= income -> {
            val savedPct = ((income - expense) / income * 100).roundToInt()
            "You kept $savedPct% of your income"
        }
        else -> "Spent ${IndianCurrencyFormatter.formatInr(expense - income, currencySymbol)} more than you earned"
    }
}

@Composable
private fun HeroMetric(
    title: String,
    amount: Double,
    currencySymbol: String,
    icon: ImageVector,
    accent: Color,
    isHidden: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    text = if (isHidden) "••••••" else IndianCurrencyFormatter.formatInr(amount, currencySymbol),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HeroAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1E1B4B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1B4B),
                maxLines = 1
            )
        }
    }
}
