package com.example.expensetracker.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.dashboard.EmptyStateCard
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.utils.CategoryIconMapper
import com.example.expensetracker.utils.ValidationUtils

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToAddTransaction: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Month & Year Navigator
        com.example.expensetracker.ui.components.MonthNavigatorBar(
            selectedMonth = uiState.selectedMonth,
            selectedYear = uiState.selectedYear,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onCurrentMonth = viewModel::goToCurrentMonth,
            onMonthYearSelected = viewModel::onMonthYearSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!uiState.isLoading && uiState.isEmptyState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 160.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    message = "No transactions recorded for this month.",
                    buttonText = "Add Transaction",
                    onButtonClick = onNavigateToAddTransaction
                )
            }
        } else if (!uiState.isEmptyState) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Net Savings Card
            item {
                com.example.expensetracker.ui.components.AnimatedScrollEntry(index = 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Monthly Financial Insights",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Net Savings",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.90f),
                                        maxLines = 1
                                    )
                                    com.example.expensetracker.ui.components.RollingOdometerText(
                                        amount = uiState.netSavings,
                                        currency = uiState.baseCurrency,
                                        animateInitial = false,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (uiState.netSavings >= 0) AppColors.income else AppColors.expense
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Savings Rate",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.90f),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${String.format("%.1f", uiState.savingsRate)}%",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Income vs Expense Comparison Bar Chart
            item {
                com.example.expensetracker.ui.components.AnimatedScrollEntry(index = 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Income vs Expense Ratio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val totalFlow = uiState.totalIncome + uiState.totalExpense
                            val incomeRatio = if (totalFlow > 0 && !totalFlow.isNaN()) {
                                (uiState.totalIncome / totalFlow).toFloat().coerceIn(0.01f, 0.99f)
                            } else 0.5f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(11.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(incomeRatio)
                                        .fillMaxHeight()
                                        .background(AppColors.safeGreen)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f - incomeRatio)
                                        .fillMaxHeight()
                                        .background(AppColors.expense)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Income: ${ValidationUtils.formatCurrency(uiState.totalIncome, uiState.baseCurrency)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AppColors.income,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Expense: ${ValidationUtils.formatCurrency(uiState.totalExpense, uiState.baseCurrency)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AppColors.expense,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Top Spending Categories
            if (uiState.topCategories.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Spending Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(uiState.topCategories, key = { _, it -> "top_${it.categoryName}" }) { index, item ->
                    com.example.expensetracker.ui.components.AnimatedScrollEntry(
                        index = index + 2,
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
                        TopCategoryItem(item = item, currency = uiState.baseCurrency)
                    }
                }
            }

            // Full Category Breakdown
            if (uiState.categoryReportItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete Category Distribution",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(uiState.categoryReportItems, key = { _, it -> "all_${it.categoryName}" }) { index, item ->
                    com.example.expensetracker.ui.components.AnimatedScrollEntry(
                        index = index + 2 + uiState.topCategories.size,
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
                        CategoryReportRow(item = item, currency = uiState.baseCurrency)
                    }
                }
            }

            item(key = "end_of_reports") {
                com.example.expensetracker.ui.components.EndOfPageIndicator(
                    message = "End of report summary"
                )
            }
        }
    }
}
}

@Composable
fun TopCategoryItem(item: CategoryReportItem, currency: String) {
    val icon = CategoryIconMapper.getIcon(item.categoryIcon)

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format("%.1f", item.percentage)}% of total monthly expenses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Text(
                text = ValidationUtils.formatCurrency(item.totalAmount, currency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryReportRow(item: CategoryReportItem, currency: String) {
    val icon = CategoryIconMapper.getIcon(item.categoryIcon)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${ValidationUtils.formatCurrency(item.totalAmount, currency)} (${String.format("%.1f", item.percentage)}%)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (item.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )
        }
    }
}
