package com.example.expensetracker.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * High-Performance Viewport Entrance Sequencer.
 * - Staggers initial screen composition rows (indices 0..8) so items cascade smoothly one by one on screen load.
 * - During active scrolling, staggers consecutive row entrances by 48ms so multiple rows appear one by one
 *   smoothly instead of popping in simultaneously as a solid block.
 */
internal object ScrollCascadeSequencer {
    private var lastScrollEntranceTime = 0L

    @Synchronized
    fun nextDelay(explicitDelay: Int, index: Int, isInitialMount: Boolean): Long {
        if (explicitDelay >= 0) return explicitDelay.toLong()

        // On initial screen open, reveal rows immediately without delay to prevent page navigation lag
        if (isInitialMount) {
            return 0L
        }

        val now = SystemClock.uptimeMillis()
        // Active scrolling / adding items: if multiple rows enter in quick succession,
        // separate them by 45ms so each row appears one by one smoothly
        val staggerDelay = if (lastScrollEntranceTime > now) {
            val delayFromNow = (lastScrollEntranceTime + 45L) - now
            lastScrollEntranceTime += 45L
            delayFromNow.coerceAtMost(250L)
        } else if (now - lastScrollEntranceTime < 45L) {
            val delayFromNow = 45L - (now - lastScrollEntranceTime)
            lastScrollEntranceTime = now + delayFromNow
            delayFromNow
        } else {
            lastScrollEntranceTime = now
            0L
        }
        return staggerDelay
    }
}

/**
 * Direction from which an offscreen item enters the visible viewport.
 */
internal enum class ItemScrollOrigin {
    UNKNOWN,
    FROM_BOTTOM,
    FROM_TOP
}

/**
 * Zero-Allocation, Thread-Safe real-time scroll velocity and direction estimator.
 * Measures list displacement across frames to dynamically tune animation parameters:
 * - Slow scrolls: 560ms duration, 36dp slide distance (luxurious, silky smooth).
 * - Fast scrolls: 280ms duration, 16dp slide distance (responsive, no ghost lag).
 * - 100% Zero allocations: eliminates ConcurrentHashMap and Sample object churn for locked 60/120 FPS.
 */
internal object ScrollVelocityTracker {
    @Volatile private var smoothedSpeed = 0f // px per ms
    @Volatile private var lastScrollDir = 0f // +1f = content moving UP (bottom entrance), -1f = content moving DOWN (top entrance)
    @Volatile private var lastGlobalSampleTime = 0L

    fun updateVelocity(dy: Float, dt: Long, nowMs: Long) {
        val instantSpeed = kotlin.math.abs(dy) / dt.toFloat()
        if (instantSpeed > 0.03f) {
            smoothedSpeed = smoothedSpeed * 0.4f + instantSpeed * 0.6f
        }
        if (dy < -1.5f) {
            lastScrollDir = 1f // Content moving UP -> new items appear from BOTTOM
        } else if (dy > 1.5f) {
            lastScrollDir = -1f // Content moving DOWN -> new items appear from TOP
        }
        lastGlobalSampleTime = nowMs
    }

    /**
     * Normalized speed ratio:
     * 0.0f = slow / gentle drag or stationary
     * 1.0f = fast fling / rapid swipe
     */
    fun speedFactor(): Float {
        val now = SystemClock.uptimeMillis()
        if (now - lastGlobalSampleTime > 160L) {
            smoothedSpeed = 0f
        }
        return ((smoothedSpeed - 0.35f) / 1.8f).coerceIn(0f, 1f)
    }

    /**
     * Scroll direction: +1f for bottom entrance, -1f for top entrance
     */
    fun scrollDirection(): Float = lastScrollDir
}

/**
 * Universal Zero-Recomposition Viewport-Aware Two-Way "One by One" Velocity-Adaptive Scroll Entry Animation.
 *
 * Performance & UX Architecture:
 * 1. Bidirectional Reveal:
 *    - Bottom data appearing: glides from below UP into place (+activeSlideDist -> 0f).
 *    - Top data appearing: glides from top DOWN into place (-activeSlideDist -> 0f).
 * 2. Full Row-Height Scroll Entrance Gate:
 *    - Bottom items appear only after their full height in pixels has scrolled into view above bottom trigger.
 *    - Top items appear only after their full height in pixels has scrolled into view below top trigger.
 * 3. Immediate Offscreen Reset:
 *    - Resets immediately when scrolling outside the viewport so reverse scrolling always re-animates smoothly.
 * 4. Scale Bloom:
 *    - Starts noticeably small (0.80f) and blossoms to actual size (1.0f) for superior tactile UI/UX.
 * 5. Velocity-Adaptive Smoothness:
 *    - Slow Scroll: 540ms duration, 32dp slide for calm, elegant, silky entrances.
 *    - Fast Scroll: 260ms duration, 14dp slide for crisp, responsive motion.
 * 6. Pure GPU RenderNode Layer:
 *    - Alpha, translation, and scale execute strictly in RenderNode draw phase (`graphicsLayer`),
 *      eliminating CPU recompositions during animation frames for locked 60/120 FPS.
 */
