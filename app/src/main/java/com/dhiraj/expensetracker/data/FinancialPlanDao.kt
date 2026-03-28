package com.dhiraj.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialPlanDao {

    @Insert
    suspend fun insert(entry: FinancialPlanEntry): Long

    @Update
    suspend fun update(entry: FinancialPlanEntry)

    @Delete
    suspend fun delete(entry: FinancialPlanEntry)

    @Query(
        """
        SELECT * FROM financial_plan_entries
        WHERE templateId = :templateId
        ORDER BY bucket ASC, createdAt ASC
        """
    )
    fun getEntriesForTemplate(templateId: Long): Flow<List<FinancialPlanEntry>>

    @Query(
        """
        SELECT * FROM financial_plan_entries
        WHERE templateId = :templateId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getEntriesForTemplateOnce(templateId: Long): List<FinancialPlanEntry>
}
