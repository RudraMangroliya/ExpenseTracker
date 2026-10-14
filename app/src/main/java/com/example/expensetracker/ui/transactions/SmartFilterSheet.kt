package com.example.expensetracker.ui.transactions

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.CategoryIconMapper
import com.example.expensetracker.utils.ValidationUtils
import com.example.expensetracker.utils.rememberAppHapticFeedback
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartFilterSheet(
    sheetState: SheetState,
    initialCriteria: TransactionFilterCriteria,
    categories: List<CategoryEntity>,
    baseCurrency: String,
    onDismiss: () -> Unit,
    onApply: (TransactionFilterCriteria) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberAppHapticFeedback()
    val currSymbol = ValidationUtils.getCurrencyDetail(baseCurrency).symbol

    var draftType by remember(initialCriteria) { mutableStateOf(initialCriteria.type) }
    var draftCategoryId by remember(initialCriteria) { mutableStateOf(initialCriteria.categoryId) }
    var draftPaymentMethod by remember(initialCriteria) { mutableStateOf(initialCriteria.paymentMethod) }
    var draftMinAmount by remember(initialCriteria) { mutableStateOf(initialCriteria.minAmount?.toString() ?: "") }
    var draftMaxAmount by remember(initialCriteria) { mutableStateOf(initialCriteria.maxAmount?.toString() ?: "") }
    var draftDatePreset by remember(initialCriteria) { mutableStateOf(initialCriteria.datePreset) }
    var draftCustomStart by remember(initialCriteria) { mutableStateOf(initialCriteria.customStartDate) }
    var draftCustomEnd by remember(initialCriteria) { mutableStateOf(initialCriteria.customEndDate) }
    var draftSortOrder by remember(initialCriteria) { mutableStateOf(initialCriteria.sortOrder) }
    var draftIsSplitOnly by remember(initialCriteria) { mutableStateOf(initialCriteria.isSplitOnly) }

    // Live calculation of draft active filters count
    val draftActiveFilterCount = remember(
        draftType, draftCategoryId, draftPaymentMethod,
        draftMinAmount, draftMaxAmount, draftDatePreset, draftSortOrder, draftIsSplitOnly
    ) {
        var count = 0
        if (draftType != "ALL") count++
        if (draftCategoryId != null && draftCategoryId!! > 0) count++
        if (draftPaymentMethod != "ALL") count++
        if (draftMinAmount.toDoubleOrNull() != null && draftMinAmount.toDoubleOrNull()!! > 0.0) count++
        if (draftMaxAmount.toDoubleOrNull() != null && draftMaxAmount.toDoubleOrNull()!! > 0.0) count++
        if (draftDatePreset != DateRangePreset.ALL_TIME) count++
        if (draftSortOrder != TransactionSortOrder.DATE_DESC) count++
        if (draftIsSplitOnly) count++
        count
    }

    val displayedCategories = remember(categories, draftType) {
        when (draftType.uppercase()) {
            "EXPENSE" -> categories.filter { it.type.equals("EXPENSE", ignoreCase = true) }
            "INCOME" -> categories.filter { it.type.equals("INCOME", ignoreCase = true) }
            else -> categories
        }
    }

    val datePresets = remember { DateRangePreset.entries }
    val sortOrders = remember { TransactionSortOrder.entries }
    val paymentOptions = remember {
        listOf("ALL", "Cash", "Credit Card", "Debit Card", "Bank Transfer", "Other")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            // Respects system status bar / camera notch so header never overlaps
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
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Smart Filters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Refine & organize transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = draftActiveFilterCount > 0,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Surface(
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Medium)
                                draftType = "ALL"
                                draftCategoryId = null
                                draftPaymentMethod = "ALL"
                                draftMinAmount = ""
                                draftMaxAmount = ""
                                draftDatePreset = DateRangePreset.ALL_TIME
                                draftCustomStart = null
                                draftCustomEnd = null
                                draftSortOrder = TransactionSortOrder.DATE_DESC
                                draftIsSplitOnly = false
                                onReset()
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 11.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset All",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp
            )

            // Scrollable Filter Sections
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Transaction Type Section (Segmented Pill Control - Never Wraps)
                FilterSectionCard(
                    title = "Transaction Type",
                    icon = Icons.Default.Payments
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple("ALL", "All Types", MaterialTheme.colorScheme.primary),
                                Triple("EXPENSE", "Expense", AppColors.expense),
                                Triple("INCOME", "Income", AppColors.income)
                            ).forEach { (typeVal, typeLabel, accentColor) ->
                                val isSelected = draftType.equals(typeVal, ignoreCase = true)
                                Surface(
                                    onClick = {
                                        haptic.perform(AppHapticFeedbackType.Light)
                                        draftType = typeVal
                                        if (draftCategoryId != null && typeVal != "ALL") {
                                            val selectedCat = categories.find { it.id == draftCategoryId }
                                            if (selectedCat != null && !selectedCat.type.equals(typeVal, ignoreCase = true)) {
                                                draftCategoryId = null
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(11.dp),
                                    color = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.60f)) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = typeLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Split Bills Section
                FilterSectionCard(
                    title = "Split Bills",
                    icon = Icons.Default.Group
                ) {
                    Surface(
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            draftIsSplitOnly = !draftIsSplitOnly
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (draftIsSplitOnly) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (draftIsSplitOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (draftIsSplitOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = if (draftIsSplitOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Split Transactions Only",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Show expenses shared with friends",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = draftIsSplitOnly,
                                onCheckedChange = {
                                    haptic.perform(AppHapticFeedbackType.Light)
                                    draftIsSplitOnly = it
                                }
                            )
                        }
                    }
                }

                // 2. Date Period Section
                FilterSectionCard(
                    title = "Date Period",
                    icon = Icons.Default.DateRange
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        datePresets.forEach { preset ->
                            val isSelected = draftDatePreset == preset
                            FilterOptionChip(
                                selected = isSelected,
                                onClick = { draftDatePreset = preset },
                                label = preset.label
                            )
                        }
                    }

                    // Custom Date Range Pickers
                    AnimatedVisibility(
                        visible = draftDatePreset == DateRangePreset.CUSTOM,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Start Date Picker Button
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = draftCustomStart ?: System.currentTimeMillis()
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply { set(y, m, d) }
                                            draftCustomStart = c.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (draftCustomStart != null) ValidationUtils.formatDate(draftCustomStart!!) else "From Date",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // End Date Picker Button
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = draftCustomEnd ?: System.currentTimeMillis()
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val c = Calendar.getInstance().apply { set(y, m, d) }
                                            draftCustomEnd = c.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (draftCustomEnd != null) ValidationUtils.formatDate(draftCustomEnd!!) else "To Date",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 3. Category Section (FlowRow for complete visibility, filtered by Type)
                val categorySectionTitle = when (draftType.uppercase()) {
                    "EXPENSE" -> "Expense Category"
                    "INCOME" -> "Income Category"
                    else -> "Category"
                }
                FilterSectionCard(
                    title = categorySectionTitle,
                    icon = Icons.Default.Category
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAllSelected = draftCategoryId == null
                        FilterOptionChip(
                            selected = isAllSelected,
                            onClick = { draftCategoryId = null },
                            label = "All Categories",
                            leadingIcon = Icons.Default.Category
                        )

                        displayedCategories.forEach { cat ->
                            val isSelected = draftCategoryId == cat.id
                            FilterOptionChip(
                                selected = isSelected,
                                onClick = { draftCategoryId = if (isSelected) null else cat.id },
                                label = cat.name,
                                leadingIcon = CategoryIconMapper.getIcon(cat.icon)
                            )
                        }
                    }
                }

                // 4. Payment Method Section
                FilterSectionCard(
                    title = "Payment Method",
                    icon = Icons.Default.CreditCard
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentOptions.forEach { method ->
                            val isSelected = draftPaymentMethod.equals(method, ignoreCase = true)
                            FilterOptionChip(
                                selected = isSelected,
                                onClick = { draftPaymentMethod = method },
                                label = if (method == "ALL") "All Methods" else method
                            )
                        }
                    }
                }

                // 5. Amount Range Section
                FilterSectionCard(
                    title = "Amount Range ($baseCurrency)",
                    icon = Icons.Default.Payments
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = draftMinAmount,
                            onValueChange = { draftMinAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Min Amount") },
                            placeholder = { Text("0.00") },
                            prefix = { Text(currSymbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            trailingIcon = if (draftMinAmount.isNotEmpty()) {
                                {
                                    IconButton(onClick = { draftMinAmount = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                        OutlinedTextField(
                            value = draftMaxAmount,
                            onValueChange = { draftMaxAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Max Amount") },
                            placeholder = { Text("Any") },
                            prefix = { Text(currSymbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                            trailingIcon = if (draftMaxAmount.isNotEmpty()) {
                                {
                                    IconButton(onClick = { draftMaxAmount = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                // 6. Sort Order Section
                FilterSectionCard(
                    title = "Sort Transactions",
                    icon = Icons.AutoMirrored.Filled.Sort
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortOrders.forEach { order ->
                            val isSelected = draftSortOrder == order
                            FilterOptionChip(
                                selected = isSelected,
                                onClick = { draftSortOrder = order },
                                label = order.label
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 1.dp
            )

            // Sticky Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Light)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Medium)
                        val min = draftMinAmount.toDoubleOrNull()
                        val max = draftMaxAmount.toDoubleOrNull()
                        val updatedCriteria = initialCriteria.copy(
                            type = draftType,
                            categoryId = draftCategoryId,
                            paymentMethod = draftPaymentMethod,
                            minAmount = min,
                            maxAmount = max,
                            datePreset = draftDatePreset,
                            customStartDate = draftCustomStart,
                            customEndDate = draftCustomEnd,
                            sortOrder = draftSortOrder,
                            isSplitOnly = draftIsSplitOnly
                        )
                        onApply(updatedCriteria)
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    val btnText = if (draftActiveFilterCount > 0) "Apply ($draftActiveFilterCount)" else "Apply Filters"
                    Text(btnText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun FilterOptionChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    leadingIcon: ImageVector? = null,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
    modifier: Modifier = Modifier
) {
    val haptic = rememberAppHapticFeedback()
    val shape = RoundedCornerShape(12.dp)

    val containerColor = if (selected) {
        selectedContainerColor
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    }

    val borderColor = if (selected) {
        selectedColor.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val contentColor = if (selected) {
        selectedColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = {
            haptic.perform(AppHapticFeedbackType.Light)
            onClick()
        },
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
