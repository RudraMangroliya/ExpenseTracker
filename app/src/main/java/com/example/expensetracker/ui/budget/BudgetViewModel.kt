package com.example.expensetracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryBudgetUiModel(
    val category: CategoryEntity,
    val budgetAmount: Double,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentage: Double
)

data class BudgetUiState(
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val overallBudget: Double = 0.0,
    val overallSpent: Double = 0.0,
    val overallRemaining: Double = 0.0,
    val overallPercentage: Double = 0.0,
    val categoryBudgets: List<CategoryBudgetUiModel> = emptyList(),
    val baseCurrency: String = "INR",
    val showSetBudgetDialog: Boolean = false,
    val editingCategoryId: Long? = null, // null for overall budget, non-null for category budget
    val dialogAmountInput: String = "",
    val dialogError: String? = null
)

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val cal = Calendar.getInstance()
    private val _selectedMonth = MutableStateFlow(cal.get(Calendar.MONTH) + 1)
    private val _selectedYear = MutableStateFlow(cal.get(Calendar.YEAR))

    private val _showSetBudgetDialog = MutableStateFlow(false)
    private val _editingCategoryId = MutableStateFlow<Long?>(null)
    private val _dialogAmountInput = MutableStateFlow("")
    private val _dialogError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetUiState> = combine(
        _selectedMonth,
        _selectedYear,
        budgetRepository.allBudgets,
        transactionRepository.allTransactions,
        categoryRepository.getCategoriesByType("EXPENSE"),
        userPreferencesRepository.baseCurrency,
        _showSetBudgetDialog,
        _editingCategoryId,
        _dialogAmountInput,
        _dialogError
    ) { args: Array<Any?> ->
        val month = args[0] as Int
        val year = args[1] as Int
        @Suppress("UNCHECKED_CAST")
        val allBudgets = args[2] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val allTx = args[3] as List<com.example.expensetracker.data.local.model.TransactionWithCategory>
        @Suppress("UNCHECKED_CAST")
        val expenseCategories = args[4] as List<CategoryEntity>
        val baseCurr = args[5] as String
        val showDialog = args[6] as Boolean
        val editCatId = args[7] as Long?
        val inputAmt = args[8] as String
        val err = args[9] as String?

        // Calculate start & end timestamp for month
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

        val monthExpenseTx = allTx.filter {
            it.transaction.type == "EXPENSE" &&
                    it.transaction.date in monthStartMs..monthEndMs
        }

        val totalMonthSpent = monthExpenseTx.sumOf { it.transaction.convertedAmount }

        val monthlyBudgets = allBudgets.filter { it.month == month && it.year == year }
        val overallBudgetObj = monthlyBudgets.find { it.categoryId == null }
        val overallBudgetVal = overallBudgetObj?.amount ?: 0.0

        val overallRemaining = overallBudgetVal - totalMonthSpent
        val overallPct = if (overallBudgetVal > 0) (totalMonthSpent / overallBudgetVal) * 100.0 else 0.0

        // Category budgets breakdown
        val catBudgetsList = expenseCategories.map { cat ->
            val catBudgetVal = monthlyBudgets.find { it.categoryId == cat.id }?.amount ?: 0.0
            val catSpentVal = monthExpenseTx.filter { it.transaction.categoryId == cat.id }
                .sumOf { it.transaction.convertedAmount }
            val catRemaining = catBudgetVal - catSpentVal
            val catPct = if (catBudgetVal > 0) (catSpentVal / catBudgetVal) * 100.0 else 0.0

            CategoryBudgetUiModel(
                category = cat,
                budgetAmount = catBudgetVal,
                spentAmount = catSpentVal,
                remainingAmount = catRemaining,
                percentage = catPct
            )
        }

        BudgetUiState(
            selectedMonth = month,
            selectedYear = year,
            overallBudget = overallBudgetVal,
            overallSpent = totalMonthSpent,
            overallRemaining = overallRemaining,
            overallPercentage = overallPct,
            categoryBudgets = catBudgetsList,
            baseCurrency = baseCurr,
            showSetBudgetDialog = showDialog,
            editingCategoryId = editCatId,
            dialogAmountInput = inputAmt,
            dialogError = err
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState()
    )

    fun onMonthYearSelected(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun openSetBudgetDialog(categoryId: Long?, currentAmount: Double) {
        _editingCategoryId.value = categoryId
        _dialogAmountInput.value = if (currentAmount > 0) currentAmount.toString() else ""
        _dialogError.value = null
        _showSetBudgetDialog.value = true
    }

    fun closeSetBudgetDialog() {
        _showSetBudgetDialog.value = false
        _dialogError.value = null
    }

    fun onDialogAmountInputChanged(input: String) {
        _dialogAmountInput.value = input
        _dialogError.value = null
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

    fun saveBudget() {
        val input = _dialogAmountInput.value.trim()
        val amountVal = input.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0) {
            _dialogError.value = "Enter a valid positive budget amount"
            return
        }

        val state = uiState.value
        val entity = BudgetEntity(
            categoryId = state.editingCategoryId,
            amount = amountVal,
            currency = state.baseCurrency,
            month = state.selectedMonth,
            year = state.selectedYear
        )

        viewModelScope.launch {
            budgetRepository.setBudget(entity)
            closeSetBudgetDialog()
        }
    }

    class Factory(
        private val budgetRepository: BudgetRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetViewModel(budgetRepository, transactionRepository, categoryRepository, userPreferencesRepository) as T
        }
    }
}
