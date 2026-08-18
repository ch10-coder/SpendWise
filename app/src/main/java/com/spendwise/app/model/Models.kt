package com.spendwise.app.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** Account types for multi-account support */
enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    CREDIT_CARD("Credit Card"),
    SAVINGS("Savings"),
    DIGITAL_WALLET("Digital Wallet")
}

/** Frequency options for recurring/scheduled transactions */
enum class RecurrenceFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BIWEEKLY("Bi-weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String
) {
    companion object {
        val ALL = listOf(
            CurrencyOption("USD", "$", "US Dollar"),
            CurrencyOption("EUR", "€", "Euro"),
            CurrencyOption("GBP", "£", "British Pound"),
            CurrencyOption("INR", "₹", "Indian Rupee"),
            CurrencyOption("JPY", "¥", "Japanese Yen"),
            CurrencyOption("CAD", "CA$", "Canadian Dollar"),
            CurrencyOption("AUD", "AU$", "Australian Dollar"),
            CurrencyOption("BRL", "R$", "Brazilian Real"),
            CurrencyOption("NGN", "₦", "Nigerian Naira"),
            CurrencyOption("PHP", "₱", "Philippine Peso")
        )
        val DEFAULT = ALL[0]
    }
}
