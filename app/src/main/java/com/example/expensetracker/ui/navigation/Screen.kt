package com.example.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val navLabel: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", "Home", Icons.Default.Dashboard)
    object Transactions : Screen("transactions", "Transactions", "History", Icons.AutoMirrored.Filled.ReceiptLong)
    object Budgets : Screen("budgets", "Budgets", "Budgets", Icons.Default.AccountBalanceWallet)
    object Reports : Screen("reports", "Reports", "Reports", Icons.Default.BarChart)
    object Settings : Screen("settings", "Settings", "Settings", Icons.Default.Settings)

    object AddTransaction : Screen("add_transaction", "Add Transaction", "Add", Icons.AutoMirrored.Filled.ReceiptLong)
    object EditTransaction : Screen("edit_transaction/{transactionId}", "Edit Transaction", "Edit", Icons.AutoMirrored.Filled.ReceiptLong) {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }

    companion object {
        val bottomNavScreens: List<Screen> by lazy {
            listOf(
                Dashboard,
                Transactions,
                Budgets,
                Reports,
                Settings
            )
        }
    }
}
