package com.dhiraj.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_entries")
data class LoanEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val friendName: String,
    val amount: Double,
    val dateGiven: Long = System.currentTimeMillis(),
    val status: String = "OPEN",
    val settledAt: Long? = null,
    val settledBy: String? = null,
    val notes: String? = null
)
