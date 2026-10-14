package com.example.expensetracker.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.model.TransactionWithCategory
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class CategoryReportItem(
    val categoryName: String,
    val categoryIcon: String,
    val totalAmount: Double,
    val percentage: Double
)

data class ReportsUiState(
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRate: Double = 0.0,
    val baseCurrency: String = "INR",
    val categoryReportItems: List<CategoryReportItem> = emptyList(),
    val topCategories: List<CategoryReportItem> = emptyList(),
    val overallBudget: Double = 0.0,
    val budgetPerformancePct: Double = 0.0,
    val isEmptyState: Boolean = false,
    val isLoading: Boolean = true
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val cal = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(cal.get(Calendar.MONTH) + 1)
    private val _selectedYear = MutableStateFlow(cal.get(Calendar.YEAR))

    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedMonth,
        _selectedYear,
        transactionRepository.allTransactions,
        budgetRepository.allBudgets,
        userPreferencesRepository.baseCurrency
    ) { month, year, allTx, allBudgets, baseCurr ->
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val monthStartMs = startCal.timeInMillis
        val monthEndMs = endCal.timeInMillis

        val monthTx = allTx.filter { it.transaction.date in monthStartMs..monthEndMs }

        val incomeSum = monthTx.filter { it.transaction.type == "INCOME" }
            .sumOf { it.transaction.convertedAmount }
        val expenseSum = monthTx.filter { it.transaction.type == "EXPENSE" }
            .sumOf { it.transaction.convertedAmount }

        val netSavings = incomeSum - expenseSum
        val savingsRate = if (incomeSum > 0) (netSavings / incomeSum) * 100.0 else 0.0

        val expenseTx = monthTx.filter { it.transaction.type == "EXPENSE" }
        val totalExpDenominator = if (expenseSum > 0) expenseSum else 1.0

        val categoryReportList = expenseTx.groupBy { it.category?.name ?: "Other" }
            .map { (catName, txList) ->
                val icon = txList.firstOrNull()?.category?.icon ?: "more_horiz"
                val sum = txList.sumOf { it.transaction.convertedAmount }
                val pct = (sum / totalExpDenominator) * 100.0
                CategoryReportItem(catName, icon, sum, pct)
            }
            .sortedByDescending { it.totalAmount }

        val top3Categories = categoryReportList.take(3)

        val monthlyBudgetObj = allBudgets.find { it.month == month && it.year == year && it.categoryId == null }
        val budgetVal = monthlyBudgetObj?.amount ?: 0.0
        val perfPct = if (budgetVal > 0) (expenseSum / budgetVal) * 100.0 else 0.0

        ReportsUiState(
            selectedMonth = month,
            selectedYear = year,
            totalIncome = incomeSum,
            totalExpense = expenseSum,
            netSavings = netSavings,
            savingsRate = savingsRate,
            baseCurrency = baseCurr,
            categoryReportItems = categoryReportList,
            topCategories = top3Categories,
            overallBudget = budgetVal,
            budgetPerformancePct = perfPct,
            isEmptyState = monthTx.isEmpty(),
            isLoading = false
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun onMonthYearSelected(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun previousMonth() {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            add(Calendar.MONTH, -1)
        }
        _selectedMonth.value = c.get(Calendar.MONTH) + 1
        _selectedYear.value = c.get(Calendar.YEAR)
    }

    fun nextMonth() {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            add(Calendar.MONTH, 1)
        }
        _selectedMonth.value = c.get(Calendar.MONTH) + 1
        _selectedYear.value = c.get(Calendar.YEAR)
    }

    fun goToCurrentMonth() {
        val c = Calendar.getInstance()
        _selectedMonth.value = c.get(Calendar.MONTH) + 1
        _selectedYear.value = c.get(Calendar.YEAR)
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val budgetRepository: BudgetRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(transactionRepository, budgetRepository, userPreferencesRepository) as T
        }
    }
}
