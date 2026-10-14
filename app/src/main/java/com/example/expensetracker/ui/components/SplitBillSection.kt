package com.example.expensetracker.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.AppColors
import com.example.expensetracker.ui.theme.LocalIsDarkTheme
import com.example.expensetracker.ui.transactions.AddEditTransactionFormState
import com.example.expensetracker.ui.transactions.SplitMode
import com.example.expensetracker.ui.transactions.SplitParticipantItem
import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.SplitReminderUtils
import com.example.expensetracker.utils.ValidationUtils
import com.example.expensetracker.utils.rememberAppHapticFeedback

@Composable
fun SplitBillSection(
    formState: AddEditTransactionFormState,
    onToggleSplit: (Boolean) -> Unit,
    onSetSplitMode: (SplitMode) -> Unit,
    onAddParticipant: (String, String) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onUpdateParticipantAmount: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val haptic = rememberAppHapticFeedback()
    var showAddPersonDialog by remember { mutableStateOf(false) }

    // System Contact Picker launcher with automatic 10-digit phone extraction & resilient fallback
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
            var name = ""
            var rawPhone = ""

            try {
                // Direct query on the URI returned by the system picker (granted temporary read permission by OS)
                context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        // 1. Phone number (if picked from CommonDataKinds.Phone)
                        val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (phoneIndex != -1) {
                            rawPhone = cursor.getString(phoneIndex) ?: ""
                        }

                        // 2. Display name
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex) ?: ""
                        }
                        if (name.isBlank()) {
                            val altNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            if (altNameIndex != -1) {
                                name = cursor.getString(altNameIndex) ?: ""
                            }
                        }
                        if (name.isBlank()) {
                            val defNameIndex = cursor.getColumnIndex("display_name")
                            if (defNameIndex != -1) {
                                name = cursor.getString(defNameIndex) ?: ""
                            }
                        }

                        // 3. If phone wasn't directly in the primary cursor (e.g. if URI was Contacts.CONTENT_URI),
                        // safely query the contact's scoped phone sub-data:
                        if (rawPhone.isBlank()) {
                            try {
                                val phoneSubUri = Uri.withAppendedPath(
                                    contactUri,
                                    ContactsContract.Contacts.Data.CONTENT_DIRECTORY
                                )
                                context.contentResolver.query(
                                    phoneSubUri,
                                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                    "${ContactsContract.Data.MIMETYPE} = ?",
                                    arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                                    null
                                )?.use { pCursor ->
                                    if (pCursor.moveToFirst()) {
                                        val pIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        if (pIdx != -1) {
                                            rawPhone = pCursor.getString(pIdx) ?: ""
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // If sub-query fails due to permission restriction, proceed with extracted name!
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SplitBillSection", "Error querying picked contact", e)
            }

            // Extract valid name or fallback
            val participantName = if (name.isNotBlank()) {
                name.trim()
            } else if (rawPhone.isNotBlank()) {
                rawPhone.trim()
            } else {
                "Friend"
            }

            // Normalize extracted phone number to exact 10 digits
            val normalizedPhone = SplitReminderUtils.extractTenDigitNumber(rawPhone)

            if (participantName.isNotBlank()) {
                haptic.perform(AppHapticFeedbackType.Success)
                onAddParticipant(participantName, normalizedPhone)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.45f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Minimalist Icon + Title + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (formState.isSplitEnabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = if (formState.isSplitEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Split Bill with Friends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record only your share in expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = formState.isSplitEnabled,
                    onCheckedChange = { checked ->
                        haptic.perform(AppHapticFeedbackType.Light)
                        onToggleSplit(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Expandable Split Options & Participants Section
            AnimatedVisibility(
                visible = formState.isSplitEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Button Row: Equal vs Custom
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = formState.splitMode == SplitMode.EQUAL,
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                onSetSplitMode(SplitMode.EQUAL)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Equal Split (=)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }

                        SegmentedButton(
                            selected = formState.splitMode == SplitMode.CUSTOM,
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                onSetSplitMode(SplitMode.CUSTOM)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Custom Split", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Participants Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Participants (${formState.splitParticipants.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (formState.splitMode == SplitMode.EQUAL && formState.splitParticipants.isNotEmpty()) {
                            val perPersonVal = formState.splitParticipants.firstOrNull()?.amount?.toDoubleOrNull() ?: 0.0
                            Text(
                                text = "≈ ${ValidationUtils.formatCurrency(perPersonVal, formState.currency)} each",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Participants List with fluid animations
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        formState.splitParticipants.forEach { item ->
                            ParticipantRow(
                                item = item,
                                currency = formState.currency,
                                isCustomMode = formState.splitMode == SplitMode.CUSTOM,
                                onUpdateAmount = { onUpdateParticipantAmount(item.id, it) },
                                onRemove = { onRemoveParticipant(item.id) }
                            )
                        }
                    }

                    // Add Person & Contacts Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                showAddPersonDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Person", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                val phonePickIntent = Intent(Intent.ACTION_PICK).apply {
                                    setDataAndType(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                                    )
                                }
                                try {
                                    contactPickerLauncher.launch(phonePickIntent)
                                } catch (_: Exception) {
                                    try {
                                        val genericContactIntent = Intent(
                                            Intent.ACTION_PICK,
                                            ContactsContract.Contacts.CONTENT_URI
                                        )
                                        contactPickerLauncher.launch(genericContactIntent)
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Contacts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    // Validation Error Feedback (if custom split difference exists)
                    AnimatedVisibility(
                        visible = formState.splitError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        formState.splitError?.let { errText ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = errText,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Live Summary Card (The Golden Rule: Only User Share Counts)
                    val totalBill = formState.amount.toDoubleOrNull() ?: 0.0
                    val userItem = formState.splitParticipants.firstOrNull { it.isCurrentUser }
                    val userShare = userItem?.amount?.toDoubleOrNull() ?: totalBill
                    val othersShare = (totalBill - userShare).coerceAtLeast(0.0)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Bill Amount:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ValidationUtils.formatCurrency(totalBill, formState.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.8.dp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Text(
                                        text = "Your Personal Expense:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = ValidationUtils.formatCurrency(userShare, formState.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val othersCount = (formState.splitParticipants.size - 1).coerceAtLeast(0)
                                val countLabel = if (othersCount == 1) "1 person" else "$othersCount people"
                                Text(
                                    text = "To collect from others ($countLabel):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = ValidationUtils.formatCurrency(othersShare, formState.currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Person Dialog with Strict 10-Digit Mobile Verification
    if (showAddPersonDialog) {
        var personName by remember { mutableStateOf("") }
        var personPhone by remember { mutableStateOf("") }
        var isPhoneTouched by remember { mutableStateOf(false) }

        val cleanPhoneDigits = SplitReminderUtils.extractTenDigitNumber(personPhone)
        val isPhoneEntered = cleanPhoneDigits.isNotEmpty()
        val isPhoneComplete = cleanPhoneDigits.length == 10
        val isPhoneInvalid = isPhoneEntered && !isPhoneComplete

        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = {
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
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text("Add Participant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Name") },
                        placeholder = { Text("e.g. Alex") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = personPhone,
                        onValueChange = { input ->
                            isPhoneTouched = true
                            personPhone = SplitReminderUtils.extractTenDigitNumber(input)
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("10-digit mobile number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isPhoneTouched && isPhoneInvalid,
                        supportingText = {
                            if (isPhoneEntered) {
                                if (isPhoneComplete) {
                                    Text(
                                        text = "✓ Valid 10-digit mobile number",
                                        color = AppColors.income,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Must be exact 10 digits (${cleanPhoneDigits.length}/10)",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Optional • Exactly 10 digits if entered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        trailingIcon = {
                            if (isPhoneComplete) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Valid",
                                    tint = AppColors.income,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = personName.trim()
                        if (trimmedName.isNotBlank() && (!isPhoneEntered || isPhoneComplete)) {
                            haptic.perform(AppHapticFeedbackType.Success)
                            onAddParticipant(trimmedName, cleanPhoneDigits)
                            showAddPersonDialog = false
                        }
                    },
                    enabled = personName.isNotBlank() && (!isPhoneEntered || isPhoneComplete),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Person", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ParticipantRow(
    item: SplitParticipantItem,
    currency: String,
    isCustomMode: Boolean,
    onUpdateAmount: (String) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar initial + Name + Phone / "You" Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.isCurrentUser) "Y" else item.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "You",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (item.phoneNumber.isNotBlank()) {
                        Text(
                            text = SplitReminderUtils.formatPhoneForDisplay(item.phoneNumber),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Share Amount (or custom input) + Fixed 28.dp Slot (Guarantees Column Alignment across all rows)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isCustomMode) {
                    OutlinedTextField(
                        value = item.amount,
                        onValueChange = onUpdateAmount,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .width(96.dp)
                            .height(48.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    val amountVal = item.amount.toDoubleOrNull() ?: 0.0
                    Text(
                        text = ValidationUtils.formatCurrency(amountVal, currency),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Trailing 28.dp slot: delete icon for others, spacer for "You" so all numbers vertically align!
                if (!item.isCurrentUser) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
