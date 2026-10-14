package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.AppDatabase
import com.example.expensetracker.data.local.dao.TransactionDao
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.data.local.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

import com.example.expensetracker.data.local.dao.TransactionSplitDao
import com.example.expensetracker.data.local.entity.TransactionSplitEntity

data class SplitParticipantData(
    val name: String,
    val phoneNumber: String = "",
    val amount: Double,
    val isCurrentUser: Boolean = false,
    val isPaid: Boolean = false
)

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val appDatabase: AppDatabase? = null
) {
    private val transactionSplitDao: TransactionSplitDao? = appDatabase?.transactionSplitDao()

    val allTransactions: Flow<List<TransactionWithCategory>> = transactionDao.getAllTransactions()

    fun getTransactionById(id: Long): Flow<TransactionWithCategory?> {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }

    fun getTransactionsByType(type: String): Flow<List<TransactionWithCategory>> {
        return transactionDao.getTransactionsByType(type)
    }

    fun getSplitsForTransaction(transactionId: Long): Flow<List<TransactionSplitEntity>> {
        return transactionSplitDao?.getSplitsByTransactionId(transactionId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun updateSplitPaidStatus(splitId: Long, isPaid: Boolean) {
        transactionSplitDao?.updatePaidStatus(
            splitId = splitId,
            isPaid = isPaid,
            paidDate = if (isPaid) System.currentTimeMillis() else null
        )
    }

    suspend fun addTransaction(
        type: String,
        amount: Double,
        currency: String,
        baseCurrency: String,
        categoryId: Long,
        date: Long,
        note: String,
        paymentMethod: String,
        isSplit: Boolean = false,
        originalTotalAmount: Double = 0.0,
        splits: List<SplitParticipantData> = emptyList()
    ): Long {
        val cleanAmount = CurrencyConverter.roundToTwoDecimals(amount)
        val rateResult = exchangeRateRepository.getRate(currency, baseCurrency)
        val convertedAmount = CurrencyConverter.convertAmount(cleanAmount, rateResult.rate)

        val entity = TransactionEntity(
            type = type,
            amount = cleanAmount,
            currency = currency.uppercase().trim(),
            convertedAmount = convertedAmount,
            categoryId = categoryId,
            date = date,
            note = note,
            paymentMethod = paymentMethod,
            isSplit = isSplit,
            originalTotalAmount = if (isSplit) originalTotalAmount else cleanAmount,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val insertedId = transactionDao.insertTransaction(entity)

        if (isSplit && splits.isNotEmpty() && transactionSplitDao != null) {
            val splitEntities = splits.map {
                TransactionSplitEntity(
                    transactionId = insertedId,
                    name = it.name.trim(),
                    phoneNumber = it.phoneNumber.trim(),
                    amount = CurrencyConverter.roundToTwoDecimals(it.amount),
                    isCurrentUser = it.isCurrentUser,
                    isPaid = it.isPaid,
                    paidDate = if (it.isPaid) System.currentTimeMillis() else null
                )
            }
            transactionSplitDao.insertSplits(splitEntities)
        }

        return insertedId
    }

    suspend fun updateTransaction(
        id: Long,
        type: String,
        amount: Double,
        currency: String,
        baseCurrency: String,
        categoryId: Long,
        date: Long,
        note: String,
        paymentMethod: String,
        isSplit: Boolean = false,
        originalTotalAmount: Double = 0.0,
        splits: List<SplitParticipantData> = emptyList()
    ) {
        val cleanAmount = CurrencyConverter.roundToTwoDecimals(amount)
        val rateResult = exchangeRateRepository.getRate(currency, baseCurrency)
        val convertedAmount = CurrencyConverter.convertAmount(cleanAmount, rateResult.rate)

        val entity = TransactionEntity(
            id = id,
            type = type,
            amount = cleanAmount,
            currency = currency.uppercase().trim(),
            convertedAmount = convertedAmount,
            categoryId = categoryId,
            date = date,
            note = note,
            paymentMethod = paymentMethod,
            isSplit = isSplit,
            originalTotalAmount = if (isSplit) originalTotalAmount else cleanAmount,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(entity)

        if (transactionSplitDao != null) {
            transactionSplitDao.deleteSplitsByTransactionId(id)
            if (isSplit && splits.isNotEmpty()) {
                val splitEntities = splits.map {
                    TransactionSplitEntity(
                        transactionId = id,
                        name = it.name.trim(),
                        phoneNumber = it.phoneNumber.trim(),
                        amount = CurrencyConverter.roundToTwoDecimals(it.amount),
                        isCurrentUser = it.isCurrentUser,
                        isPaid = it.isPaid,
                        paidDate = if (it.isPaid) System.currentTimeMillis() else null
                    )
                }
                transactionSplitDao.insertSplits(splitEntities)
            }
        }
    }

    suspend fun recalculateConvertedAmounts(newBaseCurrency: String) {
        val transactions = transactionDao.getAllTransactionEntities()
        if (transactions.isEmpty()) return
        val targetBase = newBaseCurrency.uppercase().trim()
        val rateCache = mutableMapOf<String, Double>()
        val updated = mutableListOf<TransactionEntity>()

        for (tx in transactions) {
            val txCurrency = tx.currency.uppercase().trim()
            val rate = if (txCurrency == targetBase) {
                1.0
            } else {
                rateCache.getOrPut(txCurrency) {
                    exchangeRateRepository.getRate(txCurrency, targetBase).rate
                }
            }
            val newConverted = CurrencyConverter.convertAmount(tx.amount, rate)
            if (kotlin.math.abs(tx.convertedAmount - newConverted) > 0.001) {
                updated.add(tx.copy(convertedAmount = newConverted, updatedAt = System.currentTimeMillis()))
            }
        }

        if (updated.isNotEmpty()) {
            transactionDao.updateTransactions(updated)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransactionById(transaction.id)
        appDatabase?.checkpointWal()
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
        appDatabase?.checkpointWal()
    }

    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getTotalIncome(startDate, endDate)
    }

    fun getTotalExpense(startDate: Long, endDate: Long): Flow<Double?> {
        return transactionDao.getTotalExpense(startDate, endDate)
    }

    fun getOverallTotalIncome(): Flow<Double?> {
        return transactionDao.getOverallTotalIncome()
    }

    fun getOverallTotalExpense(): Flow<Double?> {
        return transactionDao.getOverallTotalExpense()
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
        appDatabase?.vacuumAndCheckpoint()
    }
}
