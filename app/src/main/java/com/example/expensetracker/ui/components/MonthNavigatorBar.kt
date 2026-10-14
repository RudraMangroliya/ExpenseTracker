package com.example.expensetracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.AppColors
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.text.DateFormatSymbols
import java.util.Calendar

import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.rememberAppHapticFeedback

@Composable
fun MonthNavigatorBar(
    selectedMonth: Int,
    selectedYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onMonthYearSelected: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = rememberAppHapticFeedback()
    val now = Calendar.getInstance()
    val isCurrentMonth = selectedMonth == (now.get(Calendar.MONTH) + 1) && selectedYear == now.get(Calendar.YEAR)
    val monthName = DateFormatSymbols().months.getOrNull(selectedMonth - 1) ?: "Month $selectedMonth"

    var isForwardNav by remember { mutableStateOf(true) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Smooth entrance animation when page loads/appears
    var hasAppeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasAppeared = true
    }

    val barAlpha by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "pageEntranceAlpha"
    )
    val barTranslationY by animateFloatAsState(
        targetValue = if (hasAppeared) 0f else -12f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "pageEntranceSlideY"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                alpha = barAlpha
                translationY = barTranslationY
            }
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) {
                        isForwardNav = true
                        haptic.perform(AppHapticFeedbackType.Light)
                        onNextMonth()
                    } else if (dragAmount > 20) {
                        isForwardNav = false
                        haptic.perform(AppHapticFeedbackType.Light)
                        onPreviousMonth()
                    }
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile Previous Month Button with Centered Chevron
            TactileNavButton(
                icon = Icons.Default.ChevronLeft,
                contentDescription = "Previous Month",
                onClick = {
                    isForwardNav = false
                    haptic.perform(AppHapticFeedbackType.Light)
                    onPreviousMonth()
                }
            )

            // Center Interactive Capsule (Click to open Month Picker)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            if (onMonthYearSelected != null) {
                                showDatePickerDialog = true
                            } else {
                                onCurrentMonth()
                            }
                        }
                    )
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calendar icon badge with smooth scaling
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Pick Month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Smooth Sliding + Spring Scaling Month & Year Content
                AnimatedContent(
                    targetState = "$monthName $selectedYear",
                    transitionSpec = {
                        val slideOffset = { width: Int -> (width * 0.40f).toInt() }
                        val springSpec = spring<IntOffset>(dampingRatio = 0.72f, stiffness = 380f)
                        val scaleSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 380f)

                        if (isForwardNav) {
                            (slideInHorizontally(animationSpec = springSpec, initialOffsetX = slideOffset) +
                                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = scaleSpring)).togetherWith(
                                slideOutHorizontally(animationSpec = springSpec, targetOffsetX = { -slideOffset(it) }) +
                                        fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                                        scaleOut(targetScale = 0.92f, animationSpec = tween(160))
                            )
                        } else {
                            (slideInHorizontally(animationSpec = springSpec, initialOffsetX = { -slideOffset(it) }) +
                                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = scaleSpring)).togetherWith(
                                slideOutHorizontally(animationSpec = springSpec, targetOffsetX = slideOffset) +
                                        fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                                        scaleOut(targetScale = 0.92f, animationSpec = tween(160))
                            )
                        }
                    },
                    label = "monthYearSlideAnim"
                ) { _ ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Smooth Morphing Status Pill: "Current" vs "Today"
                AnimatedContent(
                    targetState = isCurrentMonth,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.85f)).togetherWith(
                            fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.85f)
                        )
                    },
                    label = "statusPillAnim"
                ) { current ->
                    if (current) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AppColors.incomeContainer,
                            border = BorderStroke(1.dp, AppColors.income.copy(alpha = 0.40f)),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.income)
                                )
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.income,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Medium)
                                onCurrentMonth()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "Jump to Today",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Tactile Next Month Button with Centered Chevron
            TactileNavButton(
                icon = Icons.Default.ChevronRight,
                contentDescription = "Next Month",
                onClick = {
                    isForwardNav = true
                    haptic.perform(AppHapticFeedbackType.Light)
                    onNextMonth()
                }
            )
        }
    }

    // Interactive Month & Year Picker Dialog
    if (showDatePickerDialog && onMonthYearSelected != null) {
        MonthYearPickerDialog(
            currentSelectedMonth = selectedMonth,
            currentSelectedYear = selectedYear,
            onDismiss = {
                haptic.perform(AppHapticFeedbackType.Light)
                showDatePickerDialog = false
            },
            onConfirm = { month, year ->
                haptic.perform(AppHapticFeedbackType.Light)
                isForwardNav = (year > selectedYear) || (year == selectedYear && month > selectedMonth)
                onMonthYearSelected(month, year)
                showDatePickerDialog = false
            }
        )
    }
}

@Composable
private fun TactileNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "buttonPressAnim"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    currentSelectedMonth: Int,
    currentSelectedYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, year: Int) -> Unit
) {
    val haptic = rememberAppHapticFeedback()
    var pickerYear by remember { mutableIntStateOf(currentSelectedYear) }
    var pickerMonth by remember { mutableIntStateOf(currentSelectedMonth) }

    val months = remember {
        listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
    }

    val now = Calendar.getInstance()
    val thisMonth = now.get(Calendar.MONTH) + 1
    val thisYear = now.get(Calendar.YEAR)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(24.dp)),
        confirmButton = {
            Button(
                onClick = {
                    haptic.perform(AppHapticFeedbackType.Light)
                    onConfirm(pickerMonth, pickerYear)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Light)
                        pickerMonth = thisMonth
                        pickerYear = thisYear
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Today")
                }
                TextButton(
                    onClick = {
                        haptic.perform(AppHapticFeedbackType.Light)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Month & Year",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year Switcher Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            pickerYear -= 1
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                        }
                        Text(
                            text = pickerYear.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = {
                            haptic.perform(AppHapticFeedbackType.Light)
                            pickerYear += 1
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                        }
                    }
                }

                // 3x4 Month Grid Selection
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(months) { index, shortMonthName ->
                        val monthIndex = index + 1
                        val isSelected = (monthIndex == pickerMonth)
                        val isRealCurrent = (monthIndex == thisMonth && pickerYear == thisYear)

                        Surface(
                            onClick = {
                                haptic.perform(AppHapticFeedbackType.Light)
                                pickerMonth = monthIndex
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (isRealCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = if (isRealCurrent && !isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .height(44.dp)
                                .fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = shortMonthName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isRealCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
