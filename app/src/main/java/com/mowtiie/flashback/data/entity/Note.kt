package com.mowtiie.flashback.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deckId")],
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String = "",
    val back: String = "",
    val reverseEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = createdAt,
)
