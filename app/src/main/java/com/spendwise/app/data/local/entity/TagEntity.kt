package com.spendwise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined tag that can be applied to transactions.
 * Tags provide cross-cutting labels beyond categories
 * (e.g., "vacation", "business", "shared").
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#6366F1"
)
