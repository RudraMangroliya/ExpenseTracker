package com.example.expensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.local.model.TransactionWithCategory
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class CategorySpending(
    val categoryName: String,
    val categoryIcon: String,
    val amount: Double,
    val percentage: Double
)

data class DashboardUiState(
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val baseCurrency: String = "INR",
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val categorySpendingList: List<CategorySpending> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.allTransactions,
        budgetRepository.allBudgets,
        userPreferencesRepository.baseCurrency
    ) { transactions, allBudgets, baseCurrency ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)

        val overallBudget = allBudgets.find { it.month == currentMonth && it.year == currentYear && it.categoryId == null }

        val totalIncome = transactions.filter { it.transaction.type == "INCOME" }
            .sumOf { it.transaction.convertedAmount }
        val totalExpense = transactions.filter { it.transaction.type == "EXPENSE" }
            .sumOf { it.transaction.convertedAmount }
        val balance = totalIncome - totalExpense

        // Current month expenses
        val currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentMonthExpense = transactions
            .filter { it.transaction.type == "EXPENSE" && it.transaction.date >= currentMonthStart }
            .sumOf { it.transaction.convertedAmount }

        val monthlyBudgetAmount = overallBudget?.amount ?: 0.0
        val remainingBudget = if (monthlyBudgetAmount > 0) monthlyBudgetAmount - currentMonthExpense else 0.0

        // Recent 5 transactions
        val recentList = transactions.take(5)

        // Category-wise spending calculation for expenses
        val expenseTransactions = transactions.filter { it.transaction.type == "EXPENSE" }
        val totalExpenseSum = if (totalExpense > 0) totalExpense else 1.0

        val categorySpendingMap = expenseTransactions
            .groupBy { it.category?.name ?: "Other" }
            .map { (catName, txList) ->
                val icon = txList.firstOrNull()?.category?.icon ?: "more_horiz"
                val sum = txList.sumOf { it.transaction.convertedAmount }
                val pct = (sum / totalExpenseSum) * 100.0
                CategorySpending(catName, icon, sum, pct)
            }
            .sortedByDescending { it.amount }

        userPreferencesRepository.updateCachedDashboardData(
            balance = balance,
            income = totalIncome,
            expense = totalExpense,
            monthlyExpense = currentMonthExpense,
            monthlyBudget = monthlyBudgetAmount,
            month = currentMonth,
            year = currentYear
        )

        DashboardUiState(
            balance = balance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            monthlyExpense = currentMonthExpense,
            monthlyBudget = monthlyBudgetAmount,
            remainingBudget = remainingBudget,
            baseCurrency = baseCurrency,
            recentTransactions = recentList,
            categorySpendingList = categorySpendingMap,
            isLoading = false
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            balance = userPreferencesRepository.cachedBalance.value,
            totalIncome = userPreferencesRepository.cachedIncome.value,
            totalExpense = userPreferencesRepository.cachedExpense.value,
            monthlyExpense = userPreferencesRepository.cachedMonthlyExpense.value,
            monthlyBudget = userPreferencesRepository.cachedMonthlyBudget.value,
            remainingBudget = if (userPreferencesRepository.cachedMonthlyBudget.value > 0) {
                (userPreferencesRepository.cachedMonthlyBudget.value - userPreferencesRepository.cachedMonthlyExpense.value).coerceAtLeast(0.0)
            } else 0.0,
            baseCurrency = userPreferencesRepository.baseCurrency.value,
            isLoading = true
        )
    )

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val budgetRepository: BudgetRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(transactionRepository, budgetRepository, userPreferencesRepository) as T
        }
    }
}
