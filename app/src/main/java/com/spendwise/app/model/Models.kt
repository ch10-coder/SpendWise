package com.spendwise.app.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
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
