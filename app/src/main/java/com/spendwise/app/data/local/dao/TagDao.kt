package com.spendwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.app.data.local.entity.TagEntity
import com.spendwise.app.data.local.entity.TransactionTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: Long)

    // --- Cross-reference operations ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionTag(crossRef: TransactionTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionTags(crossRefs: List<TransactionTagCrossRef>)

    @Delete
    suspend fun deleteTransactionTag(crossRef: TransactionTagCrossRef)

    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteAllTagsForTransaction(transactionId: Long)

    /** Get all tags associated with a specific transaction */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN transaction_tags tt ON t.id = tt.tagId
        WHERE tt.transactionId = :transactionId
        ORDER BY t.name ASC
    """)
    fun getTagsForTransaction(transactionId: Long): Flow<List<TagEntity>>

    /** Get all tags associated with a specific transaction (suspend version) */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN transaction_tags tt ON t.id = tt.tagId
        WHERE tt.transactionId = :transactionId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsForTransactionList(transactionId: Long): List<TagEntity>

    /** Get total spending for transactions with a specific tag in a date range */
    @Query("""
        SELECT COALESCE(SUM(tx.amount), 0) FROM transactions tx
        INNER JOIN transaction_tags tt ON tx.id = tt.transactionId
        WHERE tt.tagId = :tagId 
        AND tx.type = 'EXPENSE'
        AND tx.dateTimestamp BETWEEN :startTimestamp AND :endTimestamp
    """)
    fun getTagSpendingBetweenDates(tagId: Long, startTimestamp: Long, endTimestamp: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int
}
