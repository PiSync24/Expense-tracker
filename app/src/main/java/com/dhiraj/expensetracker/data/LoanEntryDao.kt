package com.dhiraj.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanEntryDao {

    @Insert
    suspend fun insert(loan: LoanEntry)

    @Update
    suspend fun update(loan: LoanEntry)

    @Delete
    suspend fun delete(loan: LoanEntry)

    @Query(
        """
        SELECT * FROM loan_entries
        ORDER BY dateGiven DESC
        """
    )
    fun getAllLoans(): Flow<List<LoanEntry>>
}
