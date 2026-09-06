package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.spendwise.app.model.AccountType

/**
 * Represents a financial account (UPI, Cash, Bank, Credit Card, etc.)
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
    val colorHex: String = "#0052FF",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val DEFAULT_ACCOUNTS = listOf(
            AccountEntity(1, "UPI (GPay / PhonePe)", AccountType.UPI, 0.0, "QrCode", "#0052FF", true),
            AccountEntity(2, "Bank Account", AccountType.BANK, 0.0, "AccountBalance", "#0284C7", false),
            AccountEntity(3, "Cash in Wallet", AccountType.CASH, 0.0, "Payments", "#10B981", false),
            AccountEntity(4, "Credit Card", AccountType.CREDIT_CARD, 0.0, "CreditCard", "#EF4444", false)
        )
    }
}
