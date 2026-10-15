<div align="center">

# 💰 MR Tracker
### Modern Expense, Budget & Split Manager for Android

<p align="center">
  <a href="https://github.com/RudraMangroliya/ExpenseTracker/releases/download/v1.0.1/MR-Tracker-v1.0.1.apk">
    <img src="https://img.shields.io/badge/⚡%20Download%20APK-v1.0.1%20(19.8%20MB)-00C853?style=for-the-badge&logo=android&logoColor=white&labelColor=1b5e20" alt="Download APK" height="42">
  </a>
  &nbsp;&nbsp;
  <a href="https://github.com/RudraMangroliya/ExpenseTracker/releases/latest">
    <img src="https://img.shields.io/badge/🏷️%20Latest%20Release-v1.0.1-2979FF?style=for-the-badge&logo=github&logoColor=white&labelColor=0d47a1" alt="Latest Release" height="42">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose">
  <img src="https://img.shields.io/badge/Storage-100%25%20Offline%20SQLite-009688?style=flat-square&logo=sqlite&logoColor=white" alt="Offline SQLite">
  <img src="https://img.shields.io/badge/License-MIT-F57C00?style=flat-square" alt="License">
</p>

A production-grade, privacy-first personal finance application for Android built with **Jetpack Compose (Material 3)**, **Room (SQLite)**, **Retrofit**, and modern **MVVM + Clean Architecture**.

</div>

---

## 📲 Download & Install the App

Anyone can install and run **MR Tracker** directly on their Android phone without needing Android Studio or developer tools:

<table>
  <tr>
    <td align="center" width="60%">
      <h3>📥 Direct APK Download</h3>
      <p>Tap below to download the latest production APK directly to your phone:</p>
      <p>
        <a href="https://github.com/RudraMangroliya/ExpenseTracker/releases/download/v1.0.1/MR-Tracker-v1.0.1.apk">
          <img src="https://img.shields.io/badge/⬇️%20DOWNLOAD%20APK-v1.0.1%20STABLE-2ea44f?style=for-the-badge&logo=android&logoColor=white&labelColor=1b5e20" alt="Download APK Button" height="46">
        </a>
      </p>
      <p>
        <a href="https://github.com/RudraMangroliya/ExpenseTracker/releases">
          <img src="https://img.shields.io/badge/🏷️%20View%20All%20Releases%20&%20Changelog-0969da?style=for-the-badge&logo=github&logoColor=white&labelColor=0550ae" alt="All Releases" height="36">
        </a>
      </p>
      <br>
      <sub>📦 <b>File:</b> <code>MR-Tracker-v1.0.1.apk</code> &bull; ⚖️ <b>Size:</b> <code>19.8 MB</code> &bull; 📱 <b>Min Android:</b> <code>7.0 (API 24+)</code></sub>
    </td>
    <td align="center" width="40%">
      <h3>📱 Scan to Download</h3>
      <img src="https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=https%3A%2F%2Fgithub.com%2FRudraMangroliya%2FExpenseTracker%2Freleases%2Fdownload%2Fv1.0.1%2FMR-Tracker-v1.0.1.apk" alt="Download QR Code" width="140" height="140">
      <br>
      <sub>Point your phone camera here to download directly to your mobile device</sub>
    </td>
  </tr>
</table>

### 📋 Installation Guide (Step-by-Step):
1. **Download**: Tap the [📥 **Download MR-Tracker-v1.0.1.apk**](https://github.com/RudraMangroliya/ExpenseTracker/releases/download/v1.0.1/MR-Tracker-v1.0.1.apk) button or scan the QR code above.
2. **Open**: Tap the download completed notification, or open your phone's **Files / Downloads** app and tap `MR-Tracker-v1.0.1.apk`.
3. **Allow Installation**:
   - If prompted: *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings**.
   - Turn ON **"Allow from this source"**.
   - Go back and tap **Install**.
4. **Done!** Launch **MR Tracker** and enjoy 100% offline, privacy-first expense and budget tracking!

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
