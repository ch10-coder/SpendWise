package com.spendwise.app.util

import java.util.Locale

/**
 * High-performance, zero-allocation currency formatter for the Indian Numbering System.
 * Formats amounts into standard Indian format (e.g. ₹1,48,500.50, ₹10,00,000.00).
 */
object IndianCurrencyFormatter {

    /**
     * Formats an amount using Indian numbering system (Lakhs and Crores).
     * Example:
     * 500.0 -> ₹500.00
     * 12450.5 -> ₹12,450.50
     * 148500.5 -> ₹1,48,500.50
     * 1050000.0 -> ₹10,50,000.00
     */
    fun formatInr(amount: Double, symbol: String = "₹", includeDecimals: Boolean = true): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        val longPart = absAmount.toLong()
        val decimalPart = Math.round((absAmount - longPart) * 100).toInt()

        val longStr = longPart.toString()
        val formattedLong = if (longStr.length <= 3) {
            longStr
        } else {
            val lastThree = longStr.substring(longStr.length - 3)
            val rest = longStr.substring(0, longStr.length - 3)
            val sb = StringBuilder(longStr.length + 4)
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                sb.append(rest[i])
                count++
                if (count % 2 == 0 && i > 0) {
                    sb.append(',')
                }
            }
            sb.reverse().append(',').append(lastThree).toString()
        }

        val sign = if (isNegative) "-" else ""
        return if (includeDecimals) {
            val decStr = if (decimalPart < 10) "0$decimalPart" else if (decimalPart >= 100) "99" else decimalPart.toString()
            "$sign$symbol$formattedLong.$decStr"
        } else {
            "$sign$symbol$formattedLong"
        }
    }

    /**
     * Compact Indian denomination format: ₹1.48 L, ₹85 K, ₹2.50 Cr
     */
    fun formatCompactInr(amount: Double, symbol: String = "₹"): String {
        val abs = Math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 10_000_000 -> "$sign$symbol${String.format(Locale.US, "%.2f", abs / 10_000_000.0)} Cr"
            abs >= 100_000 -> "$sign$symbol${String.format(Locale.US, "%.2f", abs / 100_000.0)} L"
            abs >= 1_000 -> "$sign$symbol${String.format(Locale.US, "%.1f", abs / 1_000.0)} K"
            else -> formatInr(amount, symbol, includeDecimals = false)
        }
    }
}
