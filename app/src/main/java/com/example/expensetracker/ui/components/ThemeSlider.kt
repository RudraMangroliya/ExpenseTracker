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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.local.AppTheme
import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.rememberAppHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ThemeSlider(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberAppHapticFeedback()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val currentSelectedTheme by rememberUpdatedState(selectedTheme)
    val currentOnThemeSelected by rememberUpdatedState(onThemeSelected)

    val items = remember {
        listOf(
            Triple(AppTheme.SYSTEM, "System", Icons.Default.BrightnessAuto),
            Triple(AppTheme.LIGHT, "Light", Icons.Default.LightMode),
            Triple(AppTheme.DARK, "Dark", Icons.Default.DarkMode)
        )
    }

    val selectedIndex = when (selectedTheme) {
        AppTheme.SYSTEM -> 0
        AppTheme.LIGHT -> 1
        AppTheme.DARK -> 2
    }

    val trackBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val activePillColor = MaterialTheme.colorScheme.primary

    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / 3
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val maxOffsetPx = tabWidthPx * 2

        val offsetAnim = remember { Animatable(selectedIndex * tabWidthPx) }

        // Sync with external state changes when not dragging
        LaunchedEffect(selectedIndex, tabWidthPx) {
            if (!isDragging) {
                val targetPx = selectedIndex * tabWidthPx
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

        val currentProgress = (offsetAnim.value / tabWidthPx.coerceAtLeast(1f)).coerceIn(0f, 2f)

        // Interactive Horizontal Drag Handler on Slider Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tabWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetAnim.value + dragAmount).coerceIn(0f, maxOffsetPx)
                            coroutineScope.launch {
                                offsetAnim.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            val targetIndex = (offsetAnim.value / tabWidthPx).roundToInt().coerceIn(0, 2)
                            val targetTheme = items[targetIndex].first
                            coroutineScope.launch {
                                offsetAnim.animateTo(
                                    targetValue = targetIndex * tabWidthPx,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            if (targetTheme != currentSelectedTheme) {
                                haptic.perform(AppHapticFeedbackType.Medium)
                            }
                            currentOnThemeSelected(targetTheme)
                        },
                        onDragCancel = {
                            isDragging = false
                            val targetIndex = when (currentSelectedTheme) {
                                AppTheme.SYSTEM -> 0
                                AppTheme.LIGHT -> 1
                                AppTheme.DARK -> 2
                            }
                            coroutineScope.launch {
                                offsetAnim.animateTo(
                                    targetValue = targetIndex * tabWidthPx,
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
            // Sliding Pill Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(activePillColor)
            )

            // Option Labels & Icons
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, (theme, label, icon) ->
                    // Proximity check for smooth text highlight during dragging or selection
                    val isNearest = if (isDragging) {
                        kotlin.math.abs(currentProgress - index) < 0.5f
                    } else {
                        index == selectedIndex
                    }

                    val textColor by animateColorAsState(
                        targetValue = if (isNearest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(140),
                        label = "themeTextColor_$index"
                    )

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
                                        targetValue = index * tabWidthPx,
                                        animationSpec = spring(
                                            dampingRatio = 0.82f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                                if (theme != currentSelectedTheme) {
                                    haptic.perform(AppHapticFeedbackType.Medium)
                                }
                                currentOnThemeSelected(theme)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isNearest) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
