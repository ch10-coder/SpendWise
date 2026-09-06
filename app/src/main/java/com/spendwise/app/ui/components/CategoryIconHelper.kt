package com.spendwise.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        "AttachMoney" to Icons.Default.AttachMoney,
        // Added icons
        "Subscriptions" to Icons.Default.Subscriptions,
        "CreditCard" to Icons.Default.CreditCard,
        "Savings" to Icons.Default.Savings,
        "ChildCare" to Icons.Default.ChildCare,
        "Star" to Icons.Default.Star,
        "Favorite" to Icons.Default.Favorite,
        "Work" to Icons.Default.Work,
        "LocalGroceryStore" to Icons.Default.LocalGroceryStore,
        "LocalPharmacy" to Icons.Default.LocalPharmacy,
        "LocalBar" to Icons.Default.LocalBar,
        "Sports" to Icons.Default.Sports,
        "Brush" to Icons.Default.Brush,
        "Build" to Icons.Default.Build,
        "PhoneAndroid" to Icons.Default.PhoneAndroid,
        "Cloud" to Icons.Default.Cloud,
        "Security" to Icons.Default.Security,
        "VolunteerActivism" to Icons.Default.VolunteerActivism,
        "Celebration" to Icons.Default.Celebration,
        "Checkroom" to Icons.Default.Checkroom,
        "Coffee" to Icons.Default.LocalCafe,
        "Pool" to Icons.Default.Pool,
        "MedicalServices" to Icons.Default.MedicalServices,
        "ElectricBolt" to Icons.Default.ElectricBolt,
        "WaterDrop" to Icons.Default.WaterDrop,
        "Garage" to Icons.Default.Garage,
        "Chair" to Icons.Default.Chair,
        "Microwave" to Icons.Default.Microwave,
        "SelfImprovement" to Icons.Default.SelfImprovement,
        "Commute" to Icons.Default.Commute,
        "QrCode" to Icons.Default.QrCode,
        "CurrencyRupee" to Icons.Default.CurrencyRupee
    )

    fun getIcon(name: String): ImageVector {
        return AVAILABLE_ICONS[name] ?: Icons.Default.Category
    }

    private val colorCache = java.util.concurrent.ConcurrentHashMap<String, Color>()

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF64748B)): Color {
        return colorCache.computeIfAbsent(hex) {
            try {
                val cleanHex = it.removePrefix("#")
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
}
