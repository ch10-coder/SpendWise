package com.spendwise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendwise.app.data.local.entity.AccountEntity
import com.spendwise.app.model.AccountType
import kotlinx.coroutines.flow.Flow

data class AccountBalanceTuple(
    @Embedded val account: AccountEntity,
    val calculatedBalance: Double
)

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

    @Query("UPDATE accounts SET isDefault = 0 WHERE id != :id")
    suspend fun clearDefaultExcept(id: Long)

    /** Every account with its live balance: the opening balance plus income, minus expenses
     *  and transfers out, plus transfers in. */
    @Query("""
        SELECT a.*, a.balance
            + COALESCE((SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                        FROM transactions t WHERE t.accountId = a.id), 0)
            + COALESCE((SELECT SUM(t.amount) FROM transactions t
                        WHERE t.toAccountId = a.id AND t.type = 'TRANSFER'), 0) AS calculatedBalance
        FROM accounts a
        ORDER BY a.isDefault DESC, a.name ASC
    """)
    fun getAccountsWithBalance(): Flow<List<AccountBalanceTuple>>

    /** Calculate account balance from transactions.
     *  Opening balance, plus income, minus expenses and transfers out, plus transfers in. */
    @Query("""
        SELECT (SELECT balance FROM accounts WHERE id = :accountId) + COALESCE(
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
