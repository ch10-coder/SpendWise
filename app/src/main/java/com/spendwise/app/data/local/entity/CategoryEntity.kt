package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spendwise.app.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            // Expense Categories
            CategoryEntity(1, "Food & Dining", TransactionType.EXPENSE, "Restaurant", "#EF4444", true),
            CategoryEntity(2, "Groceries", TransactionType.EXPENSE, "ShoppingCart", "#F97316", true),
            CategoryEntity(3, "Transport", TransactionType.EXPENSE, "DirectionsCar", "#3B82F6", true),
            CategoryEntity(4, "Housing & Rent", TransactionType.EXPENSE, "Home", "#8B5CF6", true),
            CategoryEntity(5, "Utilities & Bills", TransactionType.EXPENSE, "Bolt", "#EAB308", true),
            CategoryEntity(6, "Shopping", TransactionType.EXPENSE, "ShoppingBag", "#EC4899", true),
            CategoryEntity(7, "Entertainment", TransactionType.EXPENSE, "Movie", "#6366F1", true),
            CategoryEntity(8, "Health & Medical", TransactionType.EXPENSE, "LocalHospital", "#14B8A6", true),
            CategoryEntity(9, "Education", TransactionType.EXPENSE, "School", "#06B6D4", true),
            CategoryEntity(10, "Travel", TransactionType.EXPENSE, "Flight", "#0EA5E9", true),

            // Income Categories
            CategoryEntity(11, "Salary", TransactionType.INCOME, "AccountBalanceWallet", "#10B981", true),
            CategoryEntity(12, "Freelance & Consulting", TransactionType.INCOME, "Laptop", "#22C55E", true),
            CategoryEntity(13, "Investments", TransactionType.INCOME, "TrendingUp", "#16A34A", true),
            CategoryEntity(14, "Gifts & Grants", TransactionType.INCOME, "CardGiftcard", "#059669", true),
            CategoryEntity(15, "Other Income", TransactionType.INCOME, "Payments", "#15803D", true)
        )
    }
}
