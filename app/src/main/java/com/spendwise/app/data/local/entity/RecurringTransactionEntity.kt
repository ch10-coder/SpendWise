package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spendwise.app.model.RecurrenceFrequency
import com.spendwise.app.model.TransactionType

/**
 * Defines a recurring transaction rule that automatically generates
 * transactions at specified intervals (daily, weekly, monthly, etc.).
 *
 * The app checks [nextOccurrence] on launch and creates pending
 * transactions for any dates that have passed.
 */
@Entity(
    tableName = "recurring_transactions",
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
        )
    ],
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("nextOccurrence"),
        Index("isActive")
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long?,
    val accountId: Long? = null,
    val frequency: RecurrenceFrequency,
    val startDate: Long,
    /** Optional end date — null means indefinite */
    val endDate: Long? = null,
    /** Epoch millis of the next scheduled occurrence */
    val nextOccurrence: Long,
    val isActive: Boolean = true,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
