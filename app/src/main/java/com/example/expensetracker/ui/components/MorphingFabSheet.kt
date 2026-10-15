package com.example.expensetracker.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.transactions.AddEditTransactionScreen
import com.example.expensetracker.ui.transactions.AddEditTransactionViewModel
import com.example.expensetracker.utils.AppHapticFeedbackType
import com.example.expensetracker.utils.rememberAppHapticFeedback
import kotlin.math.hypot

// Master Smooth Circular Reveal Easing Curves:
// Ultra-smooth sigmoid ease-out (gentle, unhurried start with silky-smooth deceleration and soft landing)
private val CircularRevealOpenEasing = CubicBezierEasing(0.20f, 0.0f, 0.12f, 1.0f)
private val CircularRevealCloseEasing = CubicBezierEasing(0.32f, 0.0f, 0.18f, 1.0f)

/**
 * Trigger origin defining where the circular reveal animation starts expanding from.
 */
enum class MorphSource {
    FAB,               // Bottom-right floating action button (+)
    DASHBOARD_BUTTON,  // Dashboard "+ Add Income / Expense" action button
    DEFAULT            // Centered expansion
}

/**
 * GPU-accelerated Circular Reveal Shape.
 * Implemented as a hardware-clipped RoundRect with cornerRadius equal to radius,
 * mapping directly to Android RenderNode.setRoundRect() for locked 60/120 FPS performance.
 */
class CircularRevealShape(
    private val radius: Float,
    private val centerX: Float,
    private val centerY: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = radius.coerceAtLeast(0.1f)
        return Outline.Rounded(
            RoundRect(
                left = centerX - r,
                top = centerY - r,
                right = centerX + r,
                bottom = centerY + r,
                cornerRadius = CornerRadius(r, r)
            )
        )
    }
}

/**
 * Smooth Circular Reveal Material 3 Transition:
 * When tapping the + button (or Add Income/Expense button or Edit Transaction), the animation
 * blooms outward from a micro circle across the screen in a smooth circular wave, revealing the form with zero distortion.
 */
