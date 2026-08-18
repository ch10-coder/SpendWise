package com.spendwise.app.model

import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TagEntity
import com.spendwise.app.data.local.entity.TransactionEntity

data class TransactionWithCategory(
    val transaction: TransactionEntity,
    val category: CategoryEntity?
)

/** Extended transaction with category, account, and tags info for display */
data class TransactionDisplayItem(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val account: AccountEntity?,
    val tags: List<TagEntity> = emptyList()
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

/** Summary of budget progress across all categories for a month */
data class OverallBudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val remaining: Double,
    val progressFraction: Float,
    val isOverBudget: Boolean
)

/** Account with its calculated balance from transactions */
data class AccountWithBalance(
    val account: AccountEntity,
    val calculatedBalance: Double
)

/** Tag with aggregated spending for analytics */
data class TagSpending(
    val tag: TagEntity,
    val totalAmount: Double,
    val percentage: Float
)

/** Date range filter options for the Timeline */
enum class DateRangeFilter(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom Range")
}
