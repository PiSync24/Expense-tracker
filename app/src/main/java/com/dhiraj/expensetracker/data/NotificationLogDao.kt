package com.dhiraj.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {

    @Insert
    suspend fun insert(log: NotificationLog)

    @Query(
        """
        SELECT * FROM notification_logs
        ORDER BY timestamp DESC
        """
    )
    fun getAllLogs(): Flow<List<NotificationLog>>

    @Query(
        """
        UPDATE notification_logs
        SET status = :status
        WHERE notificationId = :notificationId
        """
    )
    suspend fun updateStatus(
        notificationId: Int,
        status: String
    )

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM notification_logs
            WHERE upiReference = :upiRef
            AND status = 'PENDING'
        )
        """
    )
    suspend fun existsPendingByUpi(upiRef: String): Boolean

    @Query(
        """
        SELECT COUNT(*) FROM notification_logs
        WHERE status = 'PENDING'
        """
    )
    fun getPendingCountFlow(): Flow<Int>
}
