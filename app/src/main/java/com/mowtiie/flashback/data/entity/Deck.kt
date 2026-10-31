package com.mowtiie.flashback.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    @ColumnInfo(defaultValue = "0") val reverseByDefault: Boolean = false,
    @ColumnInfo(defaultValue = "20") val newPerDay: Int = 20,
    @ColumnInfo(defaultValue = "200") val reviewsPerDay: Int = 200,
    val createdAt: Long = System.currentTimeMillis(),
)
