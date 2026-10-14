package com.example.expensetracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.local.model.TransactionWithCategory
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val totalCount: Int = 0,
    val filteredIncome: Double = 0.0,
    val filteredExpense: Double = 0.0,
    val netTotal: Double = 0.0,
    val categories: List<CategoryEntity> = emptyList(),
    val filterCriteria: TransactionFilterCriteria = TransactionFilterCriteria(),
    val baseCurrency: String = "INR",
    val transactionToDelete: TransactionWithCategory? = null,
    val snackbarMessage: String? = null,
    val isLoading: Boolean = true
) {
    // Backwards-compatible properties
    val searchQuery: String get() = filterCriteria.searchQuery
    val selectedTypeFilter: String get() = filterCriteria.type
    val selectedCategoryId: Long? get() = filterCriteria.categoryId
}

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _filterCriteria = MutableStateFlow(TransactionFilterCriteria())
    val filterCriteria: StateFlow<TransactionFilterCriteria> = _filterCriteria.asStateFlow()

    private val _transactionToDelete = MutableStateFlow<TransactionWithCategory?>(null)
    val transactionToDelete: StateFlow<TransactionWithCategory?> = _transactionToDelete.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.allTransactions,
        categoryRepository.allCategories,
        userPreferencesRepository.baseCurrency,
        _filterCriteria,
        _transactionToDelete,
        _snackbarMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allTx = args[0] as List<TransactionWithCategory>
        @Suppress("UNCHECKED_CAST")
        val categories = args[1] as List<CategoryEntity>
        val baseCurrency = args[2] as String
        val criteria = args[3] as TransactionFilterCriteria
        val toDelete = args[4] as TransactionWithCategory?
        val message = args[5] as String?

        val querySpecifiesDate = if (criteria.searchQuery.isNotBlank()) {
            com.example.expensetracker.utils.TransactionSearchMatcher.querySpecifiesDate(criteria.searchQuery)
        } else false

        val (startDate, endDate) = if (querySpecifiesDate) {
            Pair(null, null) // Explicit date/month/year search queries search globally across all dates
        } else {
            TransactionFilterCriteria.computeDateRange(
                criteria.datePreset,
                criteria.customStartDate,
                criteria.customEndDate
            )
        }

        var filteredList = allTx.filter { item ->
            val tx = item.transaction
            val cat = item.category

            // 1. Type Filter
            if (criteria.type != "ALL" && tx.type != criteria.type) {
                return@filter false
            }

            // 1.5. Split Bill Filter
            if (criteria.isSplitOnly && !tx.isSplit) {
                return@filter false
            }

            // 2. Category Filter
            if (criteria.categoryId != null && criteria.categoryId > 0 && tx.categoryId != criteria.categoryId) {
                return@filter false
            }

            // 3. Payment Method Filter
            if (criteria.paymentMethod != "ALL" && !tx.paymentMethod.equals(criteria.paymentMethod, ignoreCase = true)) {
                return@filter false
            }

            // 4. Amount Range Filter
            val amountValue = if (tx.convertedAmount > 0.0) tx.convertedAmount else tx.amount
            if (criteria.minAmount != null && amountValue < criteria.minAmount) {
                return@filter false
            }
            if (criteria.maxAmount != null && amountValue > criteria.maxAmount) {
                return@filter false
            }

            // 5. Date Range Filter
            if (startDate != null && tx.date < startDate) {
                return@filter false
            }
            if (endDate != null && tx.date > endDate) {
                return@filter false
            }

            // 6. Universal Keyword Search Filter (Date, Month, Year, Note, Category, Amount, Payment, Type)
            if (criteria.searchQuery.isNotBlank()) {
                if (!com.example.expensetracker.utils.TransactionSearchMatcher.matches(item, criteria.searchQuery)) {
                    return@filter false
                }
            }

            true
        }

        // Apply Sorting
        filteredList = when (criteria.sortOrder) {
            TransactionSortOrder.DATE_DESC -> filteredList.sortedWith(compareByDescending<TransactionWithCategory> { it.transaction.date }.thenByDescending { it.transaction.id })
            TransactionSortOrder.DATE_ASC -> filteredList.sortedWith(compareBy<TransactionWithCategory> { it.transaction.date }.thenBy { it.transaction.id })
            TransactionSortOrder.AMOUNT_DESC -> filteredList.sortedByDescending { if (it.transaction.convertedAmount > 0.0) it.transaction.convertedAmount else it.transaction.amount }
            TransactionSortOrder.AMOUNT_ASC -> filteredList.sortedBy { if (it.transaction.convertedAmount > 0.0) it.transaction.convertedAmount else it.transaction.amount }
        }

        // Compute Financial Summary for Filtered View
        var incomeSum = 0.0
        var expenseSum = 0.0
        filteredList.forEach { item ->
            val valAmount = if (item.transaction.convertedAmount > 0.0) item.transaction.convertedAmount else item.transaction.amount
            if (item.transaction.type == "INCOME") {
                incomeSum += valAmount
            } else {
                expenseSum += valAmount
            }
        }

        TransactionsUiState(
            transactions = filteredList,
            totalCount = filteredList.size,
            filteredIncome = incomeSum,
            filteredExpense = expenseSum,
            netTotal = incomeSum - expenseSum,
            categories = categories,
            filterCriteria = criteria,
            baseCurrency = baseCurrency,
            transactionToDelete = toDelete,
            snackbarMessage = message,
            isLoading = false
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _filterCriteria.value = _filterCriteria.value.copy(searchQuery = query)
    }

    fun onTypeFilterSelected(type: String) {
        val currentCatId = _filterCriteria.value.categoryId
        val updatedCatId = if (currentCatId != null && type != "ALL") {
            val cat = uiState.value.categories.find { it.id == currentCatId }
            if (cat != null && !cat.type.equals(type, ignoreCase = true)) null else currentCatId
        } else {
            currentCatId
        }
        _filterCriteria.value = _filterCriteria.value.copy(type = type, categoryId = updatedCatId)
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        _filterCriteria.value = _filterCriteria.value.copy(categoryId = categoryId)
    }

    fun onPaymentMethodFilterSelected(paymentMethod: String) {
        _filterCriteria.value = _filterCriteria.value.copy(paymentMethod = paymentMethod)
    }

    fun onAmountRangeChanged(min: Double?, max: Double?) {
        _filterCriteria.value = _filterCriteria.value.copy(minAmount = min, maxAmount = max)
    }

    fun onDatePresetSelected(preset: DateRangePreset, customStart: Long? = null, customEnd: Long? = null) {
        _filterCriteria.value = _filterCriteria.value.copy(
            datePreset = preset,
            customStartDate = customStart,
            customEndDate = customEnd
        )
    }

    fun onSortOrderSelected(order: TransactionSortOrder) {
        _filterCriteria.value = _filterCriteria.value.copy(sortOrder = order)
    }

    fun applyFilterCriteria(newCriteria: TransactionFilterCriteria) {
        _filterCriteria.value = newCriteria
    }

    fun resetAllFilters() {
        val currentQuery = _filterCriteria.value.searchQuery
        _filterCriteria.value = TransactionFilterCriteria(searchQuery = currentQuery)
    }

    fun onToggleSplitFilter() {
        val current = _filterCriteria.value
        _filterCriteria.value = current.copy(isSplitOnly = !current.isSplitOnly)
    }

    fun clearAllFiltersAndSearch() {
        _filterCriteria.value = TransactionFilterCriteria()
    }

    fun requestDeleteTransaction(tx: TransactionWithCategory) {
        _transactionToDelete.value = tx
    }

    fun dismissDeleteDialog() {
        _transactionToDelete.value = null
    }

    fun confirmDeleteTransaction() {
        val tx = _transactionToDelete.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(tx.transaction)
            _transactionToDelete.value = null
            _snackbarMessage.value = "Transaction deleted successfully"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    fun toggleSplitPaidStatus(splitId: Long, currentPaid: Boolean) {
        viewModelScope.launch {
            transactionRepository.updateSplitPaidStatus(splitId, !currentPaid)
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val userPreferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(transactionRepository, categoryRepository, userPreferencesRepository) as T
        }
    }
}
