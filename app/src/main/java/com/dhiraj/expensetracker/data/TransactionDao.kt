package com.dhiraj.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(tx: Transaction)

    @Update
    suspend fun updateTransaction(tx: Transaction)

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM transactions
            WHERE upiId = :upiRef
        )
        """
    )
    suspend fun existsByUpiRef(upiRef: String): Boolean

    @Query(
        """
        SELECT * FROM transactions
        ORDER BY date DESC
        """
    )
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query(
        """
        SELECT IFNULL(SUM(ABS(amount)), 0)
        FROM transactions
        """
    )
    fun getTotalSpentFlow(): Flow<Double>

    @Query(
        """
        SELECT category AS category, COUNT(*) AS count
        FROM transactions
        WHERE merchantName IS NOT NULL
          AND LOWER(merchantName) = LOWER(:merchant)
        GROUP BY category
        ORDER BY count DESC, MAX(date) DESC
        LIMIT :limit
        """
    )
    suspend fun getCategoryFrequencyForMerchant(
        merchant: String,
        limit: Int = 4
    ): List<CategoryCount>

    @Query(
        """
        SELECT category AS category, COUNT(*) AS count
        FROM transactions
        GROUP BY category
        ORDER BY count DESC
        LIMIT :limit
        """
    )
    suspend fun getTopCategoryFrequency(limit: Int = 4): List<CategoryCount>

    @Query(
        """
        SELECT * FROM transactions
        WHERE merchantName IS NOT NULL
        ORDER BY date DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentTransactions(limit: Int = 250): List<Transaction>

    @Query(
        """
        SELECT * FROM transactions
        WHERE notes LIKE '%' || :tag || '%'
        ORDER BY date DESC
        LIMIT 1
        """
    )
    suspend fun findLatestByNoteTag(tag: String): Transaction?

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}
