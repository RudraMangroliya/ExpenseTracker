package com.example.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class RollingOdometerLayoutTest {
    @Test
    fun testDigitPlacementMath() {
        val singleDigitHeight = 40
        for (targetDigit in 0..9) {
            val animValue = targetDigit.toFloat()
            val yOffset = (-animValue * singleDigitHeight).roundToInt()
            
            // Check where targetDigit is placed
            val targetY = yOffset + targetDigit * singleDigitHeight
            assertEquals("Target digit $targetDigit must be at y=0", 0, targetY)
            
            // Check that target digit is visible in window [0, singleDigitHeight]
            val isVisible = (targetY + singleDigitHeight > 0 && targetY < singleDigitHeight)
            assertTrue("Target digit $targetDigit must be visible", isVisible)
        }
    }

    @Test
    fun testTransitionHalfwayMath() {
        val singleDigitHeight = 40
        val animValue = 2.5f // halfway between 2 and 3
        val yOffset = (-animValue * singleDigitHeight).roundToInt()
        
        val digit2Y = yOffset + 2 * singleDigitHeight // -20
        val digit3Y = yOffset + 3 * singleDigitHeight // +20
        
        // Both digit 2 and digit 3 are visible in window [0, 40]
        assertTrue(digit2Y + singleDigitHeight > 0 && digit2Y < singleDigitHeight)
        assertTrue(digit3Y + singleDigitHeight > 0 && digit3Y < singleDigitHeight)
        
        // Digit 1 and digit 4 are NOT visible
        val digit1Y = yOffset + 1 * singleDigitHeight // -60
        val digit4Y = yOffset + 4 * singleDigitHeight // +60
        assertTrue(digit1Y + singleDigitHeight <= 0)
        assertTrue(digit4Y >= singleDigitHeight)
    }
}
