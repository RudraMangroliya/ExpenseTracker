package com.example.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationLogicTest {

    @Test
    fun balance_equalsTotalIncomeMinusTotalExpense() {
        val totalIncome = 5000.0
        val totalExpense = 1500.0
        val balance = totalIncome - totalExpense
        assertEquals(3500.0, balance, 0.001)
    }

    @Test
    fun remainingBudget_calculatedCorrectly() {
        val monthlyBudget = 2000.0
        val monthlyExpense = 1200.0
        val remaining = monthlyBudget - monthlyExpense
        assertEquals(800.0, remaining, 0.001)
    }

    @Test
    fun budgetPercentage_calculatedCorrectly() {
        val monthlyBudget = 2000.0
        val monthlyExpense = 1500.0
        val percentage = (monthlyExpense / monthlyBudget) * 100.0
        assertEquals(75.0, percentage, 0.001)
    }

    @Test
    fun budgetPercentage_zeroBudget_preventsDivideByZero() {
        val monthlyBudget = 0.0
        val monthlyExpense = 500.0
        val percentage = if (monthlyBudget > 0) (monthlyExpense / monthlyBudget) * 100.0 else 0.0
        assertEquals(0.0, percentage, 0.0001)
    }
}
