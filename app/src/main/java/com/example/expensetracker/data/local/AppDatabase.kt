package com.example.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.data.local.dao.BudgetDao
import com.example.expensetracker.data.local.dao.CategoryDao
import com.example.expensetracker.data.local.dao.ExchangeRateDao
import com.example.expensetracker.data.local.dao.TransactionDao
import com.example.expensetracker.data.local.dao.TransactionSplitDao
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.CategoryEntity
import com.example.expensetracker.data.local.entity.ExchangeRateEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.data.local.entity.TransactionSplitEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        ExchangeRateEntity::class,
        TransactionSplitEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun transactionSplitDao(): TransactionSplitDao

    fun vacuumAndCheckpoint() {
        try {
            val db = openHelper.writableDatabase
            db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            db.execSQL("VACUUM")
        } catch (_: Exception) {}
    }

    fun checkpointWal() {
        try {
            val db = openHelper.writableDatabase
            db.execSQL("PRAGMA wal_checkpoint(FULL)")
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exchange_rates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `baseCurrency` TEXT NOT NULL,
                        `targetCurrency` TEXT NOT NULL,
                        `rate` REAL NOT NULL,
                        `fetchedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_exchange_rates_baseCurrency_targetCurrency` ON `exchange_rates` (`baseCurrency`, `targetCurrency`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add split metadata fields to transactions table
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isSplit` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `originalTotalAmount` REAL NOT NULL DEFAULT 0.0")

                // Create transaction_splits table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_splits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `isCurrentUser` INTEGER NOT NULL,
                        `isPaid` INTEGER NOT NULL,
                        `paidDate` INTEGER,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_splits_transactionId` ON `transaction_splits` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_splits_phoneNumber` ON `transaction_splits` (`phoneNumber`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            insertDefaultCategories(db)
                        }

                        private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
                            try {
                                db.beginTransaction()
                                try {
                                    DEFAULT_CATEGORIES.forEach { cat ->
                                        db.execSQL(
                                            "INSERT OR IGNORE INTO categories (id, name, type, icon, isDefault, createdAt) VALUES (?, ?, ?, ?, ?, ?)",
                                            arrayOf(cat.id, cat.name, cat.type, cat.icon, if (cat.isDefault) 1 else 0, cat.createdAt)
                                        )
                                    }
                                    db.setTransactionSuccessful()
                                } finally {
                                    db.endTransaction()
                                }
                            } catch (_: Exception) {}
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            // Expense Categories
            CategoryEntity(id = 1, name = "Food & Dining", type = "EXPENSE", icon = "fastfood", isDefault = true),
            CategoryEntity(id = 2, name = "Transportation", type = "EXPENSE", icon = "directions_car", isDefault = true),
            CategoryEntity(id = 3, name = "Housing & Rent", type = "EXPENSE", icon = "home", isDefault = true),
            CategoryEntity(id = 4, name = "Utilities & Bills", type = "EXPENSE", icon = "receipt_long", isDefault = true),
            CategoryEntity(id = 5, name = "Entertainment", type = "EXPENSE", icon = "movie", isDefault = true),
            CategoryEntity(id = 6, name = "Shopping", type = "EXPENSE", icon = "shopping_bag", isDefault = true),
            CategoryEntity(id = 7, name = "Healthcare", type = "EXPENSE", icon = "medical_services", isDefault = true),
            CategoryEntity(id = 8, name = "Education", type = "EXPENSE", icon = "school", isDefault = true),
            CategoryEntity(id = 9, name = "Travel", type = "EXPENSE", icon = "flight", isDefault = true),
            CategoryEntity(id = 10, name = "Personal Care", type = "EXPENSE", icon = "spa", isDefault = true),
            CategoryEntity(id = 11, name = "Other Expense", type = "EXPENSE", icon = "more_horiz", isDefault = true),

            // Income Categories
            CategoryEntity(id = 12, name = "Salary", type = "INCOME", icon = "payments", isDefault = true),
            CategoryEntity(id = 13, name = "Freelance / Business", type = "INCOME", icon = "work", isDefault = true),
            CategoryEntity(id = 14, name = "Investments", type = "INCOME", icon = "trending_up", isDefault = true),
            CategoryEntity(id = 15, name = "Gifts & Allowance", type = "INCOME", icon = "card_giftcard", isDefault = true),
            CategoryEntity(id = 16, name = "Other Income", type = "INCOME", icon = "savings", isDefault = true)
        )
    }
}
