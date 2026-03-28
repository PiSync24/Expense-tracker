package com.dhiraj.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores ALL incoming transaction notifications
 * (SMS + NotificationListener)
 *
 * Used for:
 * - Deduplication
 * - Pending → Logged flow
 */
@Entity(tableName = "notification_logs")
data class NotificationLog(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Android notification ID (used to cancel notification) */
    val notificationId: Int,

    /** Merchant name parsed from SMS / notification */
    val merchant: String,

    /** Transaction amount */
    val amount: Double,

    /** Epoch millis when detected */
    val timestamp: Long,

    /** PENDING | LOGGED */
    val status: String,

    /** SMS | NOTIFICATION */
    val source: String,

    /** UPI reference (nullable, used ONLY for dedupe) */
    val upiReference: String?
)

