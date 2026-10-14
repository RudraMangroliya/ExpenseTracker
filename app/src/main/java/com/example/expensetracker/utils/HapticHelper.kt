package com.example.expensetracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

val LocalHapticFeedbackEnabled = compositionLocalOf { true }

enum class AppHapticFeedbackType {
    Light,      // Quick tap / button press / tab change / chip selection
    Medium,     // Slider thumb switch / segmented button change
    Success,    // Transaction created / sample data seeded
    Heavy       // Delete item / destructive action / database reset
}

class AppHapticFeedback(
    private val hapticFeedback: HapticFeedback,
    private val isEnabled: Boolean
) {
    fun perform(type: AppHapticFeedbackType = AppHapticFeedbackType.Light) {
        if (!isEnabled) return
        when (type) {
            AppHapticFeedbackType.Light -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            AppHapticFeedbackType.Medium -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            AppHapticFeedbackType.Success -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            AppHapticFeedbackType.Heavy -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberAppHapticFeedback(): AppHapticFeedback {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticFeedbackEnabled.current
    return remember(haptic, enabled) {
        AppHapticFeedback(haptic, enabled)
    }
}