@Composable
fun MorphingFabSheet(
    isOpen: Boolean,
    showFab: Boolean,
    source: MorphSource = MorphSource.FAB,
    editingTransactionId: Long?,
    viewModel: AddEditTransactionViewModel,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = rememberAppHapticFeedback()

    // 1. Hardware Profile Detection
    val isLowRamDevice = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice == true || (am?.memoryClass ?: 128) <= 128 || Runtime.getRuntime().availableProcessors() <= 4
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val maxShadowElevationPx = remember(density) { with(density) { 6.dp.toPx() } }

    // 2. Silky-Smooth & Responsive Animation Timing
    val openDuration = if (isLowRamDevice) 280 else 320
    val closeDuration = if (isLowRamDevice) 220 else 250

    val sheetProgressState = animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isOpen) openDuration else closeDuration,
            easing = if (isOpen) CircularRevealOpenEasing else CircularRevealCloseEasing
        ),
        label = "circularRevealProgress"
    )

    // Visibility of resting FAB when sheet is closed (smoothly coordinates with reveal)
    val fabVisibilityProgress by animateFloatAsState(
        targetValue = if (showFab && !isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isOpen) 180 else 220,
            easing = if (isOpen) FastOutLinearInEasing else FastOutSlowInEasing
        ),
        label = "fabVisibilityProgress"
    )

    // Hardware back gesture handler
    BackHandler(enabled = isOpen) {
        onClose()
    }

    // 3. Root Overlay Container
    Box(modifier = modifier.fillMaxSize()) {
        val progress = sheetProgressState.value

        // A. Background Scrim (GPU Alpha Layer)
        if (isOpen || progress > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = progress * (if (isLowRamDevice) 0.40f else 0.50f)
                    }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onClose()
                    }
            )
        }

        // B. Resting Floating Action Button (+)
        if (fabVisibilityProgress > 0.005f) {
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 92.dp + navBarBottom)
                    .graphicsLayer {
                        scaleX = fabVisibilityProgress
                        scaleY = fabVisibilityProgress
                        alpha = fabVisibilityProgress
                        if (!isLowRamDevice) {
                            shadowElevation = maxShadowElevationPx
                        }
                        shape = RoundedCornerShape(16.dp)
                        clip = true
                    }
                    .size(56.dp)
                    .background(primaryColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 28.dp),
                        enabled = showFab
                    ) {
                        haptic.perform(AppHapticFeedbackType.Medium)
                        onOpen()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // C. Circular Reveal Form Container: Blooms outward from a micro circle across the screen
        if (isOpen || progress > 0.005f) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val screenWidthPx = constraints.maxWidth.toFloat()
                val screenHeightPx = constraints.maxHeight.toFloat()
                val navBarBottomPx = with(density) { WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx() }

                // Origin coordinates for the expanding micro circle (starts at pinpoint 1.5dp, never big circle)
                val microRadius = with(density) { 1.5.dp.toPx() }
                val (centerX, centerY, startRadius) = remember(source, screenWidthPx, screenHeightPx, navBarBottomPx) {
                    when (source) {
                        MorphSource.FAB -> {
                            val sizePx = with(density) { 56.dp.toPx() }
                            val endPadPx = with(density) { 16.dp.toPx() }
                            val botMarginPx = with(density) { 92.dp.toPx() } + navBarBottomPx
                            val leftPx = screenWidthPx - endPadPx - sizePx
                            val topPx = screenHeightPx - botMarginPx - sizePx
                            val cX = leftPx + sizePx / 2f
                            val cY = topPx + sizePx / 2f
                            Triple(cX, cY, microRadius)
                        }
                        MorphSource.DASHBOARD_BUTTON -> {
                            val cX = screenWidthPx / 2f
                            val topPx = with(density) { 294.dp.toPx() }
                            val heightPx = with(density) { 54.dp.toPx() }
                            val cY = topPx + heightPx / 2f
                            Triple(cX, cY, microRadius)
                        }
                        MorphSource.DEFAULT -> {
                            Triple(screenWidthPx / 2f, screenHeightPx / 2f, microRadius)
                        }
                    }
                }

                // Maximum radius to reach the furthest corner of the screen (+5% padding for seamless coverage)
                val maxRadius = remember(centerX, centerY, screenWidthPx, screenHeightPx) {
                    hypot(
                        maxOf(centerX, screenWidthPx - centerX),
                        maxOf(centerY, screenHeightPx - centerY)
                    ) * 1.05f
                }

                val currentRadius = startRadius + (maxRadius - startRadius) * progress

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Hardware-accelerated GPU circular clip
                            shape = CircularRevealShape(
                                radius = currentRadius,
                                centerX = centerX,
                                centerY = centerY
                            )
                            clip = true

                            if (!isLowRamDevice && progress < 0.98f) {
                                shadowElevation = (maxShadowElevationPx * (1f - progress)).coerceAtLeast(0f)
                            }
                        }
                        // Draw expanding circular border ring of the button
                        .drawWithContent {
                            drawContent()
                            if (progress in 0.005f..0.985f) {
                                val maxStroke = with(density) { 2.5.dp.toPx() }
                                val strokeWidthPx = (currentRadius * 0.35f).coerceIn(with(density) { 1.dp.toPx() }, maxStroke)
                                val borderAlpha = ((1f - progress * 0.85f)).coerceIn(0f, 0.90f)
                                if (borderAlpha > 0.01f && currentRadius > strokeWidthPx) {
                                    drawCircle(
                                        color = primaryColor.copy(alpha = borderAlpha),
                                        radius = (currentRadius - strokeWidthPx / 2f).coerceAtLeast(0.1f),
                                        center = Offset(centerX, centerY),
                                        style = Stroke(width = strokeWidthPx)
                                    )
                                }
                            }
                        },
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Full Form Content (Crisp, zero distortion, smooth fade-in)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = (progress / 0.25f).coerceIn(0f, 1f)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            // Header Bar (Back button + Centered Title Badge + Close X)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptic.perform(AppHapticFeedbackType.Light)
                                        onClose()
                                    },
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        shadowElevation = 0.dp
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (editingTransactionId != null) Icons.Default.Edit else Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (editingTransactionId != null) "Edit Transaction" else "Add Transaction",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.2.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        haptic.perform(AppHapticFeedbackType.Light)
                                        onClose()
                                    },
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Full Add/Edit Transaction Form (Natural full-screen layout, perfectly crisp)
                            Box(modifier = Modifier.weight(1f)) {
                                AddEditTransactionScreen(
                                    viewModel = viewModel,
                                    transactionIdToEdit = editingTransactionId,
                                    onNavigateBack = onClose
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
