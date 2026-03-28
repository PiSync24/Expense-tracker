package com.dhiraj.expensetracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_plan_entries",
    indices = [Index(value = ["templateId"])]
)
data class FinancialPlanEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long = 1,
    val title: String,
    val category: String,
    val bucket: String,
    val amount: Double,
    val recurrence: String = "MONTHLY",
    val weeklyDay: Int? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
