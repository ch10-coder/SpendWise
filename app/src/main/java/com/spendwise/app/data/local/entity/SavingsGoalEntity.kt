package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user's savings goal with target amount,
 * current progress, and optional deadline.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    /** Optional deadline as epoch millis (null = no deadline) */
    val deadline: Long? = null,
    val iconName: String = "Savings",
    val colorHex: String = "#10B981",
    val createdAt: Long = System.currentTimeMillis()
)
