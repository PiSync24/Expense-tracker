package com.dhiraj.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanTemplateDao {

    @Insert
    suspend fun insert(template: PlanTemplate): Long

    @Update
    suspend fun update(template: PlanTemplate)

    @Delete
    suspend fun delete(template: PlanTemplate)

    @Query(
        """
        SELECT * FROM plan_templates
        ORDER BY createdAt ASC
        """
    )
    fun getAllTemplates(): Flow<List<PlanTemplate>>

    @Query(
        """
        SELECT COUNT(*) FROM plan_templates
        WHERE name = :name COLLATE NOCASE
        """
    )
    suspend fun exists(name: String): Int
}
