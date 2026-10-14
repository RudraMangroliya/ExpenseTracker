package com.example.expensetracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.expensetracker.ui.components.AnimatedScrollEntry
import com.example.expensetracker.ui.components.BudgetStatusLine
import com.example.expensetracker.ui.components.EndOfPageIndicator
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.local.model.TransactionWithCategory
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.utils.CategoryIconMapper
import com.example.expensetracker.utils.ValidationUtils

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 16.dp,
            bottom = 96.dp + navBarBottom
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Overview Card
        item(key = "balance_card", contentType = "overview_card") {
            AnimatedScrollEntry(
                index = 0,
                delayMillis = 0
            ) {
                BalanceCard(
                    balance = uiState.balance,
                    income = uiState.totalIncome,
                    expense = uiState.totalExpense,
                    currency = uiState.baseCurrency,
                    isLoading = uiState.isLoading,
                    onAddClick = onNavigateToAddTransaction
                )
            }
        }

        // Monthly Budget Status
        item(key = "budget_card", contentType = "overview_card") {
            AnimatedScrollEntry(
                index = 1,
                delayMillis = 50
            ) {
                MonthlyBudgetCard(
                    monthlyExpense = uiState.monthlyExpense,
                    monthlyBudget = uiState.monthlyBudget,
                    remainingBudget = uiState.remainingBudget,
                    currency = uiState.baseCurrency,
                    isLoading = uiState.isLoading,
                    onSetBudgetClick = onNavigateToBudgets
                )
            }
        }

        // Recent Transactions Section Header
        item(key = "recent_transactions_header", contentType = "section_header") {
            AnimatedScrollEntry(
                index = 2,
                delayMillis = 100
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = onNavigateToTransactions) {
                        Text("View All", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Recent Transactions List
        if (uiState.recentTransactions.isEmpty()) {
            if (!uiState.isLoading) {
                item(key = "empty_transactions", contentType = "empty_state") {
                    EmptyStateCard(
                        message = "No transactions recorded yet.",
                        buttonText = "Add First Transaction",
                        onButtonClick = onNavigateToAddTransaction
                    )
                }
            }
        } else {
            itemsIndexed(
                items = uiState.recentTransactions,
                key = { _, it -> "recent_${it.transaction.id}" },
                contentType = { _, _ -> "recent_transaction" }
            ) { index, item ->
                AnimatedScrollEntry(
                    index = index + 3,
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        placementSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        fadeOutSpec = androidx.compose.animation.core.tween(
                            durationMillis = 280,
                            easing = androidx.compose.animation.core.FastOutLinearInEasing
                        )
                    )
                ) {
                    TransactionRowItem(
                        txWithCat = item,
                        baseCurrency = uiState.baseCurrency
                    )
                }
            }
        }

        // Category Spending Overview
        if (uiState.categorySpendingList.isNotEmpty()) {
            item(key = "category_spending_header", contentType = "section_header") {
                AnimatedScrollEntry(
                    index = uiState.recentTransactions.size + 3
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Category Spending Breakdown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            itemsIndexed(
                items = uiState.categorySpendingList,
                key = { _, it -> "cat_spend_${it.categoryName}" },
                contentType = { _, _ -> "category_spending" }
            ) { index, catSpend ->
                AnimatedScrollEntry(
                    index = uiState.recentTransactions.size + 4 + index,
                    modifier = Modifier.animateItem(
                        fadeInSpec = androidx.compose.animation.core.tween(
                            durationMillis = 520,
                            easing = androidx.compose.animation.core.CubicBezierEasing(0.14f, 0.94f, 0.22f, 1.0f)
                        ),
                        placementSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                        ),
                        fadeOutSpec = androidx.compose.animation.core.tween(
                            durationMillis = 280,
                            easing = androidx.compose.animation.core.FastOutLinearInEasing
                        )
                    )
                ) {
                    CategorySpendingRow(
                        catSpend = catSpend,
                        currency = uiState.baseCurrency
                    )
                }
            }
        }

        item(key = "end_of_dashboard", contentType = "end_indicator") {
            EndOfPageIndicator(
                message = "You're all caught up"
            )
        }
    }
}

@Composable
fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    currency: String,
    isLoading: Boolean = false,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.90f)
            )
            com.example.expensetracker.ui.components.RollingOdometerText(
                amount = balance,
                currency = currency,
                animateInitial = false,
                animateChanges = true,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Income
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AppColors.incomeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Income",
                            tint = AppColors.income,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                        com.example.expensetracker.ui.components.RollingOdometerText(
                            amount = income,
                            currency = currency,
                            animateInitial = false,
                            animateChanges = true,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.income
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Expense
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AppColors.expenseContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Expense",
                            tint = AppColors.expense,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                        com.example.expensetracker.ui.components.RollingOdometerText(
                            amount = expense,
                            currency = currency,
                            animateInitial = false,
                            animateChanges = true,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.expense
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val haptic = com.example.expensetracker.utils.rememberAppHapticFeedback()

            Button(
                onClick = {
                    haptic.perform(com.example.expensetracker.utils.AppHapticFeedbackType.Light)
                    onAddClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Income / Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun MonthlyBudgetCard(
    monthlyExpense: Double,
    monthlyBudget: Double,
    remainingBudget: Double,
    currency: String,
    isLoading: Boolean = false,
    onSetBudgetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monthly Spending",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSetBudgetClick,
                    modifier = Modifier.widthIn(min = 96.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (monthlyBudget > 0) "Manage" else "Set Limit",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val progress = if (monthlyBudget > 0) (monthlyExpense / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
            val progressColor = when {
                monthlyBudget <= 0 -> MaterialTheme.colorScheme.primary
                progress >= 1.0f -> AppColors.expense
                progress >= 0.8f -> AppColors.warning
                else -> AppColors.safeGreen
            }

            BudgetStatusLine(
                progress = progress,
                progressColor = progressColor,
                height = 14.dp,
                trackColor = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ${ValidationUtils.formatCurrency(monthlyExpense, currency)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (monthlyBudget > 0) {
                    Text(
                        text = "Budget: ${ValidationUtils.formatCurrency(monthlyBudget, currency)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "No limit set",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    txWithCat: TransactionWithCategory,
    baseCurrency: String,
    modifier: Modifier = Modifier
) {
    val tx = txWithCat.transaction
    val isIncome = tx.type == "INCOME"
    val icon = CategoryIconMapper.getIcon(txWithCat.category?.icon ?: "more_horiz")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isIncome) AppColors.incomeContainer else AppColors.expenseContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isIncome) AppColors.income else AppColors.expense,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = txWithCat.category?.name ?: "Uncategorized",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = if (tx.note.isNotBlank()) "${tx.note} • ${tx.paymentMethod}" else "${ValidationUtils.formatDate(tx.date)} • ${tx.paymentMethod}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (isIncome) "+" else "-"
                val amountText = "$prefix ${ValidationUtils.formatCurrency(tx.convertedAmount, baseCurrency)}"
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isIncome) AppColors.income else AppColors.expense,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (tx.currency != baseCurrency) {
                    Text(
                        text = "(${ValidationUtils.formatCurrency(tx.amount, tx.currency)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySpendingRow(
    catSpend: CategorySpending,
    currency: String,
    modifier: Modifier = Modifier
) {
    val icon = CategoryIconMapper.getIcon(catSpend.categoryIcon)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = catSpend.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ValidationUtils.formatCurrency(catSpend.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (catSpend.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (buttonText != null && onButtonClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onButtonClick) {
                    Text(buttonText)
                }
            }
        }
    }
}
