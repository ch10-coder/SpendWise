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

/** Visual theme color palettes */
enum class ThemePalette(
    val displayName: String,
    val primaryColorHex: String,
    val description: String
) {
    EMERALD("Emerald Wealth", "#10B981", "Classic money green and fresh accents"),
    OCEAN("Ocean Sapphire", "#2563EB", "Deep calming blues and crisp slate"),
    AMETHYST("Midnight Amethyst", "#8B5CF6", "Vibrant purple and neon violet"),
    SUNSET("Golden Sunset", "#F59E0B", "Warm amber and bright gold tones"),
    ROSE("Crimson Rose", "#E11D48", "Bold ruby red and warm coral"),
    NORDIC("Nordic Teal", "#0D9488", "Clean scandinavian teal and forest accents"),
    CYBERPUNK("Cyber Cyan", "#06B6D4", "High-tech electric cyan and deep carbon")
}

/** Account types for multi-account support */
enum class AccountType(val displayName: String) {
    UPI("UPI (GPay / PhonePe / Paytm)"),
    CASH("Cash in Wallet"),
    BANK("Bank Account"),
    CREDIT_CARD("Credit Card"),
    SAVINGS("Savings / FD"),
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
            CurrencyOption("INR", "₹", "Indian Rupee"),
            CurrencyOption("USD", "$", "US Dollar"),
            CurrencyOption("EUR", "€", "Euro"),
            CurrencyOption("GBP", "£", "British Pound"),
            CurrencyOption("JPY", "¥", "Japanese Yen"),
            CurrencyOption("CAD", "CA$", "Canadian Dollar"),
            CurrencyOption("AUD", "AU$", "Australian Dollar"),
            CurrencyOption("AED", "د.إ", "UAE Dirham"),
            CurrencyOption("SGD", "S$", "Singapore Dollar")
        )
        val DEFAULT = ALL[0] // Indian Rupee (INR) is default!
    }
}
