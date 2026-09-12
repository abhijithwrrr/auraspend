package com.awbuilds.auraspend.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.awbuilds.auraspend.data.local.dao.*
import com.awbuilds.auraspend.data.local.entities.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        SubscriptionEntity::class,
        SavingsGoalEntity::class,
        SmsMessageEntity::class,
        ClassificationMemoryEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun classificationMemoryDao(): ClassificationMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v3 -> v4: add the unique source SMS id column used to prevent a message being saved
         * twice (by both Smart Add and the background auto-detect worker). Existing rows are all
         * NULL for the new column, and SQLite treats NULLs as distinct in unique indexes, so a
         * freshly created index cannot collide with legacy data.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceSmsId TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_sourceSmsId " +
                        "ON transactions(sourceSmsId)"
                )
            }
        }

        /**
         * v4 -> v5: add the persistent SMS classification queue. Each bank message is stored once
         * (keyed on the provider's `_id`) and processed one-by-one in the background; ingestion is
         * idempotent so this table can be safely back-filled from the inbox.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sms_messages` (
                        `id` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `receivedAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `amount` REAL,
                        `type` TEXT,
                        `merchant` TEXT,
                        `categoryId` TEXT,
                        `isSubscription` INTEGER NOT NULL,
                        `confidence` REAL NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v5 -> v6: add the learned classification memory. Maps normalized merchant / note keys to
         * the category the user (or a confirmed auto-save) last used, so repeat transactions are
         * categorized instantly without an LLM call. Starts empty; safe to create on upgrade.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `classification_memory` (
                        `rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `memoryKey` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `type` TEXT,
                        `source` TEXT NOT NULL,
                        `hits` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_classification_memory_memoryKey " +
                        "ON classification_memory(memoryKey)"
                )
            }
        }

        /**
         * v6 -> v7: add query indices. Dashboard, Activity and Insights filter
         * transactions by date/category/type on every screen; without these the
         * table is scanned fully once data grows.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_dateTimestamp " +
                        "ON transactions(dateTimestamp)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_categoryId " +
                        "ON transactions(categoryId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_type_dateTimestamp " +
                        "ON transactions(type, dateTimestamp)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_budgets_categoryId " +
                        "ON budgets(categoryId)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auraspend_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(false)
                    // Write-ahead logging lets dashboard reads proceed while the
                    // SMS pipeline writes transactions — no lock contention.
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
