package com.example.expensetracker.ui.budget

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.components.BudgetStatusLine
import com.example.expensetracker.ui.components.RollingOdometerText
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.utils.CategoryIconMapper
import com.example.expensetracker.utils.ValidationUtils
import kotlin.math.roundToInt

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

@Composable
fun BudgetsScreen(
    viewModel: BudgetViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    val maxScrollDistancePx = remember(density) { with(density) { 110.dp.toPx() } }
    var cachedHeaderHeightPx by remember { mutableIntStateOf(0) }

    // Continuous, mathematically seamless cumulative scroll tracking across all list items
    val scrollProgress by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val item0 = visibleItems.firstOrNull { it.index == 0 }
            if (item0 != null && item0.size > 0) {
                cachedHeaderHeightPx = item0.size
            }
            val h0 = if (cachedHeaderHeightPx > 0) {
                cachedHeaderHeightPx.toFloat()
            } else {
                with(density) { 38.dp.toPx() }
            }

            val totalScrolled = when (listState.firstVisibleItemIndex) {
                0 -> listState.firstVisibleItemScrollOffset.toFloat()
                1 -> h0 + listState.firstVisibleItemScrollOffset.toFloat()
                else -> maxScrollDistancePx
            }
            (totalScrolled / maxScrollDistancePx).coerceIn(0f, 1f)
        }
    }

    // High-responsiveness Spring (1200f) eliminates lag while keeping 120fps buttery smoothness
    val animatedProgress by animateFloatAsState(
        targetValue = scrollProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 1200f
        ),
        label = "scrollCollapseProgress"
    )

    val progress = if (uiState.overallBudget > 0) {
        (uiState.overallSpent / uiState.overallBudget).toFloat().coerceIn(0f, 1f)
    } else 0f

    val progressColor = when {
        uiState.overallBudget <= 0 -> MaterialTheme.colorScheme.primary
        uiState.overallPercentage >= 100.0 -> AppColors.expense
        uiState.overallPercentage >= 80.0 -> AppColors.warning
        else -> AppColors.safeGreen
    }

    val cardPaddingV = lerpDp(14.dp, 8.dp, animatedProgress)
    val cardPaddingH = lerpDp(18.dp, 12.dp, animatedProgress)
    val cardCornerRadius = lerpDp(18.dp, 14.dp, animatedProgress)
    val cardElevation = lerpDp(0.dp, 2.dp, animatedProgress)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Month Selector Bar (Previous / Next / Current / Picker)
        com.example.expensetracker.ui.components.MonthNavigatorBar(
            selectedMonth = uiState.selectedMonth,
            selectedYear = uiState.selectedYear,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onCurrentMonth = viewModel::goToCurrentMonth,
            onMonthYearSelected = viewModel::onMonthYearSelected
        )

        // Ultra-Smooth GPU-Assisted Collapsing Budget Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = cardPaddingH, vertical = cardPaddingV)
                    .clipToBounds()
            ) {
                CollapsingBudgetCardContent(
                    animatedProgress = animatedProgress,
                    uiState = uiState,
                    progress = progress,
                    progressColor = progressColor,
                    onEditBudget = { viewModel.openSetBudgetDialog(null, uiState.overallBudget) }
                )
            }
        }

        // Category Budgets List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 96.dp)
        ) {
            item(key = "category_budget_header") {
                Text(
                    text = "Category Budget Targets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            if (uiState.categoryBudgets.isEmpty()) {
                item(key = "empty_categories") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No category budgets available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(uiState.categoryBudgets, key = { _, it -> it.category.id }) { index, item ->
                    com.example.expensetracker.ui.components.AnimatedScrollEntry(
                        index = index,
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
                        CategoryBudgetRowItem(
                            model = item,
                            currency = uiState.baseCurrency,
                            onSetBudget = { viewModel.openSetBudgetDialog(item.category.id, item.budgetAmount) }
                        )
                    }
                }
            }

            item(key = "end_of_budgets") {
                com.example.expensetracker.ui.components.EndOfPageIndicator(
                    message = "All budgets loaded"
                )
            }
        }
    }

    // Set/Edit Budget Dialog
    if (uiState.showSetBudgetDialog) {
        val titleText = if (uiState.editingCategoryId == null) "Set Overall Monthly Budget" else "Set Category Budget"
        AlertDialog(
            onDismissRequest = viewModel::closeSetBudgetDialog,
            title = { Text(titleText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val promptText = if (uiState.editingCategoryId == null) {
                        "Enter overall monthly budget limit:"
                    } else {
                        "Enter monthly budget limit for this category:"
                    }
                    Text(promptText, style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = uiState.dialogAmountInput,
                        onValueChange = viewModel::onDialogAmountInputChanged,
                        label = { Text("Budget Amount (${uiState.baseCurrency})") },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = uiState.dialogError != null,
                        supportingText = {
                            uiState.dialogError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::saveBudget,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeSetBudgetDialog) {
                    Text("Cancel", style = MaterialTheme.typography.bodyLarge)
                }
            }
        )
    }
}

@Composable
fun CategoryBudgetRowItem(
    model: CategoryBudgetUiModel,
    currency: String,
    onSetBudget: () -> Unit
) {
    val icon = CategoryIconMapper.getIcon(model.category.icon)
    val progress = if (model.budgetAmount > 0) (model.spentAmount / model.budgetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val progressColor = when {
        model.budgetAmount <= 0 -> MaterialTheme.colorScheme.primary
        model.percentage >= 100.0 -> AppColors.expense
        model.percentage >= 80.0 -> AppColors.warning
        else -> AppColors.safeGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = model.category.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Surface(
                    onClick = onSetBudget,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (model.budgetAmount > 0) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = "Set budget",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            BudgetStatusLine(
                progress = progress,
                progressColor = progressColor,
                height = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: ${ValidationUtils.formatCurrency(model.spentAmount, currency)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (model.budgetAmount > 0) {
                        "Target: ${ValidationUtils.formatCurrency(model.budgetAmount, currency)}"
                    } else {
                        "No budget target"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (model.budgetAmount > 0) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (model.budgetAmount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Ultra-Smooth Collapsing Budget Card Content:
 * - Single persistent Button instance that horizontally resizes on scroll (from 120dp to 76dp)
 * - Two-segment BudgetStatusLine matching the target design
 * - GPU-accelerated continuous height collapse without element recreation
 */
@Composable
private fun CollapsingBudgetCardContent(
    animatedProgress: Float,
    uiState: BudgetUiState,
    progress: Float,
    progressColor: Color,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = animatedProgress.coerceIn(0f, 1f)
    val fadeAlpha = (1f - p * 1.35f).coerceIn(0f, 1f)
    val buttonWidth = lerpDp(120.dp, 76.dp, p)
    val buttonHeight = lerpDp(38.dp, 34.dp, p)
    val buttonPaddingH = lerpDp(12.dp, 8.dp, p)
    val buttonPaddingV = lerpDp(8.dp, 6.dp, p)
    val buttonCornerRadius = lerpDp(12.dp, 10.dp, p)
    val buttonIconSize = lerpDp(18.dp, 15.dp, p)
    val buttonGap = lerpDp(6.dp, 4.dp, p)
    val isBudgetSet = uiState.overallBudget > 0
    val showWarning = isBudgetSet && uiState.overallPercentage >= 80.0
    val isExceeded = uiState.overallPercentage >= 100.0

    Layout(
        content = {
            // Child 0: Title Column (Fades out smoothly on scroll)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Overall Monthly Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Target limit for current month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Child 1: The Single, Horizontally-Resizing Button (Never removed / replaced)
            Button(
                onClick = onEditBudget,
                shape = RoundedCornerShape(buttonCornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = buttonPaddingH, vertical = buttonPaddingV),
                modifier = Modifier.size(width = buttonWidth, height = buttonHeight)
            ) {
                Icon(
                    imageVector = if (isBudgetSet) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(buttonIconSize)
                )
                Spacer(modifier = Modifier.width(buttonGap))
                Text(
                    text = if (p < 0.45f) {
                        if (isBudgetSet) "Edit Limit" else "Set Limit"
                    } else {
                        if (isBudgetSet) "Edit" else "Set"
                    },
                    style = if (p < 0.45f) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Child 2: Status Line Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isExceeded) AppColors.expense else AppColors.warning,
                        modifier = Modifier.size(lerpDp(20.dp, 16.dp, p))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                BudgetStatusLine(
                    progress = progress,
                    progressColor = progressColor,
                    height = lerpDp(14.dp, 12.dp, p),
                    trackColor = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            // Child 3: Amounts Row (Fades out smoothly on scroll)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RollingOdometerText(
                        amount = uiState.overallSpent,
                        currency = uiState.baseCurrency,
                        animateInitial = false,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "Monthly Limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isBudgetSet) {
                        RollingOdometerText(
                            amount = uiState.overallBudget,
                            currency = uiState.baseCurrency,
                            animateInitial = false,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "No limit set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Child 4: Warning Banner (if showWarning)
            if (showWarning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isExceeded) AppColors.expense else AppColors.warning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isExceeded) {
                            "Budget exceeded by ${ValidationUtils.formatCurrency(uiState.overallSpent - uiState.overallBudget, uiState.baseCurrency)}!"
                        } else {
                            "Caution: ${String.format("%.1f", uiState.overallPercentage)}% of budget used."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isExceeded) AppColors.expense else AppColors.warning,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth

        // 1. Measure Button (always child 1)
        val buttonPlaceable = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val btnWidth = buttonPlaceable.width
        val btnHeight = buttonPlaceable.height

        // 2. Measure Title (child 0)
        val titleMaxWidth = (totalWidth - btnWidth - 8.dp.roundToPx()).coerceAtLeast(0)
        val titlePlaceable = measurables[0].measure(
            Constraints(minWidth = 0, maxWidth = titleMaxWidth, minHeight = 0, maxHeight = Constraints.Infinity)
        )
        val titleHeight = titlePlaceable.height

        // 3. Measure Status Line (child 2)
        val statusLineWidth = (totalWidth - (btnWidth + 10.dp.roundToPx()) * p).roundToInt().coerceIn(0, totalWidth)
        val statusLineHeightPx = lerpDp(14.dp, 12.dp, p).roundToPx()
        val statusLinePlaceable = measurables[2].measure(
            Constraints.fixed(statusLineWidth, statusLineHeightPx)
        )
        val statusLineHeight = statusLinePlaceable.height

        // 4. Measure Amounts (child 3)
        val amountsPlaceable = measurables[3].measure(
            Constraints(minWidth = 0, maxWidth = totalWidth, minHeight = 0, maxHeight = Constraints.Infinity)
        )
        val amountsHeight = amountsPlaceable.height

        // 5. Measure Warning (child 4, if present)
        val hasWarningMeasurable = measurables.size > 4
        val warningPlaceable = if (hasWarningMeasurable) {
            measurables[4].measure(
                Constraints(minWidth = 0, maxWidth = totalWidth, minHeight = 0, maxHeight = Constraints.Infinity)
            )
        } else null
        val warningHeight = warningPlaceable?.height ?: 0

        val gap1 = lerpDp(10.dp, 0.dp, p).roundToPx()
        val gap2 = lerpDp(10.dp, 0.dp, p).roundToPx()
        val gap3 = lerpDp(8.dp, 0.dp, p).roundToPx()

        val headerHeight = maxOf(titleHeight, btnHeight)
        val expandedHeight = headerHeight + gap1 + statusLineHeight + gap2 + amountsHeight + if (hasWarningMeasurable) (gap3 + warningHeight) else 0
        val collapsedHeight = maxOf(statusLineHeight, btnHeight)
        val currentTotalHeight = (expandedHeight + (collapsedHeight - expandedHeight) * p).roundToInt()

        // Positions:
        val btnX = totalWidth - btnWidth
        val btnYExpanded = ((headerHeight - btnHeight) / 2f).coerceAtLeast(0f)
        val btnYCollapsed = ((collapsedHeight - btnHeight) / 2f).coerceAtLeast(0f)
        val btnY = (btnYExpanded + (btnYCollapsed - btnYExpanded) * p).roundToInt()

        val statusLineYExpanded = (headerHeight + gap1).toFloat()
        val statusLineYCollapsed = ((collapsedHeight - statusLineHeight) / 2f).coerceAtLeast(0f)
        val statusLineY = (statusLineYExpanded + (statusLineYCollapsed - statusLineYExpanded) * p).roundToInt()

        val amountsYExpanded = (headerHeight + gap1 + statusLineHeight + gap2).toFloat()
        val amountsYCollapsed = collapsedHeight.toFloat()
        val amountsY = (amountsYExpanded + (amountsYCollapsed - amountsYExpanded) * p).roundToInt()

        val warningYExpanded = (headerHeight + gap1 + statusLineHeight + gap2 + amountsHeight + gap3).toFloat()
        val warningYCollapsed = collapsedHeight.toFloat()
        val warningY = (warningYExpanded + (warningYCollapsed - warningYExpanded) * p).roundToInt()

        layout(totalWidth, currentTotalHeight) {
            if (fadeAlpha > 0f) {
                titlePlaceable.placeRelativeWithLayer(0, 0) {
                    alpha = fadeAlpha
                }
            }

            statusLinePlaceable.placeRelative(0, statusLineY)

            buttonPlaceable.placeRelative(btnX, btnY)

            if (fadeAlpha > 0f) {
                amountsPlaceable.placeRelativeWithLayer(0, amountsY) {
                    alpha = fadeAlpha
                }
            }

            if (warningPlaceable != null && fadeAlpha > 0f) {
                warningPlaceable.placeRelativeWithLayer(0, warningY) {
                    alpha = fadeAlpha
                }
            }
        }
    }
}
