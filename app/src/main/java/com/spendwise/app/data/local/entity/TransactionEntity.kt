package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendwise.app.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("dateTimestamp"),
        Index(value = ["type", "dateTimestamp"]),
        Index("accountId"),
        Index("toAccountId"),
        Index("splitGroupId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long?,
    val dateTimestamp: Long,
    val note: String? = null,
    /** Account this transaction belongs to (null = default/unassigned) */
    val accountId: Long? = null,
    /** Target account for TRANSFER type transactions */
    val toAccountId: Long? = null,
    /** Links split transactions together via shared group ID */
    val splitGroupId: Long? = null,
    /** Path to receipt image file on device */
    val receiptPath: String? = null,
    /** ID of the recurring rule that generated this transaction (null if manual) */
    val recurringRuleId: Long? = null
)
