package com.example.expensetracker.ui.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.local.UserPreferencesRepository
import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.CategoryIconMapper
import com.example.expensetracker.utils.ValidationUtils
import com.example.expensetracker.utils.rememberAppHapticFeedback
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel,
    transactionIdToEdit: Long? = null,
    onNavigateBack: () -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val context = LocalContext.current
    val haptic = rememberAppHapticFeedback()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionIdToEdit) {
        if (transactionIdToEdit != null && transactionIdToEdit > 0) {
            viewModel.loadTransactionForEdit(transactionIdToEdit)
        } else {
            viewModel.prepareForAdd()
        }
    }

    LaunchedEffect(formState.saveSuccess, formState.deleteSuccess) {
        if (formState.saveSuccess || formState.deleteSuccess) {
            viewModel.clearEvents()
        }
    }

    val scrollState = rememberScrollState()
    var userToggledSplit by remember { mutableStateOf(false) }
    var userAddedParticipant by remember { mutableStateOf(false) }

    // Smoothly scroll down to reveal newly expanded split bill options ONLY when user interactively toggles it
    LaunchedEffect(formState.isSplitEnabled) {
        if (userToggledSplit && formState.isSplitEnabled) {
            userToggledSplit = false
            kotlinx.coroutines.delay(150)
            scrollState.animateScrollBy(320f)
        }
    }

    // Gently scroll when user adds a participant to keep the list and summary comfortably in view
    LaunchedEffect(formState.splitParticipants.size) {
        if (userAddedParticipant && formState.isSplitEnabled) {
            userAddedParticipant = false
            kotlinx.coroutines.delay(100)
            scrollState.animateScrollBy(180f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern Animated Type Slider (Expense vs Income)
        com.example.expensetracker.ui.components.TransactionTypeSlider(
            selectedType = formState.type,
            onTypeSelected = viewModel::onTypeChanged
        )

        // Amount Input & Currency
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = formState.amount,
                onValueChange = viewModel::onAmountChanged,
                label = { Text("Amount") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = formState.amountError != null,
                supportingText = {
                    formState.amountError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Currency Dropdown
            var currencyExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { 
                    currencyExpanded = !currencyExpanded
                    haptic.perform(AppHapticFeedbackType.Light)
                },
                modifier = Modifier.width(135.dp)
            ) {
                val currDetail = ValidationUtils.getCurrencyDetail(formState.currency)
                OutlinedTextField(
                    value = "${currDetail.code} (${currDetail.symbol})",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false },
                    modifier = Modifier.widthIn(min = 230.dp)
                ) {
                    UserPreferencesRepository.SUPPORTED_CURRENCIES.forEachIndexed { index, curr ->
                        com.example.expensetracker.ui.components.CurrencyDropdownOption(
                            currencyCode = curr,
                            isSelected = formState.currency == curr,
                            index = index,
                            isMenuExpanded = currencyExpanded,
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                viewModel.onCurrencyChanged(curr)
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Exchange Rate Conversion Info Banner
        formState.rateStatusInfo?.let { infoStr ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = infoStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Category Picker Dropdown
        var categoryExpanded by remember { mutableStateOf(false) }
        val selectedCategory = formState.categories.find { it.id == formState.categoryId }

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { 
                categoryExpanded = !categoryExpanded
                haptic.perform(AppHapticFeedbackType.Light)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                leadingIcon = selectedCategory?.let { cat ->
                    {
                        Icon(
                            imageVector = CategoryIconMapper.getIcon(cat.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                isError = formState.categoryError != null,
                supportingText = {
                    formState.categoryError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                formState.categories.forEachIndexed { index, category ->
                    com.example.expensetracker.ui.components.CategoryDropdownOption(
                        category = category,
                        isSelected = formState.categoryId == category.id,
                        index = index,
                        isMenuExpanded = categoryExpanded,
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            viewModel.onCategoryChanged(category.id)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Date Picker
        val cal = Calendar.getInstance().apply { timeInMillis = formState.date }
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                haptic.perform(AppHapticFeedbackType.Light)
                viewModel.onDateChanged(selectedCal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        OutlinedButton(
            onClick = { 
                haptic.perform(AppHapticFeedbackType.Light)
                datePickerDialog.show() 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Date: ${ValidationUtils.formatDate(formState.date, "EEEE, MMM dd, yyyy")}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Payment Method Dropdown
        var paymentExpanded by remember { mutableStateOf(false) }
        val paymentMethods = listOf("Cash", "Credit Card", "Debit Card", "Bank Transfer", "Other")

        ExposedDropdownMenuBox(
            expanded = paymentExpanded,
            onExpandedChange = { 
                paymentExpanded = !paymentExpanded
                haptic.perform(AppHapticFeedbackType.Light)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.paymentMethod,
                onValueChange = {},
                readOnly = true,
                label = { Text("Payment Method") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = paymentExpanded,
                onDismissRequest = { paymentExpanded = false }
            ) {
                paymentMethods.forEachIndexed { index, method ->
                    com.example.expensetracker.ui.components.PaymentMethodDropdownOption(
                        method = method,
                        isSelected = formState.paymentMethod == method,
                        index = index,
                        isMenuExpanded = paymentExpanded,
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            viewModel.onPaymentMethodChanged(method)
                            paymentExpanded = false
                        }
                    )
                }
            }
        }

        // Note Input
        OutlinedTextField(
            value = formState.note,
            onValueChange = viewModel::onNoteChanged,
            label = { Text("Note (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.noteError != null,
            supportingText = {
                formState.noteError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                } ?: Text("${formState.note.length} / 250 characters")
            },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        // Split Bill Section (Only for Expense)
        if (formState.type == "EXPENSE") {
            com.example.expensetracker.ui.components.SplitBillSection(
                formState = formState,
                onToggleSplit = { checked ->
                    userToggledSplit = true
                    viewModel.toggleSplit(checked)
                },
                onSetSplitMode = viewModel::setSplitMode,
                onAddParticipant = { name, phone ->
                    userAddedParticipant = true
                    viewModel.addParticipant(name, phone)
                },
                onRemoveParticipant = viewModel::removeParticipant,
                onUpdateParticipantAmount = viewModel::updateParticipantAmount
            )
        }

        // Action Buttons: Save / Update & Delete
        val isEditMode = formState.transactionId != null

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Primary Update / Save Button
            Button(
                onClick = {
                    haptic.perform(AppHapticFeedbackType.Success)
                    viewModel.saveTransaction(onCloseStarted = onNavigateBack)
                },
                enabled = !formState.isSaving && !formState.isDeleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (formState.isSaving) {
                        "Saving..."
                    } else if (isEditMode) {
                        "Update Transaction"
                    } else {
                        "Save Transaction"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Prominent Delete Button in Edit Mode
            if (isEditMode) {
                OutlinedButton(
                    onClick = { 
                        haptic.perform(AppHapticFeedbackType.Heavy)
                        showDeleteDialog = true 
                    },
                    enabled = !formState.isSaving && !formState.isDeleting,
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (formState.isDeleting) "Deleting..." else "Delete Transaction",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                haptic.perform(AppHapticFeedbackType.Light)
                showDeleteDialog = false 
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Delete Transaction?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to permanently delete this transaction? Your balance, reports, and monthly budget will be updated immediately.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Heavy)
                        showDeleteDialog = false
                        viewModel.deleteCurrentTransaction(onCloseStarted = onNavigateBack)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    haptic.perform(AppHapticFeedbackType.Light)
                    showDeleteDialog = false 
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
