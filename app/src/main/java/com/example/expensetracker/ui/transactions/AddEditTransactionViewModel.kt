package com.example.expensetracker.ui.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.CurrencyConverter
import com.example.expensetracker.data.repository.ExchangeRateRepository
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.utils.NotificationHelper
import com.example.expensetracker.utils.ValidationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.expensetracker.data.repository.SplitParticipantData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SplitMode {
    EQUAL,
    CUSTOM
}

data class SplitParticipantItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String = "",
    val amount: String = "0.0",
    val isCurrentUser: Boolean = false,
    val isPaid: Boolean = false
)

data class AddEditTransactionFormState(
    val transactionId: Long? = null,
    val type: String = "EXPENSE",
    val amount: String = "",
    val currency: String = "INR",
    val baseCurrency: String = "INR",
    val categoryId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val paymentMethod: String = "Cash",
    val isSplitEnabled: Boolean = false,
    val splitMode: SplitMode = SplitMode.EQUAL,
    val splitParticipants: List<SplitParticipantItem> = emptyList(),
    val splitError: String? = null,
    val amountError: String? = null,
    val categoryError: String? = null,
    val noteError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val rateStatusInfo: String? = null
)

class AddEditTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val context: Context
) : ViewModel() {

    private val _formState = MutableStateFlow(AddEditTransactionFormState())
    val formState: StateFlow<AddEditTransactionFormState> = _formState.asStateFlow()

    init {
        val baseCurr = userPreferencesRepository.baseCurrency.value
        _formState.value = _formState.value.copy(
            currency = baseCurr,
            baseCurrency = baseCurr
        )
    }

    fun prepareForAdd() {
        val baseCurr = userPreferencesRepository.baseCurrency.value
        _formState.value = AddEditTransactionFormState(
            transactionId = null,
            type = "EXPENSE",
            amount = "",
            currency = baseCurr,
            baseCurrency = baseCurr,
            date = System.currentTimeMillis(),
            note = "",
            paymentMethod = "Cash",
            isSplitEnabled = false,
            splitMode = SplitMode.EQUAL,
            splitParticipants = emptyList(),
            splitError = null,
            isSaving = false,
            isDeleting = false,
            saveSuccess = false,
            deleteSuccess = false
        )
        loadCategories("EXPENSE")
    }

    fun clearEvents() {
        _formState.value = _formState.value.copy(
            saveSuccess = false,
            deleteSuccess = false
        )
    }

    fun loadTransactionForEdit(transactionId: Long) {
        viewModelScope.launch {
            val baseCurr = userPreferencesRepository.baseCurrency.value
            val txWithCat = transactionRepository.getTransactionById(transactionId).first()
            if (txWithCat != null) {
                val tx = txWithCat.transaction
                val isSplit = tx.isSplit
                // When editing a split expense, show original total bill in the amount input!
                val displayAmount = if (isSplit && tx.originalTotalAmount > 0.0) {
                    tx.originalTotalAmount
                } else {
                    tx.amount
                }

                val participants = if (isSplit && txWithCat.splits.isNotEmpty()) {
                    txWithCat.splits.map { split ->
                        SplitParticipantItem(
                            id = split.id.toString(),
                            name = split.name,
                            phoneNumber = split.phoneNumber,
                            amount = if (split.amount % 1.0 == 0.0) split.amount.toLong().toString() else split.amount.toString(),
                            isCurrentUser = split.isCurrentUser,
                            isPaid = split.isPaid
                        )
                    }
                } else emptyList()

                val isCustom = participants.size >= 2 && participants.mapNotNull { it.amount.toDoubleOrNull() }.distinct().size > 1

                _formState.value = AddEditTransactionFormState(
                    transactionId = tx.id,
                    type = tx.type,
                    amount = if (displayAmount % 1.0 == 0.0) displayAmount.toLong().toString() else displayAmount.toString(),
                    currency = tx.currency,
                    baseCurrency = baseCurr,
                    categoryId = tx.categoryId,
                    date = tx.date,
                    note = tx.note,
                    paymentMethod = tx.paymentMethod,
                    isSplitEnabled = isSplit,
                    splitMode = if (isCustom) SplitMode.CUSTOM else SplitMode.EQUAL,
                    splitParticipants = participants,
                    splitError = null,
                    isSaving = false,
                    isDeleting = false,
                    saveSuccess = false,
                    deleteSuccess = false
                )
                loadCategories(tx.type)
                checkRateStatus()
            }
        }
    }

    fun onTypeChanged(newType: String) {
        _formState.value = _formState.value.copy(
            type = newType,
            categoryId = null, // reset category choice for new type
            categoryError = null,
            isSplitEnabled = if (newType == "INCOME") false else _formState.value.isSplitEnabled
        )
        loadCategories(newType)
    }

    fun toggleSplit(enabled: Boolean) {
        if (!enabled) {
            _formState.value = _formState.value.copy(
                isSplitEnabled = false,
                splitError = null
            )
            return
        }

        var participants = _formState.value.splitParticipants
        if (participants.isEmpty()) {
            participants = listOf(
                SplitParticipantItem(
                    name = "You",
                    phoneNumber = "",
                    isCurrentUser = true
                )
            )
        }
        _formState.value = _formState.value.copy(
            isSplitEnabled = true,
            splitParticipants = participants
        )
        recalculateSplits()
    }

    fun setSplitMode(mode: SplitMode) {
        _formState.value = _formState.value.copy(
            splitMode = mode,
            splitError = null
        )
        recalculateSplits()
    }

    fun addParticipant(name: String, phoneNumber: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        val current = _formState.value.splitParticipants.toMutableList()
        current.add(
            SplitParticipantItem(
                name = trimmedName,
                phoneNumber = phoneNumber.trim(),
                isCurrentUser = false
            )
        )
        _formState.value = _formState.value.copy(splitParticipants = current)
        recalculateSplits()
    }

    fun removeParticipant(id: String) {
        val current = _formState.value.splitParticipants.filterNot { it.id == id && !it.isCurrentUser }
        _formState.value = _formState.value.copy(splitParticipants = current)
        recalculateSplits()
    }

    fun updateParticipantAmount(id: String, amountStr: String) {
        val current = _formState.value.splitParticipants.map {
            if (it.id == id) it.copy(amount = amountStr) else it
        }
        _formState.value = _formState.value.copy(splitParticipants = current)
        validateCustomSplit(current)
    }

    private fun recalculateSplits() {
        val state = _formState.value
        if (!state.isSplitEnabled || state.splitParticipants.isEmpty()) return

        val totalAmount = state.amount.toDoubleOrNull() ?: 0.0
        val count = state.splitParticipants.size

        if (state.splitMode == SplitMode.EQUAL) {
            if (count > 0 && totalAmount > 0.0) {
                val equalShare = CurrencyConverter.roundToTwoDecimals(totalAmount / count)
                // Absorb cent difference into user's share so sum strictly matches total bill
                val sumOther = equalShare * (count - 1)
                val userShare = CurrencyConverter.roundToTwoDecimals(totalAmount - sumOther)

                val updated = state.splitParticipants.map { item ->
                    val share = if (item.isCurrentUser) userShare else equalShare
                    item.copy(amount = if (share % 1.0 == 0.0) share.toLong().toString() else String.format(java.util.Locale.US, "%.2f", share))
                }
                _formState.value = _formState.value.copy(splitParticipants = updated, splitError = null)
            } else {
                val updated = state.splitParticipants.map { it.copy(amount = "0.0") }
                _formState.value = _formState.value.copy(splitParticipants = updated, splitError = null)
            }
        } else {
            validateCustomSplit(state.splitParticipants)
        }
    }

    private fun validateCustomSplit(participants: List<SplitParticipantItem>) {
        val totalAmount = _formState.value.amount.toDoubleOrNull() ?: 0.0
        val sumShares = participants.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val diff = CurrencyConverter.roundToTwoDecimals(totalAmount - sumShares)

        val error = when {
            kotlin.math.abs(diff) < 0.01 -> null
            diff > 0 -> String.format(java.util.Locale.US, "Remaining to allocate: %.2f", diff)
            else -> String.format(java.util.Locale.US, "Exceeds total bill by: %.2f", -diff)
        }
        _formState.value = _formState.value.copy(splitError = error)
    }

    private var categoriesJob: kotlinx.coroutines.Job? = null

    private fun loadCategories(type: String) {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            categoryRepository.getCategoriesByType(type).collect { list ->
                _formState.value = _formState.value.copy(categories = list)
                if (_formState.value.categoryId == null && list.isNotEmpty()) {
                    _formState.value = _formState.value.copy(categoryId = list.first().id)
                }
            }
        }
    }

    fun onAmountChanged(amount: String) {
        _formState.value = _formState.value.copy(
            amount = amount,
            amountError = ValidationUtils.validateAmount(amount)
        )
        checkRateStatus()
        recalculateSplits()
    }

    fun onCurrencyChanged(currency: String) {
        _formState.value = _formState.value.copy(currency = currency)
        checkRateStatus()
    }

    fun onCategoryChanged(categoryId: Long) {
        _formState.value = _formState.value.copy(
            categoryId = categoryId,
            categoryError = ValidationUtils.validateCategory(categoryId)
        )
    }

    fun onDateChanged(dateMs: Long) {
        _formState.value = _formState.value.copy(date = dateMs)
    }

    fun onNoteChanged(note: String) {
        _formState.value = _formState.value.copy(
            note = note,
            noteError = ValidationUtils.validateNote(note)
        )
    }

    fun onPaymentMethodChanged(method: String) {
        _formState.value = _formState.value.copy(paymentMethod = method)
    }

    private fun checkRateStatus() {
        val curr = _formState.value.currency.uppercase().trim()
        val base = _formState.value.baseCurrency.uppercase().trim()
        if (curr == base) {
            _formState.value = _formState.value.copy(rateStatusInfo = null)
            return
        }
        viewModelScope.launch {
            val rateRes = exchangeRateRepository.getRate(curr, base)
            val amt = _formState.value.amount.toDoubleOrNull()
            val rateFormatted = String.format(java.util.Locale.getDefault(), "%.4f", rateRes.rate)
            val tag = if (rateRes.isCached) "Cached" else "Live"
            val infoStr = if (amt != null && amt > 0.0) {
                val converted = CurrencyConverter.convertAmount(amt, rateRes.rate)
                val convStr = ValidationUtils.formatCurrency(converted, base)
                "1 $curr ≈ $rateFormatted $base ($tag) • Total: $convStr"
            } else {
                "Rate: 1 $curr = $rateFormatted $base ($tag)"
            }
            _formState.value = _formState.value.copy(rateStatusInfo = infoStr)
        }
    }

    fun saveTransaction(onCloseStarted: (() -> Unit)? = null) {
        if (_formState.value.isSaving || _formState.value.isDeleting) return

        val currentState = _formState.value
        val amountErr = ValidationUtils.validateAmount(currentState.amount)
        val catErr = ValidationUtils.validateCategory(currentState.categoryId)
        val noteErr = ValidationUtils.validateNote(currentState.note)

        if (amountErr != null || catErr != null || noteErr != null) {
            _formState.value = currentState.copy(
                amountError = amountErr,
                categoryError = catErr,
                noteError = noteErr
            )
            return
        }

        if (currentState.isSplitEnabled && currentState.type == "EXPENSE") {
            if (currentState.splitParticipants.size < 2) {
                _formState.value = currentState.copy(
                    splitError = "Please add at least 1 person to split with"
                )
                return
            }
            if (currentState.splitError != null) {
                return
            }
        }

        val totalBillVal = currentState.amount.toDouble()
        val catId = currentState.categoryId!!

        // Core rule: "after splite my amount only add in expense"
        val userShareAmount = if (currentState.isSplitEnabled && currentState.type == "EXPENSE") {
            val userItem = currentState.splitParticipants.firstOrNull { it.isCurrentUser }
            userItem?.amount?.toDoubleOrNull() ?: totalBillVal
        } else {
            totalBillVal
        }

        val splitDataList = if (currentState.isSplitEnabled && currentState.type == "EXPENSE") {
            currentState.splitParticipants.map {
                SplitParticipantData(
                    name = it.name.trim(),
                    phoneNumber = it.phoneNumber.trim(),
                    amount = it.amount.toDoubleOrNull() ?: 0.0,
                    isCurrentUser = it.isCurrentUser,
                    isPaid = it.isPaid
                )
            }
        } else emptyList()

        _formState.value = currentState.copy(isSaving = true)

        // 1. Trigger the close animation of the sheet / + button first
        onCloseStarted?.invoke()

        viewModelScope.launch {
            try {
                // 2. Allow the sheet to finish closing into the + FAB (240ms duration) before DB commit
                if (onCloseStarted != null) {
                    delay(280)
                }

                if (currentState.transactionId == null) {
                    transactionRepository.addTransaction(
                        type = currentState.type,
                        amount = userShareAmount,
                        currency = currentState.currency,
                        baseCurrency = currentState.baseCurrency,
                        categoryId = catId,
                        date = currentState.date,
                        note = currentState.note.trim(),
                        paymentMethod = currentState.paymentMethod,
                        isSplit = currentState.isSplitEnabled && currentState.type == "EXPENSE",
                        originalTotalAmount = totalBillVal,
                        splits = splitDataList
                    )
                } else {
                    transactionRepository.updateTransaction(
                        id = currentState.transactionId,
                        type = currentState.type,
                        amount = userShareAmount,
                        currency = currentState.currency,
                        baseCurrency = currentState.baseCurrency,
                        categoryId = catId,
                        date = currentState.date,
                        note = currentState.note.trim(),
                        paymentMethod = currentState.paymentMethod,
                        isSplit = currentState.isSplitEnabled && currentState.type == "EXPENSE",
                        originalTotalAmount = totalBillVal,
                        splits = splitDataList
                    )
                }
                if (currentState.type == "EXPENSE") {
                    try {
                        NotificationHelper.checkAndTriggerBudgetAlerts(
                            context = context,
                            budgetRepository = budgetRepository,
                            transactionRepository = transactionRepository,
                            userPreferencesRepository = userPreferencesRepository,
                            categoryRepository = categoryRepository,
                            transactionDate = currentState.date,
                            categoryId = catId
                        )
                    } catch (_: Exception) {
                        // Safe failure for notification delivery
                    }
                }

                _formState.value = _formState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    amountError = "Failed to save transaction: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteCurrentTransaction(onCloseStarted: (() -> Unit)? = null) {
        if (_formState.value.isSaving || _formState.value.isDeleting) return
        val txId = _formState.value.transactionId ?: return
        _formState.value = _formState.value.copy(isDeleting = true)

        // 1. Trigger close animation of sheet first
        onCloseStarted?.invoke()

        viewModelScope.launch {
            try {
                // 2. Allow close animation to complete before committing deletion
                if (onCloseStarted != null) {
                    delay(280)
                }
                transactionRepository.deleteTransactionById(txId)
                _formState.value = _formState.value.copy(isDeleting = false, deleteSuccess = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(
                    isDeleting = false,
                    amountError = "Failed to delete transaction: ${e.localizedMessage}"
                )
            }
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val budgetRepository: BudgetRepository,
        private val exchangeRateRepository: ExchangeRateRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditTransactionViewModel(
                transactionRepository,
                categoryRepository,
                budgetRepository,
                exchangeRateRepository,
                userPreferencesRepository,
                context
            ) as T
        }
    }
}
