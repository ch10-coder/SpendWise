package com.spendwise.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.app.model.CategorySpending
import com.spendwise.app.model.DailySpending
import com.spendwise.app.ui.theme.GoogleGreen
import com.spendwise.app.ui.theme.GoogleRed
import java.util.Locale
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle

@Composable
fun DonutPieChart(
    categorySpendings: List<CategorySpending>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categorySpendings) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(750))
    }

    // Pre-calculate target sweep angles based on exact percentages
    val targetAngles = remember(categorySpendings) {
        val percentages = categorySpendings.map { it.percentage }
        val sum = percentages.sum()
        if (sum == 0f) {
            categorySpendings.map { 0f }
        } else {
            percentages.map { (it / 100f) * 360f }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(190.dp)) {
                val strokeWidth = 34.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                if (categorySpendings.isEmpty() || totalExpense <= 0) {
                    // Empty grey ring
                    drawCircle(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.4f),
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var startAngle = -90f

                    categorySpendings.forEachIndexed { index, spending ->
                        val targetSweep = targetAngles[index]
                        val animatedSweep = targetSweep * animationProgress.value
                        val sliceColor = CategoryIconHelper.parseColor(spending.category.colorHex)

                        if (animatedSweep > 0f) {
                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = animatedSweep,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        }
                        startAngle += animatedSweep
                    }
                }
            }

            // Center text summary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalExpense)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Breakdown Legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categorySpendings.forEach { item ->
                val sliceColor = CategoryIconHelper.parseColor(item.category.colorHex)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(sliceColor)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", item.percentage),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", item.totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DailySpendingBarChart(
    dailySpendings: List<DailySpending>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val maxExpense = (dailySpendings.maxOfOrNull { it.totalExpense } ?: 0.0).coerceAtLeast(10.0)
    val textMeasurer = rememberTextMeasurer()
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(
        color = axisTextColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )

    val maxLabelText = remember(maxExpense, currencySymbol) {
        "$currencySymbol${String.format(Locale.US, "%,.0f", maxExpense)}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Daily Spending Trend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(136.dp)) {
                // Draw max amount label
                val labelLayoutResult = textMeasurer.measure(maxLabelText, style = labelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = maxLabelText,
                    style = labelStyle,
                    topLeft = Offset(0f, 0f)
                )

                val bottomPadding = 20.dp.toPx()
                val topPadding = labelLayoutResult.size.height.toFloat() + 4.dp.toPx()
                val chartHeight = size.height - bottomPadding - topPadding

                if (dailySpendings.isNotEmpty()) {
                    val barWidth = (size.width / dailySpendings.size).coerceAtMost(16.dp.toPx())
                    val spacing = (size.width - (barWidth * dailySpendings.size)) / (dailySpendings.size + 1)

                    dailySpendings.forEachIndexed { index, daily ->
                        val barHeight = ((daily.totalExpense / maxExpense) * chartHeight).toFloat()
                        val x = spacing + index * (barWidth + spacing)
                        val y = topPadding + (chartHeight - barHeight)

                        if (barHeight > 0) {
                            drawRoundRect(
                                color = GoogleRed.copy(alpha = 0.85f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        } else {
                            // Baseline tick
                            drawCircle(
                                color = Color(0xFF94A3B8).copy(alpha = 0.4f),
                                radius = 2.dp.toPx(),
                                center = Offset(x + barWidth / 2, topPadding + chartHeight - 2.dp.toPx())
                            )
                        }

                        // X-axis labels (days)
                        val day = daily.dayOfMonth
                        if (day == 1 || day == 5 || day == 10 || day == 15 || day == 20 || day == 25 || day == 30) {
                            val dayText = day.toString()
                            val dayLayout = textMeasurer.measure(dayText, style = labelStyle)
                            drawText(
                                textMeasurer = textMeasurer,
                                text = dayText,
                                style = labelStyle,
                                topLeft = Offset(
                                    x + (barWidth / 2) - (dayLayout.size.width / 2),
                                    topPadding + chartHeight + 4.dp.toPx()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
