package com.example.expensetracker.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.local.model.TransactionWithCategory
import com.example.expensetracker.ui.dashboard.EmptyStateCard
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.CategoryIconMapper
import com.example.expensetracker.utils.SplitReminderUtils
import com.example.expensetracker.utils.ValidationUtils
import com.example.expensetracker.utils.rememberAppHapticFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val chipsScrollState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberAppHapticFeedback()
    val coroutineScope = rememberCoroutineScope()

    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<TransactionWithCategory?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-scroll to top instantly whenever transactions list or filter criteria changes
    val latestTxId = uiState.transactions.firstOrNull()?.transaction?.id
    val txCount = uiState.transactions.size

    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(latestTxId, txCount, uiState.filterCriteria) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        if (uiState.transactions.isNotEmpty() && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Input Row with Filter Sheet Trigger Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.filterCriteria.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Search note, split participant, category, date...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.filterCriteria.searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                }
                                viewModel.onSearchQueryChanged("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                // Filter Sheet Trigger Button with Active Filter Count Badge
                Surface(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Light)
                        showFilterSheet = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (uiState.filterCriteria.activeFilterCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.filterCriteria.activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BadgedBox(
                            badge = {
                                if (uiState.filterCriteria.activeFilterCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${uiState.filterCriteria.activeFilterCount}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Smart Filters",
                                tint = if (uiState.filterCriteria.activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Horizontally Scrollable Filter Chips Row with Sticky Clear All Button on the Right

            // Auto-scroll towards active filter tags when filters are applied, or instant reset when cleared
            LaunchedEffect(uiState.filterCriteria.activeFilterCount) {
                if (uiState.filterCriteria.activeFilterCount > 0) {
                    chipsScrollState.animateScrollToItem(3.coerceAtMost(chipsScrollState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                } else {
                    chipsScrollState.scrollToItem(0)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable Type Filter Chips & Active Filter Tags
                LazyRow(
                    state = chipsScrollState,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = uiState.filterCriteria.type == "ALL",
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                viewModel.onTypeFilterSelected("ALL")
                            },
                            label = { Text("All", fontWeight = FontWeight.Bold) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filterCriteria.type == "INCOME",
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                viewModel.onTypeFilterSelected("INCOME")
                            },
                            label = { Text("Income", fontWeight = FontWeight.Bold) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filterCriteria.type == "EXPENSE",
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                viewModel.onTypeFilterSelected("EXPENSE")
                            },
                            label = { Text("Expense", fontWeight = FontWeight.Bold) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.filterCriteria.isSplitOnly,
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                viewModel.onToggleSplitFilter()
                            },
                            label = { Text("Split Bill", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    // Dynamic Category Filter Tag
                    if (uiState.filterCriteria.categoryId != null) {
                        val catName = uiState.categories.find { it.id == uiState.filterCriteria.categoryId }?.name ?: "Category"
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {
                                    haptic.perform(AppHapticFeedbackType.Light)
                                    viewModel.onCategoryFilterSelected(null)
                                },
                                label = { Text("Cat: $catName", fontWeight = FontWeight.SemiBold) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove category filter", modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }

                    // Dynamic Date Range Filter Tag
                    if (uiState.filterCriteria.datePreset != DateRangePreset.ALL_TIME) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {
                                    haptic.perform(AppHapticFeedbackType.Light)
                                    viewModel.onDatePresetSelected(DateRangePreset.ALL_TIME)
                                },
                                label = { Text("Date: ${uiState.filterCriteria.datePreset.label}", fontWeight = FontWeight.SemiBold) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove date filter", modifier = Modifier.size(14.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }

                    // Dynamic Payment Method Tag
                    if (uiState.filterCriteria.paymentMethod != "ALL") {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {
                                    haptic.perform(AppHapticFeedbackType.Light)
                                    viewModel.onPaymentMethodFilterSelected("ALL")
                                },
                                label = { Text("Pay: ${uiState.filterCriteria.paymentMethod}", fontWeight = FontWeight.SemiBold) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove payment filter", modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    // Dynamic Amount Range Tag
                    if (uiState.filterCriteria.minAmount != null || uiState.filterCriteria.maxAmount != null) {
                        val minText = uiState.filterCriteria.minAmount?.let { "$$it" } ?: "$0"
                        val maxText = uiState.filterCriteria.maxAmount?.let { "$$it" } ?: "∞"
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {
                                    haptic.perform(AppHapticFeedbackType.Light)
                                    viewModel.onAmountRangeChanged(null, null)
                                },
                                label = { Text("Amt: $minText - $maxText", fontWeight = FontWeight.SemiBold) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove amount filter", modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    // Dynamic Sort Order Tag (if not default)
                    if (uiState.filterCriteria.sortOrder != TransactionSortOrder.DATE_DESC) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {
                                    haptic.perform(AppHapticFeedbackType.Light)
                                    viewModel.onSortOrderSelected(TransactionSortOrder.DATE_DESC)
                                },
                                label = { Text("Sort: ${uiState.filterCriteria.sortOrder.label}", fontWeight = FontWeight.SemiBold) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Reset sort", modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }

                // Pinned Sticky "Clear All" Pill on the RIGHT side
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.filterCriteria.isFiltered,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.height(26.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Medium)
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                    chipsScrollState.scrollToItem(0)
                                }
                                viewModel.clearAllFiltersAndSearch()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.40f)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear All",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Clear All",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Premium Results Counter & Financial Summary Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Record Count Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "${uiState.totalCount} ${if (uiState.totalCount == 1) "record" else "records"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Right: Financial Summary Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.filteredIncome > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppColors.incomeContainer.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    AppColors.income.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = AppColors.income,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "+${ValidationUtils.formatCurrency(uiState.filteredIncome, uiState.baseCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.income
                                    )
                                }
                            }
                        }

                        if (uiState.filteredExpense > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppColors.expenseContainer.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    AppColors.expense.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = AppColors.expense,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "-${ValidationUtils.formatCurrency(uiState.filteredExpense, uiState.baseCurrency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.expense
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transactions List / Empty State
            if (uiState.transactions.isEmpty()) {
                if (!uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 160.dp + navBarBottom),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            message = if (uiState.filterCriteria.isFiltered) {
                                "No transactions match your smart search and filter criteria."
                            } else {
                                "No transactions recorded yet."
                            },
                            buttonText = if (uiState.filterCriteria.isFiltered) "Reset Filters" else "Add Transaction",
                            onButtonClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                if (uiState.filterCriteria.isFiltered) {
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                        chipsScrollState.scrollToItem(0)
                                    }
                                    viewModel.clearAllFiltersAndSearch()
                                } else {
                                    onNavigateToAddTransaction()
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp + navBarBottom),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = uiState.transactions,
                        key = { _, item -> item.transaction.id }
                    ) { index, txWithCat ->
                        com.example.expensetracker.ui.components.AnimatedScrollEntry(
                            index = index,
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                placementSpec = null,
                                fadeOutSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 200,
                                    easing = androidx.compose.animation.core.FastOutLinearInEasing
                                )
                            )
                        ) {
                            TransactionListItem(
                                txWithCat = txWithCat,
                                baseCurrency = uiState.baseCurrency,
                                onClick = {
                                    selectedTxForDetail = txWithCat
                                },
                                onEditClick = {
                                    haptic.perform(AppHapticFeedbackType.Light)
                                    onNavigateToEditTransaction(txWithCat.transaction.id)
                                },
                                onDeleteClick = {
                                    haptic.perform(AppHapticFeedbackType.Heavy)
                                    viewModel.requestDeleteTransaction(txWithCat)
                                }
                            )
                        }
                    }

                    item(key = "end_of_transactions") {
                        com.example.expensetracker.ui.components.EndOfPageIndicator(
                            message = "End of transactions"
                        )
                    }
                }
            }
        }

        // Multi-Criteria Smart Filter Modal Bottom Sheet
        if (showFilterSheet) {
            SmartFilterSheet(
                sheetState = sheetState,
                initialCriteria = uiState.filterCriteria,
                categories = uiState.categories,
                baseCurrency = uiState.baseCurrency,
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showFilterSheet = false
                    }
                },
                onApply = { newCriteria ->
                    viewModel.applyFilterCriteria(newCriteria)
                    coroutineScope.launch {
                        sheetState.hide()
                        showFilterSheet = false
                    }
                },
                onReset = {
                    coroutineScope.launch {
                        listState.scrollToItem(0)
                        chipsScrollState.scrollToItem(0)
                    }
                    viewModel.resetAllFilters()
                }
            )
        }

        // Transaction Details Modal Bottom Sheet
        selectedTxForDetail?.let { txWithCat ->
            val currentTxWithCat = uiState.transactions.find { it.transaction.id == txWithCat.transaction.id } ?: txWithCat
            TransactionDetailSheet(
                txWithCat = currentTxWithCat,
                baseCurrency = uiState.baseCurrency,
                onDismiss = { selectedTxForDetail = null },
                onEdit = {
                    selectedTxForDetail = null
                    onNavigateToEditTransaction(currentTxWithCat.transaction.id)
                },
                onDelete = {
                    selectedTxForDetail = null
                    viewModel.requestDeleteTransaction(currentTxWithCat)
                },
                onTogglePaidStatus = { splitId, isPaid ->
                    viewModel.toggleSplitPaidStatus(splitId, isPaid)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Delete Confirmation Dialog
        if (uiState.transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    haptic.perform(AppHapticFeedbackType.Light)
                    viewModel.dismissDeleteDialog()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = { Text("Delete Transaction?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to permanently delete this transaction? Your balance, reports, and monthly budget will be updated immediately.", style = MaterialTheme.typography.bodyLarge) },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Heavy)
                            viewModel.confirmDeleteTransaction()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        haptic.perform(AppHapticFeedbackType.Light)
                        viewModel.dismissDeleteDialog()
                    }) {
                        Text("Cancel", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionListItem(
    txWithCat: TransactionWithCategory,
    baseCurrency: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val tx = txWithCat.transaction
    val isIncome = tx.type == "INCOME"
    val icon = CategoryIconMapper.getIcon(txWithCat.category?.icon ?: "more_horiz")
    val haptic = rememberAppHapticFeedback()

    Card(
        onClick = {
            haptic.perform(AppHapticFeedbackType.Light)
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Top Row: Icon, Category Name, Date, and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = txWithCat.category?.name ?: "Uncategorized",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tx.isSplit) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Split",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = ValidationUtils.formatDate(tx.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    val prefix = if (isIncome) "+" else "-"
                    val amountText = "$prefix ${ValidationUtils.formatCurrency(tx.convertedAmount, baseCurrency)}"
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isIncome) AppColors.income else AppColors.expense,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )

                    if (tx.isSplit) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Your share",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    } else if (tx.currency != baseCurrency) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "(${ValidationUtils.formatCurrency(tx.amount, tx.currency)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }
                }
            }

            // Dedicated Split Bill Summary Banner with clean alignment & high visibility
            if (tx.isSplit) {
                val splitCount = txWithCat.splits.size.coerceAtLeast(2)
                val otherParticipants = txWithCat.splits.filter { !it.isCurrentUser }
                val unpaidCount = otherParticipants.count { !it.isPaid }
                val isAllSettled = otherParticipants.isNotEmpty() && unpaidCount == 0

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Total bill & participant count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            val totalBillText = if (tx.originalTotalAmount > 0.0) {
                                ValidationUtils.formatCurrency(tx.originalTotalAmount, tx.currency)
                            } else {
                                val sum = tx.amount + otherParticipants.sumOf { it.amount }
                                ValidationUtils.formatCurrency(sum, tx.currency)
                            }
                            Text(
                                text = "Total Bill: $totalBillText",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "($splitCount people)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Right: Settlement status badge
                        if (isAllSettled) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppColors.incomeContainer.copy(alpha = 0.70f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = AppColors.income
                                    )
                                    Text(
                                        text = "All settled",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.income
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "$unpaidCount pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Note preview bubble if a note exists
            if (tx.note.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = tx.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bottom Bar: Payment Method Tag (Fully visible, never squished) & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Method Tag with dedicated space for complete text like "Bank Transfer"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = getPaymentMethodIcon(tx.paymentMethod),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tx.paymentMethod,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Quick Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            onEditClick()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Heavy)
                            onDeleteClick()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    txWithCat: TransactionWithCategory,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePaidStatus: (Long, Boolean) -> Unit = { _, _ -> }
) {
    val tx = txWithCat.transaction
    val isIncome = tx.type == "INCOME"
    val icon = CategoryIconMapper.getIcon(txWithCat.category?.icon ?: "more_horiz")
    val haptic = rememberAppHapticFeedback()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = detailSheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            // Respects system status bar / camera notch so header never overlaps with top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Category Icon, Title, Type Badge, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = txWithCat.category?.name ?: "Uncategorized",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isIncome) AppColors.incomeContainer else AppColors.expenseContainer
                    ) {
                        Text(
                            text = if (isIncome) "Income" else "Expense",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) AppColors.income else AppColors.expense
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Big Amount Hero Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (tx.isSplit) "Your Share (Recorded Expense)" else "Total Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val prefix = if (isIncome) "+" else "-"
                    Text(
                        text = "$prefix ${ValidationUtils.formatCurrency(tx.convertedAmount, baseCurrency)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isIncome) AppColors.income else AppColors.expense
                    )
                    if (tx.isSplit && tx.originalTotalAmount > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original Bill: ${ValidationUtils.formatCurrency(tx.originalTotalAmount, tx.currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (tx.currency != baseCurrency) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Original: ${ValidationUtils.formatCurrency(tx.amount, tx.currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Details Information Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Date",
                        value = ValidationUtils.formatDate(tx.date, "EEEE, MMMM dd, yyyy")
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    DetailRow(
                        icon = getPaymentMethodIcon(tx.paymentMethod),
                        label = "Payment Method",
                        value = tx.paymentMethod
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    DetailRow(
                        icon = Icons.Default.Category,
                        label = "Category",
                        value = txWithCat.category?.name ?: "Uncategorized"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    DetailRow(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        label = "Transaction ID",
                        value = "#${tx.id}"
                    )
                }
            }

            // Note Section: Fully displays note text without any truncation
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Note / Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (tx.note.isNotBlank()) {
                        Text(
                            text = tx.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    } else {
                        Text(
                            text = "No note attached to this transaction.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Split Bill & Participants Breakdown Section
            if (tx.isSplit && txWithCat.splits.isNotEmpty()) {
                val otherParticipants = txWithCat.splits.filter { !it.isCurrentUser }
                val totalBill = if (tx.originalTotalAmount > 0.0) tx.originalTotalAmount else (tx.amount + otherParticipants.sumOf { it.amount })
                val totalToCollect = otherParticipants.sumOf { it.amount }
                val totalCollected = otherParticipants.filter { it.isPaid }.sumOf { it.amount }
                val progress = if (totalToCollect > 0.0) (totalCollected / totalToCollect).toFloat().coerceIn(0f, 1f) else 1f
                val unpaidCount = otherParticipants.count { !it.isPaid }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Split Bill (${txWithCat.splits.size} People)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (unpaidCount == 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AppColors.incomeContainer
                                ) {
                                    Text(
                                        text = "All Settled",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.income
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "$unpaidCount Pending",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Settlement Progress Summary
                        if (totalToCollect > 0.0) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Collected: ${ValidationUtils.formatCurrency(totalCollected, tx.currency)} / ${ValidationUtils.formatCurrency(totalToCollect, tx.currency)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progress >= 1f) AppColors.income else MaterialTheme.colorScheme.primary
                                    )
                                }
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (progress >= 1f) AppColors.income else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Participant Rows
                        txWithCat.splits.forEach { split ->
                            val isUser = split.isCurrentUser
                            val formattedAmount = ValidationUtils.formatCurrency(split.amount, tx.currency)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
                                       else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                           else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left: Avatar + Name + Formatted Phone
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isUser) MaterialTheme.colorScheme.primaryContainer
                                                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isUser) "Y" else split.name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUser) MaterialTheme.colorScheme.primary
                                                           else MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }

                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isUser) "You" else split.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (isUser) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "Organizer",
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }

                                                if (split.phoneNumber.isNotBlank()) {
                                                    Text(
                                                        text = SplitReminderUtils.formatPhoneForDisplay(split.phoneNumber),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else if (isUser) {
                                                    Text(
                                                        text = "Your share of the bill",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Right: Amount + Proper Interactive Status Toggle Pill
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = formattedAmount,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )

                                            if (isUser) {
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = AppColors.incomeContainer.copy(alpha = 0.6f),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .padding(horizontal = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(12.dp),
                                                            tint = AppColors.income
                                                        )
                                                        Text(
                                                            text = "Paid by you",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = AppColors.income
                                                        )
                                                    }
                                                }
                                            } else {
                                                // Minimalist & Tactile Proper Paid / Unpaid Toggle Pill Button
                                                Surface(
                                                    onClick = {
                                                        haptic.perform(AppHapticFeedbackType.Medium)
                                                        onTogglePaidStatus(split.id, split.isPaid)
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = if (split.isPaid) {
                                                        AppColors.incomeContainer.copy(alpha = 0.65f)
                                                    } else {
                                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.50f)
                                                    },
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (split.isPaid) AppColors.income.copy(alpha = 0.40f)
                                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                                                    ),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .padding(horizontal = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (split.isPaid) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(13.dp),
                                                            tint = if (split.isPaid) AppColors.income else MaterialTheme.colorScheme.error
                                                        )
                                                        Text(
                                                            text = if (split.isPaid) "Paid" else "Unpaid",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (split.isPaid) AppColors.income else MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Reminder Action Buttons (WhatsApp / SMS) with Increased Height & Touch Targets
                                    if (!isUser && !split.isPaid && split.phoneNumber.isNotBlank()) {
                                        val catName = txWithCat.category?.name ?: "Expense"
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        val totalBillAmount = if (tx.originalTotalAmount > 0.0) {
                                            ValidationUtils.formatCurrency(tx.originalTotalAmount, tx.currency)
                                        } else {
                                            ValidationUtils.formatCurrency(tx.amount, tx.currency)
                                        }
                                        val txDateStr = ValidationUtils.formatDate(tx.date, "dd MMM yyyy")
                                        val txNote = tx.note.trim()

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            thickness = 0.8.dp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // WhatsApp Reminder Button
                                            Surface(
                                                onClick = {
                                                    haptic.perform(AppHapticFeedbackType.Light)
                                                    SplitReminderUtils.sendWhatsAppReminder(
                                                        context = context,
                                                        name = split.name,
                                                        phone = split.phoneNumber,
                                                        formattedAmount = formattedAmount,
                                                        categoryName = catName,
                                                        totalAmount = totalBillAmount,
                                                        dateStr = txDateStr,
                                                        note = txNote
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF25D366).copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = androidx.compose.ui.res.painterResource(id = com.example.expensetracker.R.drawable.ic_whatsapp),
                                                        contentDescription = "WhatsApp",
                                                        tint = Color(0xFF1EBE5D),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "WhatsApp",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1EBE5D)
                                                    )
                                                }
                                            }

                                            // SMS Reminder Button
                                            Surface(
                                                onClick = {
                                                    haptic.perform(AppHapticFeedbackType.Light)
                                                    SplitReminderUtils.sendSmsReminder(
                                                        context = context,
                                                        name = split.name,
                                                        phone = split.phoneNumber,
                                                        formattedAmount = formattedAmount,
                                                        categoryName = catName
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = androidx.compose.ui.res.painterResource(id = com.example.expensetracker.R.drawable.ic_sms_message),
                                                        contentDescription = "SMS",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "SMS",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    } else if (!isUser && split.isPaid) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppColors.incomeContainer.copy(alpha = 0.35f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = AppColors.income,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Settled • Share collected",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AppColors.income,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Edit & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Light)
                        onEdit()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Heavy)
                        onDelete()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getPaymentMethodIcon(method: String): ImageVector {
    return when (method.trim().lowercase()) {
        "cash" -> Icons.Default.Payments
        "credit card" -> Icons.Default.CreditCard
        "debit card" -> Icons.Default.AccountBalanceWallet
        "bank transfer" -> Icons.Default.AccountBalance
        else -> Icons.Default.CreditCard
    }
}
