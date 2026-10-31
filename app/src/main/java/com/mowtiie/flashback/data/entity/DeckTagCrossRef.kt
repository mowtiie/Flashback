package com.mowtiie.flashback.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "deck_tag",
    primaryKeys = ["deckId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class DeckTagCrossRef(
    val deckId: Long,
    val tagId: Long,
)
