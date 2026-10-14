package com.example.expensetracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconMapper {
    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "fastfood" -> Icons.Default.Fastfood
            "directions_car" -> Icons.Default.DirectionsCar
            "home" -> Icons.Default.Home
            "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
            "movie" -> Icons.Default.Movie
            "shopping_bag" -> Icons.Default.ShoppingBag
            "medical_services" -> Icons.Default.MedicalServices
            "school" -> Icons.Default.School
            "flight" -> Icons.Default.Flight
            "spa" -> Icons.Default.Spa
            "payments" -> Icons.Default.Payments
            "work" -> Icons.Default.Work
            "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
            "card_giftcard" -> Icons.Default.CardGiftcard
            "savings" -> Icons.Default.Savings
            else -> Icons.Default.MoreHoriz
        }
    }
}
