package com.example.expensetracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { false }

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// User Brand Palette - 4 Harmonious Sage Colors
// 1. #5C7057 - rgb(92, 112, 87) - Deep Sage / Forest Olive
val SageForest = Color(0xFF5C7057)
// 2. #89A482 - rgb(137, 164, 130) - Medium Olive Sage
val SageMoss = Color(0xFF89A482)
// 3. #ACC5A6 - rgb(172, 197, 166) - Soft Light Mint Sage
val SageMint = Color(0xFFACC5A6)
// 4. #D1EDD3 - rgb(209, 237, 211) - Pale Celadon / Pastel Mint
val SageCeladon = Color(0xFFD1EDD3)

// Primary Brand Colors
val PrimaryLight = SageForest // #5C7057 (Strong contrast on white: 5.4:1)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = SageCeladon // #D1EDD3 (Refreshing pale celadon highlight)
val OnPrimaryContainerLight = Color(0xFF192517)

val PrimaryDark = SageMint // #ACC5A6 (Luminous sage accent on dark: 9.1:1 contrast)
val OnPrimaryDark = Color(0xFF131D12)
val PrimaryContainerDark = Color(0xFF384735) // Deep rich forest container
val OnPrimaryContainerDark = SageCeladon // #D1EDD3

// Secondary Colors
val SecondaryLight = SageMoss // #89A482
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE2EFE0)
val OnSecondaryContainerLight = Color(0xFF1E2C1C)

val SecondaryDark = SageMoss // #89A482
val OnSecondaryDark = Color(0xFF131D12)
val SecondaryContainerDark = Color(0xFF283626)
val OnSecondaryContainerDark = SageCeladon // #D1EDD3

// Tertiary Colors
val TertiaryLight = Color(0xFF4C6848)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = SageMint // #ACC5A6
val OnTertiaryContainerLight = Color(0xFF142413)

val TertiaryDark = SageCeladon // #D1EDD3
val OnTertiaryDark = Color(0xFF142413)
val TertiaryContainerDark = SageForest // #5C7057
val OnTertiaryContainerDark = SageCeladon // #D1EDD3

// Background & Surface Colors (Sage / Forest Theme)
val BackgroundLight = Color(0xFFF6F8F5) // Airy, clean natural sage-tinted off-white
val SurfaceLight = Color(0xFFFFFFFF) // Pure clean white cards & sheets
val SurfaceVariantLight = Color(0xFFE4EDE2)
val OnBackgroundLight = Color(0xFF161E15)
val OnSurfaceLight = Color(0xFF161E15)
val OnSurfaceVariantLight = Color(0xFF4F5F4D)
val OutlineLight = Color(0xFFBCCBB9)
val OutlineVariantLight = Color(0xFFDBE5D9)

val BackgroundDark = Color(0xFF0F150E) // Deep OLED obsidian with subtle forest undertone
val SurfaceDark = Color(0xFF172016) // Elevated dark sage card surface
val SurfaceVariantDark = Color(0xFF222C21)
val OnBackgroundDark = Color(0xFFE4EDE3)
val OnSurfaceDark = Color(0xFFEFF5EE)
val OnSurfaceVariantDark = Color(0xFFA5B5A2)
val OutlineDark = Color(0xFF3C4B3A)
val OutlineVariantDark = Color(0xFF273426)

object AppColors {
    // Income Colors
    val IncomeLight = Color(0xFF2E7D32)
    val IncomeDark = Color(0xFF81C784) // Vibrant, high-contrast light green for dark surfaces

    val IncomeContainerLight = Color(0xFFE8F5E9)
    val IncomeContainerDark = Color(0xFF1B3B2B) // Clean emerald dark container

    // Expense Colors
    val ExpenseLight = Color(0xFFC62828)
    val ExpenseDark = Color(0xFFFF8A80) // High-contrast soft coral red for dark surfaces

    val ExpenseContainerLight = Color(0xFFFFEBEE)
    val ExpenseContainerDark = Color(0xFF3E1F24) // Clean dark wine container

    // Warning Colors
    val WarningLight = Color(0xFFE65100)
    val WarningDark = Color(0xFFFFB74D)

    // Progress Bar Safe Colors
    val SafeGreenLight = Color(0xFF43A047)
    val SafeGreenDark = Color(0xFF81C784)

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    val income: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) IncomeDark else IncomeLight

    val incomeContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) IncomeContainerDark else IncomeContainerLight

    val expense: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) ExpenseDark else ExpenseLight

    val expenseContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) ExpenseContainerDark else ExpenseContainerLight

    val warning: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) WarningDark else WarningLight

    val safeGreen: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalIsDarkTheme.current) SafeGreenDark else SafeGreenLight
}