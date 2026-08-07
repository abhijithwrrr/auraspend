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
        SmsMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun smsMessageDao(): SmsMessageDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auraspend_db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
