package com.dhiraj.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * AppDatabase
 *
 * This is the single source of truth for persistent data.
 */
@Database(
    entities = [
        Transaction::class,
        NotificationLog::class,
        CategoryEntity::class,
        FinancialPlanEntry::class,
        LoanEntry::class,
        PlanTemplate::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun financialPlanDao(): FinancialPlanDao
    abstract fun loanEntryDao(): LoanEntryDao
    abstract fun planTemplateDao(): PlanTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS financial_plan_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        bucket TEXT NOT NULL,
                        amount REAL NOT NULL,
                        recurrence TEXT NOT NULL,
                        weeklyDay INTEGER,
                        isCompleted INTEGER NOT NULL,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS loan_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        friendName TEXT NOT NULL,
                        amount REAL NOT NULL,
                        dateGiven INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        settledAt INTEGER,
                        settledBy TEXT,
                        notes TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS plan_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT INTO plan_templates(id, name, createdAt)
                    SELECT 1, 'Default Monthly', $now
                    WHERE NOT EXISTS (SELECT 1 FROM plan_templates)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE financial_plan_entries
                    ADD COLUMN templateId INTEGER NOT NULL DEFAULT 1
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_financial_plan_entries_templateId
                    ON financial_plan_entries(templateId)
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "expense_tracker_db"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .addCallback(prepopulateDataCallback())
                        .build()

                INSTANCE = instance
                instance
            }
        }

        private fun prepopulateDataCallback() =
            object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)

                    val now = System.currentTimeMillis()
                    db.execSQL(
                        "INSERT INTO categories(name, emoji, isCustom, createdAt) VALUES(?, ?, ?, ?)",
                        arrayOf("Food", "🍔", 0, now)
                    )
                    db.execSQL(
                        "INSERT INTO categories(name, emoji, isCustom, createdAt) VALUES(?, ?, ?, ?)",
                        arrayOf("Transport", "🚕", 0, now)
                    )
                    db.execSQL(
                        "INSERT INTO categories(name, emoji, isCustom, createdAt) VALUES(?, ?, ?, ?)",
                        arrayOf("Shopping", "🛍️", 0, now)
                    )
                    db.execSQL(
                        "INSERT INTO categories(name, emoji, isCustom, createdAt) VALUES(?, ?, ?, ?)",
                        arrayOf("Bills", "💡", 0, now)
                    )
                    db.execSQL(
                        "INSERT INTO categories(name, emoji, isCustom, createdAt) VALUES(?, ?, ?, ?)",
                        arrayOf("Entertainment", "🎬", 0, now)
                    )
                    db.execSQL(
                        "INSERT INTO categories(name, emoji, isCustom, createdAt) VALUES(?, ?, ?, ?)",
                        arrayOf("Health", "💊", 0, now)
                    )

                    db.execSQL(
                        "INSERT INTO plan_templates(id, name, createdAt) VALUES(?, ?, ?)",
                        arrayOf(1, "Default Monthly", now)
                    )
                }
            }
    }
}
