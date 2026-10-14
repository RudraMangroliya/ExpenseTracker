# Expense & Budget Tracker — Android App

An Android Expense and Budget Tracker application built using **Kotlin**, **Jetpack Compose (Material 3)**, **Room (SQLite)**, **Retrofit**, and modern **MVVM + Repository** architecture.

---

## Key Features

- **Dashboard**: Complete financial overview displaying Total Balance, Total Income, Total Expense, Current Month Spending, Remaining Monthly Budget meter, Recent Transactions, and Category-wise Spending Breakdown.
- **Transactions Management (CRUD)**: Create, Read, Update, and Delete income/expense transactions with instant recalculation of all financial metrics across the app.
- **Search & Filter**: Search transactions by note, category, payment method, or amount. Filter by transaction type (Income/Expense) or category.
- **Form Validation**: Built-in validation rules for required fields, positive numeric amounts, date formatting, and max note lengths. Prevents double submission during save operations.
- **Budgeting System**: Set monthly overall and category-specific budget targets. Visual progress indicators with color status alerts:
  - 🟢 **Green**: Under 80% used
  - 🟡 **Yellow**: 80% – 100% used (Caution)
  - 🔴 **Red**: Over 100% used (Budget Exceeded Warning)
- **Reports & Financial Insights**: Monthly financial overview showing Income vs. Expense ratio, Net Savings, Savings Rate %, Top 3 Spending Categories, and full category distribution.
- **Multi-Currency & Exchange Rate API Integration**: Integrates with the public **Frankfurter API** (`https://api.frankfurter.app/`) to allow transactions in multiple currencies (USD, EUR, GBP, JPY, CAD, AUD, INR) converted to the user's base currency.
- **Offline Caching**: All exchange rates are cached in local SQLite tables with fetched timestamps. The app remains functional offline using cached or fallback static exchange rates.
- **Settings & Data Tools**: Change base currency, switch theme (System Default, Light, Dark), toggle budget notifications, manual exchange rate sync, seed realistic sample demo data, and full database reset options.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3) + Navigation Compose
- **Database**: SQLite via Room (Runtime, KTX, KSP/Compiler)
- **Networking**: Retrofit 2 + Gson Converter + OkHttp 4
- **Architecture**: MVVM + Repository pattern with Coroutines & `StateFlow`

### Package Structure
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
│   │   ├── api/             # ExchangeRateApiService
│   │   ├── dto/             # ExchangeRateResponseDto
│   │   └── RetrofitClient.kt
│   └── repository/          # TransactionRepository, CategoryRepository, BudgetRepository, ExchangeRateRepository, CurrencyConverter
├── ui/
│   ├── budget/              # BudgetScreen & BudgetViewModel
│   ├── dashboard/           # DashboardScreen & DashboardViewModel
│   ├── navigation/          # Screen definitions & Bottom Navigation routes
│   ├── reports/             # ReportsScreen & ReportsViewModel
│   ├── settings/            # SettingsScreen & SettingsViewModel
│   ├── theme/               # Material 3 Color, Type, Theme definitions
│   ├── transactions/        # TransactionsScreen, AddEditTransactionScreen & ViewModels
│   └── MainScreen.kt        # Scaffold, Navigation host, BottomBar, FAB
├── utils/                   # ValidationUtils, CategoryIconMapper
├── ExpenseTrackerApp.kt     # Application subclass
└── MainActivity.kt          # Single Activity entry point
```

---

## SQLite Database Schema

The app uses a normalized SQLite schema managed by Room:

| Table | Primary Key | Key Fields | Foreign Keys / Index | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `categories` | `id` (Long) | `name`, `type`, `icon`, `isDefault`, `createdAt` | None | Stores income & expense categories |
| `transactions` | `id` (Long) | `type`, `amount`, `currency`, `convertedAmount`, `categoryId`, `date`, `note`, `paymentMethod`, `createdAt`, `updatedAt` | `categoryId` → `categories.id` | Stores financial income and expense records |
| `budgets` | `id` (Long) | `categoryId` (nullable), `amount`, `currency`, `month`, `year`, `createdAt`, `updatedAt` | `categoryId` → `categories.id` | Stores monthly overall or category budget targets |
| `exchange_rates` | `id` (Long) | `baseCurrency`, `targetCurrency`, `rate`, `fetchedAt` | None | Caches API exchange rates for offline conversion |

---

## API Integration & Offline Fallback Strategy

1. **Live Network Request**: When adding/editing transactions in foreign currencies or changing base currency, the app queries `https://api.frankfurter.app/latest?from={baseCurrency}`.
2. **Database Cache**: Fetched rate pairs are inserted/updated into the `exchange_rates` SQLite table.
3. **Offline Fallback**: If network is unavailable or offline, `ExchangeRateRepository` queries the Room database for cached rates. If no cached rates exist, a static relative fallback conversion table is used so transaction creation is never blocked.

---

## Testing

### Automated Unit Tests
Includes tests for form validations, amount calculations, currency conversion logic, and budget math:
```bash
./gradlew testDebugUnitTest
```

### Instrumentation / Database Tests
Includes in-memory Room database tests verifying entity creation, relational queries (`TransactionWithCategory`), updates, deletions, and budget queries:
```bash
./gradlew connectedAndroidTest
```

---

## Building and Running

1. Open project in **Android Studio** (2024.1+ / Ladybug / Jellyfish).
2. Sync Gradle files (`Gradle Sync`).
3. Build the APK:
```bash
./gradlew app:assembleDebug
```
4. Run on an Android device or emulator running **Android 7.0 (API level 24)** or higher.
