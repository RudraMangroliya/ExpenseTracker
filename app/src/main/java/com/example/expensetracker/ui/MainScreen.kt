package com.example.expensetracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.data.local.AppTheme
import com.example.expensetracker.ui.budget.BudgetViewModel
import com.example.expensetracker.ui.budget.BudgetsScreen
import com.example.expensetracker.ui.dashboard.DashboardScreen
import com.example.expensetracker.ui.dashboard.DashboardViewModel
import com.example.expensetracker.ui.navigation.Screen
import com.example.expensetracker.ui.reports.ReportsScreen
import com.example.expensetracker.ui.reports.ReportsViewModel
import com.example.expensetracker.ui.settings.SettingsScreen
import com.example.expensetracker.ui.settings.SettingsViewModel
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.transactions.AddEditTransactionScreen
import com.example.expensetracker.ui.transactions.AddEditTransactionViewModel
import com.example.expensetracker.ui.transactions.TransactionsScreen
import com.example.expensetracker.ui.transactions.TransactionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as? ExpenseTrackerApp ?: return

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    // ViewModel instances for home & bottom sheet
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            transactionRepository = app.transactionRepository,
            budgetRepository = app.budgetRepository,
            userPreferencesRepository = app.userPreferencesRepository
        )
    )

    val addEditTransactionViewModel: AddEditTransactionViewModel = viewModel(
        factory = AddEditTransactionViewModel.Factory(
            transactionRepository = app.transactionRepository,
            categoryRepository = app.categoryRepository,
            budgetRepository = app.budgetRepository,
            exchangeRateRepository = app.exchangeRateRepository,
            userPreferencesRepository = app.userPreferencesRepository,
            context = app
        )
    )

    val transactionsViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.Factory(
            transactionRepository = app.transactionRepository,
            categoryRepository = app.categoryRepository,
            userPreferencesRepository = app.userPreferencesRepository
        )
    )

    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModel.Factory(
            budgetRepository = app.budgetRepository,
            transactionRepository = app.transactionRepository,
            categoryRepository = app.categoryRepository,
            userPreferencesRepository = app.userPreferencesRepository
        )
    )

    val reportsViewModel: ReportsViewModel = viewModel(
        factory = ReportsViewModel.Factory(
            transactionRepository = app.transactionRepository,
            budgetRepository = app.budgetRepository,
            userPreferencesRepository = app.userPreferencesRepository
        )
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            userPreferencesRepository = app.userPreferencesRepository,
            exchangeRateRepository = app.exchangeRateRepository,
            transactionRepository = app.transactionRepository,
            budgetRepository = app.budgetRepository,
            categoryRepository = app.categoryRepository,
            appDatabase = app.database,
            context = app
        )
    )

    // Request notification permission on app open if not yet granted (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            app.userPreferencesRepository.setNotificationsEnabled(true)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            kotlinx.coroutines.delay(1200)
            if (!com.example.expensetracker.utils.NotificationHelper.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Observe theme & haptic preferences directly from UserPreferencesRepository (instant, lightweight)
    val appTheme by app.userPreferencesRepository.theme.collectAsState()
    val hapticFeedbackEnabled by app.userPreferencesRepository.hapticFeedbackEnabled.collectAsState()
    val isDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    ExpenseTrackerTheme(darkTheme = isDarkTheme, dynamicColor = false) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.example.expensetracker.utils.LocalHapticFeedbackEnabled provides hapticFeedbackEnabled
        ) {
            val showBottomBar = Screen.bottomNavScreens.any { it.route == currentRoute }
            val showFab = currentRoute == Screen.Dashboard.route || currentRoute == Screen.Transactions.route
            val isEditRoute = currentRoute.startsWith("edit_transaction")
            val isAddRoute = currentRoute == Screen.AddTransaction.route
            val isFormRoute = isAddRoute || isEditRoute

            val topScrimBrush = remember(isDarkTheme) {
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF151916) else androidx.compose.ui.graphics.Color(0xFFFBFDF8),
                        (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF151916) else androidx.compose.ui.graphics.Color(0xFFFBFDF8)).copy(alpha = 0.70f),
                        (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF151916) else androidx.compose.ui.graphics.Color(0xFFFBFDF8)).copy(alpha = 0.25f),
                        androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }

            val bottomScrimBrush = remember(isDarkTheme) {
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color.Transparent,
                        (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF151916) else androidx.compose.ui.graphics.Color(0xFFFBFDF8)).copy(alpha = 0.20f),
                        (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF151916) else androidx.compose.ui.graphics.Color(0xFFFBFDF8)).copy(alpha = 0.50f),
                        (if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF151916) else androidx.compose.ui.graphics.Color(0xFFFBFDF8)).copy(alpha = 0.75f)
                    )
                )
            }

            val navigateToTab: (String) -> Unit = { route ->
                if (currentRoute != route) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            var isAddSheetOpen by remember { mutableStateOf(false) }
            var addSheetSource by remember { mutableStateOf(com.example.expensetracker.ui.components.MorphSource.FAB) }
            var editingTransactionId by remember { mutableStateOf<Long?>(null) }

            val openAddTransaction: (com.example.expensetracker.ui.components.MorphSource) -> Unit = { source ->
                addSheetSource = source
                editingTransactionId = null
                addEditTransactionViewModel.prepareForAdd()
                isAddSheetOpen = true
            }

            val openEditTransaction: (Long) -> Unit = { id ->
                addSheetSource = com.example.expensetracker.ui.components.MorphSource.DEFAULT
                editingTransactionId = id
                addEditTransactionViewModel.loadTransactionForEdit(id)
                isAddSheetOpen = true
            }

            val closeAddSheet: () -> Unit = {
                isAddSheetOpen = false
                editingTransactionId = null
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                topBar = {
                    val currentHeaderTitle: String
                    val currentHeaderIcon: androidx.compose.ui.graphics.vector.ImageVector
                    when {
                        isAddRoute -> {
                            currentHeaderTitle = "Add Transaction"
                            currentHeaderIcon = Icons.Default.Add
                        }
                        isEditRoute -> {
                            currentHeaderTitle = "Edit Transaction"
                            currentHeaderIcon = Icons.Default.Edit
                        }
                        else -> {
                            val screen = Screen.bottomNavScreens.find { it.route == currentRoute }
                            currentHeaderTitle = screen?.title ?: Screen.Dashboard.title
                            currentHeaderIcon = screen?.icon ?: Screen.Dashboard.icon
                        }
                    }

                    CenterAlignedTopAppBar(
                        title = {
                            androidx.compose.animation.AnimatedContent(
                                targetState = Pair(currentHeaderTitle, currentHeaderIcon),
                                transitionSpec = {
                                    (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                                     androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)))
                                        .togetherWith(
                                            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                                            androidx.compose.animation.scaleOut(targetScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                                        )
                                },
                                label = "topAppBarTitleAnim"
                            ) { (titleText, headerIcon) ->
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    ),
                                    shadowElevation = 0.dp
                                ) {
                                    androidx.compose.foundation.layout.Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = headerIcon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                                        Text(
                                            text = titleText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.2.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        navigationIcon = {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isFormRoute,
                                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
                                        androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(220)),
                                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) +
                                       androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(180))
                            ) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier
                                        .padding(start = 8.dp)
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
                            }
                        },
                        actions = {
                            if (isFormRoute) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
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
                        }
                    )
                },
                floatingActionButton = {}
            ) { innerPadding ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding()),
                        enterTransition = {
                            androidx.compose.animation.fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            )
                        },
                        exitTransition = {
                            androidx.compose.animation.fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                            )
                        },
                        popEnterTransition = {
                            androidx.compose.animation.fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            )
                        },
                        popExitTransition = {
                            androidx.compose.animation.fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                            )
                        }
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToAddTransaction = { openAddTransaction(com.example.expensetracker.ui.components.MorphSource.DASHBOARD_BUTTON) },
                                onNavigateToTransactions = { navigateToTab(Screen.Transactions.route) },
                                onNavigateToBudgets = { navigateToTab(Screen.Budgets.route) }
                            )
                        }

                        composable(Screen.Transactions.route) {
                            TransactionsScreen(
                                viewModel = transactionsViewModel,
                                onNavigateToAddTransaction = { openAddTransaction(com.example.expensetracker.ui.components.MorphSource.FAB) },
                                onNavigateToEditTransaction = openEditTransaction
                            )
                        }

                        composable(Screen.Budgets.route) {
                            BudgetsScreen(viewModel = budgetViewModel)
                        }

                        composable(Screen.Reports.route) {
                            ReportsScreen(
                                viewModel = reportsViewModel,
                                onNavigateToAddTransaction = { openAddTransaction(com.example.expensetracker.ui.components.MorphSource.DEFAULT) }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(viewModel = settingsViewModel)
                        }

                        composable(
                            route = Screen.AddTransaction.route,
                            enterTransition = {
                                slideIntoContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Up,
                                    animationSpec = androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(260))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220))
                            },
                            popEnterTransition = {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220))
                            }
                        ) {
                            AddEditTransactionScreen(
                                viewModel = addEditTransactionViewModel,
                                transactionIdToEdit = null,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.EditTransaction.route,
                            arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
                            enterTransition = {
                                slideIntoContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Up,
                                    animationSpec = androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(260))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220))
                            },
                            popEnterTransition = {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Down,
                                    animationSpec = androidx.compose.animation.core.tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220))
                            }
                        ) { backStackEntry ->
                            val txId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                            AddEditTransactionScreen(
                                viewModel = addEditTransactionViewModel,
                                transactionIdToEdit = txId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // 1. Soft Top Gradient Scrim (High Opacity -> Low/Transparent) below Top Header
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = innerPadding.calculateTopPadding())
                            .height(28.dp)
                            .background(topScrimBrush)
                    )

                    // 2. Soft Bottom Gradient Scrim (Transparent -> Frosted Translucency) above and behind Floating Dock
                    if (showBottomBar) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(115.dp)
                                .background(bottomScrimBrush)
                        )

                        // Floating Dock Bar overlaid cleanly over the gradient scrim
                        com.example.expensetracker.ui.components.FloatingDockBar(
                            currentRoute = currentRoute,
                            onNavigateToTab = { route -> navigateToTab(route) },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            // Material 3 Container Transform: Morphing FAB / Button -> Add/Edit Transaction Sheet
            com.example.expensetracker.ui.components.MorphingFabSheet(
                isOpen = isAddSheetOpen,
                showFab = showFab,
                source = addSheetSource,
                editingTransactionId = editingTransactionId,
                viewModel = addEditTransactionViewModel,
                onOpen = { openAddTransaction(com.example.expensetracker.ui.components.MorphSource.FAB) },
                onClose = closeAddSheet
            )
        }
    }
}
}
