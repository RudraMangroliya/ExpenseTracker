package com.example.expensetracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.rememberAppHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TransactionTypeSlider(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberAppHapticFeedback()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val currentSelectedType by rememberUpdatedState(selectedType)
    val currentOnTypeSelected by rememberUpdatedState(onTypeSelected)

    val isExpense = selectedType == "EXPENSE"

    // Background track colors
    val trackBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        val halfWidth = maxWidth / 2
        val halfWidthPx = with(density) { halfWidth.toPx() }

        val offsetAnim = remember { Animatable(if (isExpense) 0f else halfWidthPx) }

        // Sync with external state changes when not dragging
        LaunchedEffect(isExpense, halfWidthPx) {
            if (!isDragging) {
                val targetPx = if (isExpense) 0f else halfWidthPx
                if (offsetAnim.value != targetPx) {
                    offsetAnim.animateTo(
                        targetValue = targetPx,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        }

        val progress = (offsetAnim.value / halfWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)

        // Dynamic Real-time Gradient Morphing between Expense Red and Income Green
        val expenseColorStart = Color(0xFFE53935)
        val expenseColorEnd = Color(0xFFD32F2F)
        val incomeColorStart = Color(0xFF43A047)
        val incomeColorEnd = Color(0xFF2E7D32)

        val currentStartColor = lerp(expenseColorStart, incomeColorStart, progress)
        val currentEndColor = lerp(expenseColorEnd, incomeColorEnd, progress)
        val activeGradient = Brush.horizontalGradient(listOf(currentStartColor, currentEndColor))

        val isNearExpense = progress < 0.5f

        val expenseTextColor by animateColorAsState(
            targetValue = if (isNearExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(140),
            label = "expenseTextColor"
        )

        val incomeTextColor by animateColorAsState(
            targetValue = if (!isNearExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(140),
            label = "incomeTextColor"
        )

        // Horizontal Drag Handler on Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(halfWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetAnim.value + dragAmount).coerceIn(0f, halfWidthPx)
                            coroutineScope.launch {
                                offsetAnim.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            val selectIncome = offsetAnim.value >= (halfWidthPx / 2f)
                            val snapTargetPx = if (selectIncome) halfWidthPx else 0f
                            val newType = if (selectIncome) "INCOME" else "EXPENSE"

                            coroutineScope.launch {
                                offsetAnim.animateTo(
                                    targetValue = snapTargetPx,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            if (newType != currentSelectedType) {
                                haptic.perform(AppHapticFeedbackType.Medium)
                            }
                            currentOnTypeSelected(newType)
                        },
                        onDragCancel = {
                            isDragging = false
                            val snapTargetPx = if (currentSelectedType == "EXPENSE") 0f else halfWidthPx
                            coroutineScope.launch {
                                offsetAnim.animateTo(
                                    targetValue = snapTargetPx,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            // The Sliding Pill Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                    .width(halfWidth)
                    .fillMaxHeight()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(activeGradient)
            )

            // Clickable Touch Areas & Content Labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expense Tab (Left)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isDragging = false
                            coroutineScope.launch {
                                offsetAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            if (currentSelectedType != "EXPENSE") {
                                haptic.perform(AppHapticFeedbackType.Medium)
                            }
                            currentOnTypeSelected("EXPENSE")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = expenseTextColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isNearExpense) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = expenseTextColor
                        )
                    }
                }

                // Income Tab (Right)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isDragging = false
                            coroutineScope.launch {
                                offsetAnim.animateTo(
                                    targetValue = halfWidthPx,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            if (currentSelectedType != "INCOME") {
                                haptic.perform(AppHapticFeedbackType.Medium)
                            }
                            currentOnTypeSelected("INCOME")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = incomeTextColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!isNearExpense) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = incomeTextColor
                        )
                    }
                }
            }
        }
    }
}
