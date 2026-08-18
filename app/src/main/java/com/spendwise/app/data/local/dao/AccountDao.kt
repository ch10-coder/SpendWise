package com.spendwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.model.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccount(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE type = :type ORDER BY name ASC")
    fun getAccountsByType(type: AccountType): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    /** Calculate account balance from transactions.
     *  Income adds, Expense subtracts, Transfers subtract from source. */
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'INCOME'), 0
        ) - COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'EXPENSE'), 0
        ) - COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'TRANSFER'), 0
        ) + COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE toAccountId = :accountId AND type = 'TRANSFER'), 0
        )
    """)
    fun getCalculatedBalance(accountId: Long): Flow<Double?>
}
