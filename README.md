# 💰 MR Tracker — Modern Expense & Budget Manager

[![Download APK](https://img.shields.io/badge/Download-MR%20Tracker%20v1.0.1%20APK-brightgreen?style=for-the-badge&logo=android)](https://github.com/RudraMangroliya/ExpenseTracker/releases/download/v1.0.1/MR-Tracker-v1.0.1.apk)
[![Latest Release](https://img.shields.io/github/v/release/RudraMangroliya/ExpenseTracker?color=blue&label=Latest%20Version&style=for-the-badge)](https://github.com/RudraMangroliya/ExpenseTracker/releases/latest)

[![Android](https://img.shields.io/badge/Android-7.0%2B%20%28API%2024%2B%29-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

A production-grade, privacy-first personal finance application for Android built with **Jetpack Compose (Material 3)**, **Room (SQLite)**, **Retrofit**, and modern **MVVM + Clean Architecture**.

---

## 📲 Download & Install the App

Anyone can install and run **MR Tracker** directly on their Android phone without needing Android Studio or developer tools:

| 🚀 Action | Link |
| :--- | :--- |
| **Direct APK Download** | [📥 **Download MR-Tracker-v1.0.1.apk** (19.8 MB)](https://github.com/RudraMangroliya/ExpenseTracker/releases/download/v1.0.1/MR-Tracker-v1.0.1.apk) |
| **All Releases & Changelog** | [🏷️ **View All GitHub Releases**](https://github.com/RudraMangroliya/ExpenseTracker/releases) |

### 📋 Installation Guide (Step-by-Step):
1. **Download**: Tap the [Direct APK Download Link](https://github.com/RudraMangroliya/ExpenseTracker/releases/download/v1.0.1/MR-Tracker-v1.0.1.apk) on your Android device (Chrome or any browser).
2. **Open**: Tap the download completed notification, or open your phone's **Files / Downloads** app and tap `MR-Tracker-v1.0.1.apk`.
3. **Allow Installation**:
   - If prompted: *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings**.
   - Turn ON **"Allow from this source"**.
   - Go back and tap **Install**.
4. **Done!** Launch **MR Tracker** and start tracking your expenses!

---

## ✨ Key Features

- 💎 **Liquid Glass UI & Floating Dock**: Custom 120 FPS glassmorphic floating navigation dock with smooth page transitions and micro-animations.
- 💰 **Comprehensive Dashboard**: Real-time Total Balance, Income, Expense, Monthly Spending Progress, and Recent Transactions at a single glance.
- 🔍 **Smart Filters & Search**: Search across notes, amounts, categories, dates, and split bill participants. Filter by payment method, income/expense, and sort orders.
- 👥 **Bill Splitting**: Split group expenses easily and generate instant reminder summaries shareable via WhatsApp or SMS.
- 🎯 **Category & Monthly Budgets**: Set overall monthly limits and individual category budgets with color-coded warning meters (Green, Amber, Red).
- 📊 **Financial Insights & Analytics**: Visual charts showing Net Savings, Savings Rate %, Income vs Expense ratios, and top spending categories.
- 💱 **Live Currency Converter**: Real-time European Central Bank foreign exchange rates via public Frankfurter API with full offline caching.
- 📳 **Haptic Feedback & Themes**: Tactile vibration feedback on interactions with System, Light, and Dark mode options.
- 🔒 **100% Offline & Private**: All data is stored locally in SQLite on your device. No account registration or external tracking required.

---

## 🏗️ Architecture & Tech Stack

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3) + Navigation Compose
- **Local Persistence**: Room SQLite (DAOs, Entities, Relational Queries)
- **Networking**: Retrofit 2 + OkHttp + Gson Converter
- **Background Sync**: AndroidX WorkManager (Periodic 12-hour exchange rate sync)
- **Architecture**: MVVM + Repository Pattern + Kotlin Coroutines (`StateFlow`, `combine`)
- **Performance**: ART Profile Installer + Ahead-of-Time (AOT) baseline profile compilation

### Project Structure
```
com.example.expensetracker/
├── data/
│   ├── local/
│   │   ├── dao/             # CategoryDao, TransactionDao, BudgetDao, ExchangeRateDao
│   │   ├── entity/          # CategoryEntity, TransactionEntity, BudgetEntity, ExchangeRateEntity
│   │   ├── model/           # TransactionWithCategory
│   │   ├── AppDatabase.kt   # Room Database setup + default categories seeding
│   │   └── UserPreferencesRepository.kt
│   ├── remote/
│   │   ├── api/             # ExchangeRateApiService (Frankfurter API)
│   │   ├── dto/             # ExchangeRateResponseDto
│   │   └── RetrofitClient.kt
│   └── repository/          # TransactionRepository, CategoryRepository, BudgetRepository, ExchangeRateRepository
├── ui/
│   ├── budget/              # BudgetsScreen & BudgetViewModel
│   ├── components/          # FloatingDockBar, RollingOdometerText, AnimatedScrollEntry, etc.
│   ├── dashboard/           # DashboardScreen & DashboardViewModel
│   ├── navigation/          # Screen definitions & routes
│   ├── reports/             # ReportsScreen & ReportsViewModel
│   ├── settings/            # SettingsScreen & SettingsViewModel
│   ├── theme/               # Material 3 Color, Type, Theme definitions
│   ├── transactions/        # TransactionsScreen, AddEditTransactionScreen & ViewModels
│   └── MainScreen.kt        # Top-level scaffold, Fluid FadeThrough navigation, Hoisted ViewModels
├── utils/                   # ValidationUtils, SplitReminderUtils, HapticHelper
├── workers/                 # ExchangeRateSyncWorker
├── ExpenseTrackerApp.kt     # Application subclass
└── MainActivity.kt          # Single Activity entry point
```

---

## 🛠️ Building From Source

If you are a developer and want to run or modify the code locally:

1. **Clone the repository**:
   ```bash
   git clone https://github.com/RudraMangroliya/ExpenseTracker.git
   ```
2. **Open in Android Studio**:
   - Open Android Studio (Ladybug / Jellyfish / 2024.1+).
   - Let Gradle sync project dependencies.
3. **Build Debug APK**:
   ```powershell
   ./gradlew assembleDebug
   ```
4. **Run Unit Tests**:
   ```powershell
   ./gradlew testDebugUnitTest
   ```

---

## 📄 License
This project is open-source and available under the **MIT License**.
