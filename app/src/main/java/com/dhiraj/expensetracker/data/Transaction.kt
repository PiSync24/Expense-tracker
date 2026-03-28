package com.dhiraj.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.Date
import com.dhiraj.expensetracker.data.Category

/**
 * Transaction Data Model
 * 
 * This represents a single expense/transaction in your app.
 * Think of it as a "blueprint" for what information each transaction holds.
 * 
 * @Entity - Tells Room (Android's database library) this is a database table
 * Each Transaction object will be stored as a row in the database
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double,
    val merchantName: String?,
    val category: String,
    val date: Date,
    val bankName: String?,
    val upiId: String?,
    val rawSmsText: String?,
    val isManualEntry: Boolean = false,
    val notes: String? = null
)

/**
 * Category Enum
 * 
 * Predefined categories for quick selection in notifications
 * You can add more categories here as needed
 */

