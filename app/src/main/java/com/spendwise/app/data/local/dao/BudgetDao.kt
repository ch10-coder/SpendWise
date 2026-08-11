package com.spendwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.app.data.local.entity.BudgetEntity
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class BudgetWithCategoryTuple(
    val id: Long,
    val categoryId: Long,
    val limitAmount: Double,
    val monthYear: String,
    val cat_id: Long,
    val cat_name: String,
    val cat_type: TransactionType,
    val cat_iconName: String,
    val cat_colorHex: String,
    val cat_isDefault: Boolean
) {
    fun toCategory(): CategoryEntity {
        return CategoryEntity(
            id = cat_id,
            name = cat_name,
            type = cat_type,
            iconName = cat_iconName,
            colorHex = cat_colorHex,
            isDefault = cat_isDefault
        )
    }
}

@Dao
interface BudgetDao {

    @Query("""
        SELECT b.*, c.id AS cat_id, c.name AS cat_name, c.type AS cat_type,
               c.iconName AS cat_iconName, c.colorHex AS cat_colorHex, c.isDefault AS cat_isDefault
        FROM budgets b
        INNER JOIN categories c ON b.categoryId = c.id
        WHERE b.monthYear = :monthYear
        ORDER BY b.limitAmount DESC
    """)
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetWithCategoryTuple>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetForCategoryAndMonth(categoryId: Long, monthYear: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)
}
