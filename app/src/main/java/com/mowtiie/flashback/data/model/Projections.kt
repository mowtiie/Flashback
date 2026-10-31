package com.mowtiie.flashback.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.entity.Deck
import com.mowtiie.flashback.data.entity.DeckTagCrossRef
import com.mowtiie.flashback.data.entity.Tag

data class CardStateBreakdown(
    val newCount: Int,
    val learningCount: Int,
    val youngCount: Int,
    val matureCount: Int,
) {
    fun total(): Int = newCount + learningCount + youngCount + matureCount
}

data class DailyCount(
    val day: String,
    val count: Int,
)

data class DeckListItem(
    val summary: DeckSummary,
    val tags: List<Tag>,
)

data class DeckSummary(
    @Embedded val deck: Deck,
    val newCount: Int,
    val learnCount: Int,
    val dueCount: Int,
    val totalCount: Int,
    val newToday: Int,
    val reviewsToday: Int,
) {
    fun availableNew(): Int = maxOf(0, minOf(newCount, deck.newPerDay - newToday))

    fun availableDue(): Int = maxOf(0, minOf(dueCount, deck.reviewsPerDay - reviewsToday))

    fun studyableCount(): Int = availableNew() + learnCount + availableDue()
}

data class DeckWithTags(
    @Embedded val deck: Deck,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = DeckTagCrossRef::class,
            parentColumn = "deckId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<Tag>,
)

data class NotePreview(
    val id: Long,
    val front: String,
    val back: String,
    val reverseEnabled: Boolean,
)

/** Identity equality on purpose: the study queue tracks the same instance across state changes. */
class StudyCard(
    @Embedded val card: Card,
    var front: String,
    var back: String,
) {
    fun question(): String = if (card.ordinal == Card.ORDINAL_REVERSE) back else front

    fun answer(): String = if (card.ordinal == Card.ORDINAL_REVERSE) front else back
}

data class TagWithCount(
    @Embedded val tag: Tag,
    val deckCount: Int,
)
