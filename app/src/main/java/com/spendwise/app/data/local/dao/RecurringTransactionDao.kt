package com.spendwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.app.data.local.entity.RecurringTransactionEntity
import com.spendwise.app.model.RecurrenceFrequency
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions ORDER BY nextOccurrence ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextOccurrence ASC")
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getRecurringTransactionById(id: Long): RecurringTransactionEntity?

    /** Get all active recurring transactions whose nextOccurrence is at or before the given timestamp */
    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE isActive = 1 AND nextOccurrence <= :timestamp
        ORDER BY nextOccurrence ASC
    """)
    suspend fun getDueRecurringTransactions(timestamp: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(rule: RecurringTransactionEntity): Long

    @Update
    suspend fun updateRecurringTransaction(rule: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurringTransaction(rule: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: Long)

    /** Pause/resume a recurring transaction */
    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    /** Update the next occurrence date after generating a transaction */
    @Query("UPDATE recurring_transactions SET nextOccurrence = :nextOccurrence WHERE id = :id")
    suspend fun updateNextOccurrence(id: Long, nextOccurrence: Long)
}
