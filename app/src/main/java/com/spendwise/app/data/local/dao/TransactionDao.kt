package com.spendwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.app.data.local.entity.CategoryEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.model.TransactionType
import com.spendwise.app.model.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

data class CategorySumResult(
    val categoryId: Long,
    val categoryName: String,
    val categoryType: TransactionType,
    val iconName: String,
    val colorHex: String,
    val totalAmount: Double
)

@Dao
interface TransactionDao {

    @Query("""
        SELECT t.*, c.id AS cat_id, c.name AS cat_name, c.type AS cat_type, 
               c.iconName AS cat_iconName, c.colorHex AS cat_colorHex, c.isDefault AS cat_isDefault
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        ORDER BY t.dateTimestamp DESC
    """)
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategoryTuple>>

    @Query("""
        SELECT t.*, c.id AS cat_id, c.name AS cat_name, c.type AS cat_type, 
               c.iconName AS cat_iconName, c.colorHex AS cat_colorHex, c.isDefault AS cat_isDefault
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.dateTimestamp BETWEEN :startTimestamp AND :endTimestamp
        ORDER BY t.dateTimestamp DESC
    """)
    fun getTransactionsBetweenDates(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithCategoryTuple>>

    @Query("""
        SELECT t.*, c.id AS cat_id, c.name AS cat_name, c.type AS cat_type, 
               c.iconName AS cat_iconName, c.colorHex AS cat_colorHex, c.isDefault AS cat_isDefault
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.accountId = :accountId
        ORDER BY t.dateTimestamp DESC
    """)
    fun getTransactionsForAccount(accountId: Long): Flow<List<TransactionWithCategoryTuple>>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = :type AND dateTimestamp BETWEEN :startTimestamp AND :endTimestamp
    """)
    fun getTotalAmountByTypeBetweenDates(type: TransactionType, startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE type = :type
    """)
    fun getTotalAmountByType(type: TransactionType): Flow<Double?>

    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, c.type AS categoryType,
               c.iconName AS iconName, c.colorHex AS colorHex, SUM(t.amount) AS totalAmount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.type = :type AND t.dateTimestamp BETWEEN :startTimestamp AND :endTimestamp
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategorySpendingBetweenDates(type: TransactionType, startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySumResult>>

    @Query("""
        SELECT SUM(amount) FROM transactions
        WHERE categoryId = :categoryId AND dateTimestamp BETWEEN :startTimestamp AND :endTimestamp
    """)
    fun getSpendingForCategoryBetweenDates(categoryId: Long, startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY dateTimestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    /** Get transactions belonging to a split group */
    @Query("SELECT * FROM transactions WHERE splitGroupId = :groupId ORDER BY dateTimestamp ASC")
    fun getTransactionsBySplitGroup(groupId: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    /** Delete all transactions in a split group */
    @Query("DELETE FROM transactions WHERE splitGroupId = :groupId")
    suspend fun deleteTransactionsBySplitGroup(groupId: Long)
}

data class TransactionWithCategoryTuple(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long?,
    val dateTimestamp: Long,
    val note: String?,
    val accountId: Long?,
    val toAccountId: Long?,
    val splitGroupId: Long?,
    val receiptPath: String?,
    val recurringRuleId: Long?,
    val cat_id: Long?,
    val cat_name: String?,
    val cat_type: TransactionType?,
    val cat_iconName: String?,
    val cat_colorHex: String?,
    val cat_isDefault: Boolean?
) {
    fun toTransactionWithCategory(): TransactionWithCategory {
        val category = if (cat_id != null && cat_name != null && cat_type != null && cat_iconName != null && cat_colorHex != null) {
            CategoryEntity(
                id = cat_id,
                name = cat_name,
                type = cat_type,
                iconName = cat_iconName,
                colorHex = cat_colorHex,
                isDefault = cat_isDefault ?: false
            )
        } else null

        val transaction = TransactionEntity(
            id = id,
            title = title,
            amount = amount,
            type = type,
            categoryId = categoryId,
            dateTimestamp = dateTimestamp,
            note = note,
            accountId = accountId,
            toAccountId = toAccountId,
            splitGroupId = splitGroupId,
            receiptPath = receiptPath,
            recurringRuleId = recurringRuleId
        )

        return TransactionWithCategory(transaction, category)
    }
}
