package com.example.expensetracker.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.example.expensetracker.utils.ValidationUtils
import kotlin.math.roundToInt

private data class OdometerCharItem(
    val key: String,
    val char: Char,
    val isDigit: Boolean,
    val digitValue: Int,
    val staggerIndex: Int
)

private fun parseOdometerItems(text: String): List<OdometerCharItem> {
    var dotIdx = -1
    for (i in text.indices.reversed()) {
        if (text[i] == '.' || text[i] == ',') {
            val remaining = text.substring(i + 1)
            if (remaining.all { it.isDigit() } && remaining.isNotEmpty() && remaining.length <= 3) {
                dotIdx = i
                break
            }
        }
    }

    val items = mutableListOf<OdometerCharItem>()
    val rightItems = mutableListOf<OdometerCharItem>()

    if (dotIdx != -1) {
        val decimals = text.substring(dotIdx + 1)
        for ((dIdx, c) in decimals.withIndex()) {
            rightItems.add(
                OdometerCharItem(
                    key = "dec_$dIdx",
                    char = c,
                    isDigit = c.isDigit(),
                    digitValue = if (c.isDigit()) c - '0' else -1,
                    staggerIndex = 0
                )
            )
        }
        rightItems.add(
            0,
            OdometerCharItem(
                key = "dot",
                char = text[dotIdx],
                isDigit = false,
                digitValue = -1,
                staggerIndex = 0
            )
        )
    }

    val intText = if (dotIdx != -1) text.substring(0, dotIdx) else text
    var digitCounter = 0
    var sepCounter = 0
    var symCounter = 0
    val leftItems = mutableListOf<OdometerCharItem>()

    for (c in intText.reversed()) {
        when {
            c.isDigit() -> {
                leftItems.add(
                    OdometerCharItem(
                        key = "int_$digitCounter",
                        char = c,
                        isDigit = true,
                        digitValue = c - '0',
                        staggerIndex = 0
                    )
                )
                digitCounter++
            }
            c == ',' || c == '.' || c == ' ' -> {
                leftItems.add(
                    OdometerCharItem(
                        key = "sep_$sepCounter",
                        char = c,
                        isDigit = false,
                        digitValue = -1,
                        staggerIndex = 0
                    )
                )
                sepCounter++
            }
            else -> {
                leftItems.add(
                    OdometerCharItem(
                        key = "sym_$symCounter",
                        char = c,
                        isDigit = false,
                        digitValue = -1,
                        staggerIndex = 0
                    )
                )
                symCounter++
            }
        }
    }

    leftItems.reverse()
    items.addAll(leftItems)
    items.addAll(rightItems)

    var digitStagger = 0
    return items.map { item ->
        if (item.isDigit) {
            item.copy(staggerIndex = digitStagger++)
        } else {
            item
        }
    }
}

/**
 * Universal High-Performance Rolling Number Odometer Text Component.
 *
 * Uses a custom Layout that measures all 10 digits (0..9) with exact fixed viewport
 * bounds, guaranteeing all digits 0-9 are 100% visible and smoothly animated.
 */
@Composable
fun RollingOdometerText(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    animateInitial: Boolean = false,
    animateChanges: Boolean = true,
    initialDelayMs: Int = 0,
    durationMs: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val formattedText = remember(amount, currency) {
        ValidationUtils.formatCurrency(amount, currency)
    }

    RollingOdometerText(
        formattedText = formattedText,
        modifier = modifier,
        animateInitial = animateInitial,
        animateChanges = animateChanges,
        initialDelayMs = initialDelayMs,
        durationMs = durationMs,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Composable
fun RollingOdometerText(
    formattedText: String,
    modifier: Modifier = Modifier,
    animateInitial: Boolean = false,
    animateChanges: Boolean = true,
    initialDelayMs: Int = 0,
    durationMs: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val isLowRamDevice = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice == true || (am?.memoryClass ?: 128) <= 128
    }

    val tabularStyle = remember(style) {
        style.copy(fontFeatureSettings = "tnum")
    }

    // Measure widest digit "8" to fix column width and prevent horizontal wobbling
    val digitMeasureResult = remember(tabularStyle, textMeasurer) {
        textMeasurer.measure("8", tabularStyle)
    }

    val digitWidth = remember(digitMeasureResult, density) {
        with(density) { digitMeasureResult.size.width.toDp() }
    }
    val digitHeight = remember(digitMeasureResult, density) {
        with(density) { digitMeasureResult.size.height.toDp() }
    }

    val items = remember(formattedText) {
        parseOdometerItems(formattedText)
    }

    val animDuration = durationMs ?: if (isLowRamDevice) 1150 else 1450
    val staggerStep = if (isLowRamDevice) 28 else 42

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (item in items) {
            key(item.key) {
                if (item.isDigit) {
                    OdometerDigitDrum(
                        key = item.key,
                        digit = item.digitValue,
                        digitWidth = digitWidth,
                        digitHeight = digitHeight,
                        style = tabularStyle,
                        color = color,
                        fontWeight = fontWeight,
                        animateInitial = animateInitial,
                        animateChanges = animateChanges,
                        staggerDelayMs = (item.staggerIndex * staggerStep).coerceAtMost(320),
                        initialDelayMs = initialDelayMs,
                        durationMs = animDuration
                    )
                } else {
                    Box(
                        modifier = Modifier.height(digitHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.char.toString(),
                            style = tabularStyle,
                            color = color,
                            fontWeight = fontWeight,
                            maxLines = maxLines,
                            overflow = overflow
                        )
                    }
                }
            }
        }
    }
}

