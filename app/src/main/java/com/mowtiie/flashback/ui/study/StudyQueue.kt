package com.mowtiie.flashback.ui.study

import com.mowtiie.flashback.data.model.StudyCard
import com.mowtiie.flashback.scheduler.CardState
import java.util.ArrayDeque
import java.util.PriorityQueue

/**
 * In-session card queue. Learning cards re-enter mid-session, so order is
 * recomputed against the clock on every request.
 */
class StudyQueue {

    private val upcoming = ArrayDeque<StudyCard>()
    private val learning = PriorityQueue<StudyCard>(compareBy { it.card.dueAt })

    var countNew = 0
        private set
    var countReview = 0
        private set

    fun seed(cards: List<StudyCard>) {
        upcoming.clear()
        learning.clear()
        countNew = 0
        countReview = 0
        for (card in cards) {
            place(card, front = false)
        }
    }

    /** Next card, or null. Null doesn't always mean finished; see [nextLearningDueAt]. */
    fun next(now: Long): StudyCard? {
        val head = learning.peek()
        if (head != null && head.card.dueAt <= now) {
            return learning.poll()
        }
        if (upcoming.isNotEmpty()) {
            return pollUpcoming()
        }
        if (head != null && head.card.dueAt - now <= LEARN_AHEAD_MS) {
            return learning.poll()
        }
        return null
    }

    fun requeue(card: StudyCard) {
        if (CardState.isLearningLike(card.card.state)) {
            learning.add(card)
        }
    }

    fun restore(card: StudyCard) {
        place(card, front = true)
    }

    /** Removes a card wherever it sits; undo needs it as the card may have been requeued. */
    fun remove(card: StudyCard): Boolean {
        if (learning.remove(card)) {
            return true
        }
        if (upcoming.remove(card)) {
            adjust(card, -1)
            return true
        }
        return false
    }

    fun removeNote(noteId: Long) {
        val waiting = upcoming.iterator()
        while (waiting.hasNext()) {
            val card = waiting.next()
            if (card.card.noteId == noteId) {
                waiting.remove()
                adjust(card, -1)
            }
        }
        learning.removeIf { it.card.noteId == noteId }
    }

    private fun place(card: StudyCard, front: Boolean) {
        // isLearningLike also covers NEW, so rule NEW out first.
        if (card.card.state != CardState.NEW && CardState.isLearningLike(card.card.state)) {
            learning.add(card)
            return
        }
        if (front) {
            upcoming.addFirst(card)
        } else {
            upcoming.addLast(card)
        }
        adjust(card, 1)
    }

    private fun pollUpcoming(): StudyCard? {
        val card = upcoming.poll()
        if (card != null) {
            adjust(card, -1)
        }
        return card
    }

    private fun adjust(card: StudyCard, delta: Int) {
        when (card.card.state) {
            CardState.NEW -> countNew += delta
            CardState.REVIEW -> countReview += delta
        }
    }

    fun nextLearningDueAt(): Long? = learning.peek()?.card?.dueAt

    fun isEmpty(): Boolean = upcoming.isEmpty() && learning.isEmpty()

    val size: Int get() = upcoming.size + learning.size

    val countLearning: Int get() = learning.size

    companion object {
        /** How far ahead a learning card may be pulled forward when nothing else is left. */
        const val LEARN_AHEAD_MS = 20 * 60_000L
    }
}