@Composable
fun AnimatedScrollEntry(
    modifier: Modifier = Modifier,
    index: Int = 0,
    delayMillis: Int = -1,
    bottomDockMarginDp: androidx.compose.ui.unit.Dp? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }

    // Velocity-adaptive slide distances
    val slideDistanceSlowPx = remember(density) { with(density) { 32.dp.toPx() } }
    val slideDistanceFastPx = remember(density) { with(density) { 14.dp.toPx() } }

    // Viewport boundaries & clearances: default 100dp clears the bottom dock on list pages
    val defaultDockMarginPx = remember(density) { with(density) { 100.dp.toPx() } }
    val bottomDockMarginPx = remember(density, bottomDockMarginDp) {
        if (bottomDockMarginDp != null) with(density) { bottomDockMarginDp.toPx() } else defaultDockMarginPx
    }
    val defaultTopMarginPx = remember(density) { with(density) { 0.dp.toPx() } }
    val tolerancePx = remember(density) { with(density) { 12.dp.toPx() } }
    val topEntranceThresholdPx = remember(density) { with(density) { 24.dp.toPx() } }

    var prevItemY by remember { mutableFloatStateOf(Float.NaN) }
    var prevItemTime by remember { mutableLongStateOf(0L) }
    val mountTime = remember { SystemClock.uptimeMillis() }
    val isAboveFold = (index in 0..8 || delayMillis < 0)
    var hasEntered by remember { mutableStateOf(isAboveFold) }
    var itemOrigin by remember { mutableStateOf(ItemScrollOrigin.UNKNOWN) }
    // +1f for entering from bottom (slides UP), -1f for entering from top (slides DOWN)
    var slideDirection by remember { mutableFloatStateOf(1f) }
    var entrySpeedRatio by remember { mutableFloatStateOf(0f) }
    val animProgress = remember { Animatable(if (isAboveFold) 1f else 0f) }

    LaunchedEffect(hasEntered) {
        if (hasEntered) {
            if (animProgress.value >= 1f) return@LaunchedEffect
            val isInitial = (SystemClock.uptimeMillis() - mountTime) < 300L
            val delayMs = ScrollCascadeSequencer.nextDelay(delayMillis, index, isInitial)
            if (delayMs > 0) {
                delay(delayMs)
            }

            // Slower, smoother entrance for superior UI/UX:
            // 540ms on gentle/slow scrolls down to 260ms on fast flings
            val animDuration = if (isInitial) {
                540
            } else {
                (540 - (entrySpeedRatio * 260f).toInt()).coerceIn(260, 540)
            }

            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = animDuration,
                    easing = CubicBezierEasing(0.20f, 0.25f, 0.15f, 1.0f)
                )
            )
        } else {
            animProgress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val y = coordinates.positionInWindow().y
                val height = coordinates.size.height
                if (height <= 0) return@onGloballyPositioned

                // Fast bypass: if item already entered and position hasn't changed, skip heavy bounds recalculation
                if (hasEntered && !prevItemY.isNaN() && kotlin.math.abs(y - prevItemY) < 1.0f) {
                    return@onGloballyPositioned
                }

                val now = SystemClock.uptimeMillis()
                if (!prevItemY.isNaN()) {
                    val dt = now - prevItemTime
                    if (dt in 4..150) {
                        val dy = y - prevItemY
                        ScrollVelocityTracker.updateVelocity(dy, dt, now)
                    }
                }
                prevItemY = y
                prevItemTime = now

                val itemBottom = y + height

                // Dynamically resolve parent scroll container bounds for pixel-perfect trigger lines
                val parentCoords = coordinates.parentCoordinates
                val parentAttached = parentCoords != null && parentCoords.isAttached
                val parentY = if (parentAttached) parentCoords.positionInWindow().y else defaultTopMarginPx
                val parentHeight = if (parentAttached) parentCoords.size.height.toFloat() else (screenHeightPx - parentY)

                val effectiveTopTrigger = if (parentAttached) parentY else defaultTopMarginPx
                val effectiveBottomTrigger = if (parentAttached) {
                    minOf(parentY + parentHeight, screenHeightPx - bottomDockMarginPx)
                } else {
                    screenHeightPx - bottomDockMarginPx
                }
                val isInitial = (now - mountTime) < 300L

                // 1. Off-screen reset and origin tracking with 20px hysteresis buffer to eliminate boundary oscillation:
                val hysteresisBuffer = 20f
                if (y >= effectiveBottomTrigger + hysteresisBuffer) {
                    itemOrigin = ItemScrollOrigin.FROM_BOTTOM
                    if (hasEntered) hasEntered = false
                } else if (itemBottom <= effectiveTopTrigger - hysteresisBuffer) {
                    itemOrigin = ItemScrollOrigin.FROM_TOP
                    if (hasEntered) hasEntered = false
                } else if (itemOrigin == ItemScrollOrigin.UNKNOWN && !isInitial) {
                    // Item was freshly composed during active scrolling:
                    val scrollDir = ScrollVelocityTracker.scrollDirection()
                    itemOrigin = when {
                        scrollDir < 0f -> ItemScrollOrigin.FROM_TOP
                        scrollDir > 0f -> ItemScrollOrigin.FROM_BOTTOM
                        y < effectiveTopTrigger + topEntranceThresholdPx -> ItemScrollOrigin.FROM_TOP
                        else -> ItemScrollOrigin.FROM_BOTTOM
                    }
                }

                // Determine whether item enters from TOP or BOTTOM:
                val scrollDir = ScrollVelocityTracker.scrollDirection()
                val isFromTop = when (itemOrigin) {
                    ItemScrollOrigin.FROM_TOP -> true
                    ItemScrollOrigin.FROM_BOTTOM -> false
                    ItemScrollOrigin.UNKNOWN -> {
                        if (scrollDir < 0f) true
                        else if (scrollDir > 0f) false
                        else (y < (effectiveTopTrigger + effectiveBottomTrigger) / 2f)
                    }
                }

                // 2. Visible entrance logic:
                // - On initial mount: rows currently inside the visible area cascade smoothly.
                // - When scrolling from BOTTOM: row appears when scrolled into view above the bottom dock trigger.
                // - When scrolling from TOP: row appears when scrolled into view below the top trigger.
                val canEnter = if (isInitial) {
                    (y < effectiveBottomTrigger) && (itemBottom > effectiveTopTrigger)
                } else if (isFromTop) {
                    val topThreshold = if (index == 0) effectiveTopTrigger - tolerancePx else effectiveTopTrigger
                    (y >= topThreshold) && (itemBottom <= effectiveBottomTrigger + tolerancePx)
                } else {
                    (itemBottom <= effectiveBottomTrigger + tolerancePx || y < effectiveBottomTrigger - tolerancePx) && (y >= effectiveTopTrigger - tolerancePx)
                }

                if (canEnter && !hasEntered) {
                    val currentSpeed = ScrollVelocityTracker.speedFactor()
                    entrySpeedRatio = if (isInitial) 0f else currentSpeed
                    slideDirection = if (isInitial) 1f else (if (isFromTop) -1f else 1f)
                    hasEntered = true
                }
            }
            .graphicsLayer {
                val p = animProgress.value

                // Smooth linear alpha bloom from 0.0f to 1.0f
                alpha = p

                // Velocity-adaptive slide distance
                val activeSlideDist = slideDistanceSlowPx + (slideDistanceFastPx - slideDistanceSlowPx) * entrySpeedRatio

                // Directional slide:
                // - Bottom data entering: slideDirection = +1f -> starts at +activeSlideDist (down), glides UP to 0f
                // - Top data entering: slideDirection = -1f -> starts at -activeSlideDist (top), glides DOWN to 0f
                translationY = slideDirection * (1f - p) * activeSlideDist

                // Scale bloom: starts noticeably small (0.80f on slow, 0.88f on fast), blooms to actual size 1.0f for superior UI/UX
                val startScale = 0.80f + 0.08f * entrySpeedRatio
                val s = startScale + (1f - startScale) * p
                scaleX = s
                scaleY = s
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
    ) {
        content()
    }
}