/**
 * Silky-smooth easing curve for vertical odometer digit rolling:
 * Starts gently without a violent snap, maintains a fluid and readable roll through
 * intermediate digits, and eases into the target digit like silk.
 */
private val OdometerSmoothEasing = CubicBezierEasing(0.20f, 0.05f, 0.12f, 1.0f)

/**
 * Mechanical Drum Reel for a single digit column.
 * Uses custom Layout policy ensuring every digit 0-9 is measured at full viewport
 * height and translated via hardware RenderNode layer.
 */
@Composable
private fun OdometerDigitDrum(
    key: String,
    digit: Int,
    digitWidth: Dp,
    digitHeight: Dp,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight?,
    animateInitial: Boolean,
    animateChanges: Boolean,
    staggerDelayMs: Int,
    initialDelayMs: Int,
    durationMs: Int,
    modifier: Modifier = Modifier
) {
    val compositeKey = "${androidx.compose.runtime.currentCompositeKeyHash}_$key"
    val hasAnimatedInitial = androidx.compose.runtime.saveable.rememberSaveable(key = compositeKey) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val initialValue = if (animateInitial && !hasAnimatedInitial.value) 0f else digit.toFloat()
    val animatable = remember(key) { Animatable(initialValue) }

    LaunchedEffect(key, digit, animateChanges) {
        if (!hasAnimatedInitial.value && animateInitial) {
            hasAnimatedInitial.value = true
            val delay = initialDelayMs + staggerDelayMs
            animatable.animateTo(
                targetValue = digit.toFloat(),
                animationSpec = tween(
                    durationMillis = durationMs,
                    delayMillis = delay,
                    easing = OdometerSmoothEasing
                )
            )
        } else if (animatable.targetValue.roundToInt() != digit || animatable.value != digit.toFloat()) {
            if (animateChanges) {
                animatable.animateTo(
                    targetValue = digit.toFloat(),
                    animationSpec = tween(
                        durationMillis = durationMs,
                        delayMillis = staggerDelayMs,
                        easing = OdometerSmoothEasing
                    )
                )
            } else {
                animatable.snapTo(digit.toFloat())
            }
        }
    }

    Layout(
        content = {
            for (i in 0..9) {
                Text(
                    text = i.toString(),
                    style = style,
                    color = color,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Center
                )
            }
        },
        modifier = modifier
            .size(width = digitWidth, height = digitHeight)
            .clipToBounds()
    ) { measurables, constraints ->
        val singleDigitWidth = constraints.maxWidth
        val singleDigitHeight = constraints.maxHeight

        val childConstraints = Constraints.fixed(singleDigitWidth, singleDigitHeight)
        val currentVal = animatable.value
        val isStatic = kotlin.math.abs(currentVal - currentVal.roundToInt()) < 0.001f

        if (isStatic) {
            // Stationary resting state: measure and place strictly the single target digit (90% measurement reduction)
            val targetDigit = currentVal.roundToInt().coerceIn(0, 9)
            val placeable = measurables[targetDigit].measure(childConstraints)
            layout(singleDigitWidth, singleDigitHeight) {
                placeable.placeWithLayer(0, 0)
            }
        } else {
            // Active rolling state: only measure the 1-2 digits intersecting visible viewport
            val minIndex = kotlin.math.floor(currentVal).toInt().coerceIn(0, 9)
            val maxIndex = kotlin.math.ceil(currentVal).toInt().coerceIn(0, 9)
            val measured = mutableListOf<Pair<Int, androidx.compose.ui.layout.Placeable>>()
            for (i in minIndex..maxIndex) {
                measured.add(i to measurables[i].measure(childConstraints))
            }
            layout(singleDigitWidth, singleDigitHeight) {
                val yOffset = (-currentVal * singleDigitHeight).roundToInt()
                for ((idx, placeable) in measured) {
                    val y = yOffset + idx * singleDigitHeight
                    placeable.placeWithLayer(0, y)
                }
            }
        }
    }
}
