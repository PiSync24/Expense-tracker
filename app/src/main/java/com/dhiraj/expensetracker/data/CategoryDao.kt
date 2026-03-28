package com.dhiraj.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query(
        """
        SELECT * FROM categories
        ORDER BY isCustom DESC, name ASC
        """
    )
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM categories
        WHERE name = :name COLLATE NOCASE
        """
    )
    suspend fun exists(name: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM categories
        WHERE id != :categoryId
          AND name = :name COLLATE NOCASE
        """
    )
    suspend fun existsForOtherId(name: String, categoryId: Long): Int
}
