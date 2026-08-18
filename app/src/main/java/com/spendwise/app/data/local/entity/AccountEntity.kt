package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spendwise.app.model.AccountType

/**
 * Represents a financial account (Cash, Bank, Credit Card, etc.)
 * Each account tracks its own balance independently.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val iconName: String = "AccountBalanceWallet",
    val colorHex: String = "#10B981",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT_ACCOUNTS = listOf(
            AccountEntity(1, "Cash", AccountType.CASH, 0.0, "Payments", "#10B981", true),
            AccountEntity(2, "Bank Account", AccountType.BANK, 0.0, "AccountBalance", "#3B82F6", false),
            AccountEntity(3, "Credit Card", AccountType.CREDIT_CARD, 0.0, "CreditCard", "#EF4444", false)
        )
    }
}
