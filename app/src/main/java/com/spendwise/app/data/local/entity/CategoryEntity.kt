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
            // Indian Lifestyle Expense Categories
            CategoryEntity(1, "Kirana & Groceries", TransactionType.EXPENSE, "ShoppingCart", "#F97316", true),
            CategoryEntity(2, "Chai, Snacks & Dining", TransactionType.EXPENSE, "LocalCafe", "#EF4444", true),
            CategoryEntity(3, "Auto, Metro & Fuel", TransactionType.EXPENSE, "DirectionsCar", "#3B82F6", true),
            CategoryEntity(4, "Rent & Maintenance", TransactionType.EXPENSE, "Home", "#8B5CF6", true),
            CategoryEntity(5, "Electricity & Bills", TransactionType.EXPENSE, "Bolt", "#EAB308", true),
            CategoryEntity(6, "Shopping & E-Commerce", TransactionType.EXPENSE, "ShoppingBag", "#EC4899", true),
            CategoryEntity(7, "Movies & Subscriptions", TransactionType.EXPENSE, "Movie", "#6366F1", true),
            CategoryEntity(8, "Pharmacy & Medical", TransactionType.EXPENSE, "LocalHospital", "#14B8A6", true),
            CategoryEntity(9, "SIP & Mutual Funds", TransactionType.EXPENSE, "TrendingUp", "#10B981", true),
            CategoryEntity(10, "Pooja, Festivals & Gifts", TransactionType.EXPENSE, "Celebration", "#D97706", true),

            // Income Categories
            CategoryEntity(11, "Salary & Business Income", TransactionType.INCOME, "AccountBalanceWallet", "#10B981", true),
            CategoryEntity(12, "Freelance & Consulting", TransactionType.INCOME, "Laptop", "#22C55E", true),
            CategoryEntity(13, "Returns & Dividends", TransactionType.INCOME, "TrendingUp", "#16A34A", true),
            CategoryEntity(14, "Cashbacks & Rewards", TransactionType.INCOME, "CardGiftcard", "#059669", true),
            CategoryEntity(15, "Other Income", TransactionType.INCOME, "Payments", "#15803D", true)
        )
    }
}
