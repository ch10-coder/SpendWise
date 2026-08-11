package com.spendwise.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    val AVAILABLE_ICONS = mapOf(
        "Restaurant" to Icons.Default.Restaurant,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "Home" to Icons.Default.Home,
        "Bolt" to Icons.Default.Bolt,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "Movie" to Icons.Default.Movie,
        "LocalHospital" to Icons.Default.LocalHospital,
        "School" to Icons.Default.School,
        "Flight" to Icons.Default.Flight,
        "LocalCafe" to Icons.Default.LocalCafe,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "Pets" to Icons.Default.Pets,
        "Wifi" to Icons.Default.Wifi,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Laptop" to Icons.Default.Laptop,
        "TrendingUp" to Icons.Default.TrendingUp,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "Payments" to Icons.Default.Payments,
        "AccountBalance" to Icons.Default.AccountBalance,
        "AttachMoney" to Icons.Default.AttachMoney
    )

    fun getIcon(name: String): ImageVector {
        return AVAILABLE_ICONS[name] ?: Icons.Default.Category
    }

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF64748B)): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val colorLong = cleanHex.toLong(16)
            if (cleanHex.length == 6) {
                Color(colorLong or 0x00000000FF000000L)
            } else {
                Color(colorLong)
            }
        } catch (e: Exception) {
            defaultColor
        }
    }
}
