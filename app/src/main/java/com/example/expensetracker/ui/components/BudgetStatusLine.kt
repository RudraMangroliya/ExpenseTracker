package com.example.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Segmented status line showing:
 * 1. Left section: Spent progress capsule pill
 * 2. Gap between sections
 * 3. Right section: Remaining budget capsule pill with a target limit marker dot at the right edge
 *
 * Matching the exact visual style from the user's reference images.
 */
@Composable
fun BudgetStatusLine(
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    trackColor: Color = Color.White
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = maxWidth
        val clampedProgress = progress.coerceIn(0f, 1f)
        val gap = 6.dp
        val minPillWidth = height
        val dotSize = (height * 0.45f).coerceIn(5.dp, 7.dp)

        if (clampedProgress >= 1f) {
            // Exceeded or 100%: Single continuous capsule pill in progressColor
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(progressColor)
            )
        } else if (clampedProgress <= 0f) {
            // 0% spent: full track capsule with target limit dot at the right end
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(trackColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = (height * 0.35f).coerceAtLeast(4.dp))
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }
        } else {
            // Two separate pill sections as shown in reference images
            val availableWidth = (totalWidth - gap).coerceAtLeast(minPillWidth * 2)
            val spentWidth = (availableWidth * clampedProgress).coerceIn(minPillWidth, availableWidth - minPillWidth)

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section 1: Spent Pill (Green / ProgressColor)
                Box(
                    modifier = Modifier
                        .width(spentWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(progressColor)
                )

                Spacer(modifier = Modifier.width(gap))

                // Section 2: Remaining Track Pill (White) with Target Limit Dot on far right
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(trackColor),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = (height * 0.35f).coerceAtLeast(4.dp))
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(progressColor)
                    )
                }
            }
        }
    }
}
