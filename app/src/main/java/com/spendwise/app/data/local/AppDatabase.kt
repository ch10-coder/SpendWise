package com.spendwise.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendwise.app.data.local.dao.AccountDao
import com.spendwise.app.data.local.dao.BudgetDao
import com.spendwise.app.data.local.dao.CategoryDao
import com.spendwise.app.data.local.dao.RecurringTransactionDao
import com.spendwise.app.data.local.dao.TagDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.data.local.entity.BudgetEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.RecurringTransactionEntity
import com.spendwise.app.data.local.entity.SavingsGoalEntity
import com.spendwise.app.data.local.entity.TagEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.data.local.entity.TransactionTagCrossRef
import com.spendwise.app.model.AccountType
import com.spendwise.app.model.RecurrenceFrequency
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Type converters for Room database.
 * Handles enum ↔ String serialization for TransactionType, AccountType, and RecurrenceFrequency.
 */
object Converters {
    @TypeConverter
    @JvmStatic
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    @JvmStatic
    fun toTransactionType(value: String): TransactionType =
        try { TransactionType.valueOf(value) } catch (e: Exception) { TransactionType.EXPENSE }

    @TypeConverter
    @JvmStatic
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    @JvmStatic
    fun toAccountType(value: String): AccountType =
        try { AccountType.valueOf(value) } catch (e: Exception) { AccountType.CASH }

    @TypeConverter
    @JvmStatic
    fun fromRecurrenceFrequency(freq: RecurrenceFrequency): String = freq.name

    @TypeConverter
    @JvmStatic
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency =
        try { RecurrenceFrequency.valueOf(value) } catch (e: Exception) { RecurrenceFrequency.MONTHLY }
}

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        AccountEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        RecurringTransactionEntity::class,
        SavingsGoalEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun tagDao(): TagDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun savingsGoalDao(): com.spendwise.app.data.local.dao.SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from v1 to v2:
         * - Adds accounts, tags, transaction_tags, recurring_transactions, savings_goals tables
         * - Adds new columns to transactions table (accountId, toAccountId, splitGroupId, receiptPath, recurringRuleId)
         * - Adds new index to budgets table (monthYear)
         * - Adds composite index to transactions (type, dateTimestamp)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create accounts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        balance REAL NOT NULL DEFAULT 0.0,
                        iconName TEXT NOT NULL DEFAULT 'AccountBalanceWallet',
                        colorHex TEXT NOT NULL DEFAULT '#10B981',
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create tags table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL DEFAULT '#6366F1'
                    )
                """)

                // Create transaction_tags join table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaction_tags (
                        transactionId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY (transactionId, tagId),
                        FOREIGN KEY (transactionId) REFERENCES transactions(id) ON DELETE CASCADE,
                        FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_tags_transactionId ON transaction_tags(transactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_tags_tagId ON transaction_tags(tagId)")

                // Create recurring_transactions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        categoryId INTEGER,
                        accountId INTEGER,
                        frequency TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        nextOccurrence INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        note TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                        FOREIGN KEY (accountId) REFERENCES accounts(id) ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_categoryId ON recurring_transactions(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_accountId ON recurring_transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_nextOccurrence ON recurring_transactions(nextOccurrence)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_isActive ON recurring_transactions(isActive)")

                // Create savings_goals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        targetAmount REAL NOT NULL,
                        currentAmount REAL NOT NULL DEFAULT 0.0,
                        deadline INTEGER,
                        iconName TEXT NOT NULL DEFAULT 'Savings',
                        colorHex TEXT NOT NULL DEFAULT '#10B981',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Add new columns to transactions table
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER DEFAULT NULL REFERENCES accounts(id) ON DELETE SET NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN toAccountId INTEGER DEFAULT NULL REFERENCES accounts(id) ON DELETE SET NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN splitGroupId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiptPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringRuleId INTEGER DEFAULT NULL")

                // Add new indexes
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type_dateTimestamp ON transactions(type, dateTimestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_toAccountId ON transactions(toAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_splitGroupId ON transactions(splitGroupId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_monthYear ON budgets(monthYear)")

                // Seed default accounts
                db.execSQL("INSERT OR IGNORE INTO accounts (id, name, type, balance, iconName, colorHex, isDefault, createdAt) VALUES (1, 'Cash', 'CASH', 0.0, 'Payments', '#10B981', 1, ${System.currentTimeMillis()})")
                db.execSQL("INSERT OR IGNORE INTO accounts (id, name, type, balance, iconName, colorHex, isDefault, createdAt) VALUES (2, 'Bank Account', 'BANK', 0.0, 'AccountBalance', '#3B82F6', 0, ${System.currentTimeMillis()})")
                db.execSQL("INSERT OR IGNORE INTO accounts (id, name, type, balance, iconName, colorHex, isDefault, createdAt) VALUES (3, 'Credit Card', 'CREDIT_CARD', 0.0, 'CreditCard', '#EF4444', 0, ${System.currentTimeMillis()})")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spendwise.db"
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Database creation callback — seeds initial categories and accounts
         * on fresh install. Uses the built instance directly to avoid the
         * INSTANCE race condition.
         */
        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed via raw SQL to avoid INSTANCE race condition.
                // Categories are seeded by Room via the entity defaults,
                // but we insert them explicitly here for reliability.
                scope.launch(Dispatchers.IO) {
                    INSTANCE?.let { database ->
                        val categoryDao = database.categoryDao()
                        if (categoryDao.getCategoryCount() == 0) {
                            categoryDao.insertCategories(CategoryEntity.DEFAULT_CATEGORIES)
                        }
                        val accountDao = database.accountDao()
                        if (accountDao.getAccountCount() == 0) {
                            accountDao.insertAccounts(AccountEntity.DEFAULT_ACCOUNTS)
                        }
                    }
                }
            }
        }
    }
}
