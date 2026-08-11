package com.spendwise.app.model

import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity

data class TransactionWithCategory(
    val transaction: TransactionEntity,
    val category: CategoryEntity?
)

data class BudgetWithSpending(
    val budgetId: Long,
    val category: CategoryEntity,
    val limitAmount: Double,
    val spentAmount: Double,
    val monthYear: String
) {
    val remainingAmount: Double
        get() = limitAmount - spentAmount

    val progressFraction: Float
        get() = if (limitAmount > 0) (spentAmount / limitAmount).toFloat().coerceIn(0f, 2f) else 0f

    val isOverBudget: Boolean
        get() = spentAmount > limitAmount
}

data class CategorySpending(
    val category: CategoryEntity,
    val totalAmount: Double,
    val percentage: Float
)

data class MonthlyAnalytics(
    val monthYear: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val categorySpendings: List<CategorySpending>,
    val dailySpendings: List<DailySpending>
)

data class DailySpending(
    val dayOfMonth: Int,
    val totalExpense: Double,
    val totalIncome: Double
)
