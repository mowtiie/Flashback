package com.mowtiie.flashback.ui.study

import com.mowtiie.flashback.data.entity.Card
import com.mowtiie.flashback.data.model.StudyCard
import com.mowtiie.flashback.scheduler.CardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StudyQueueTest {

    private lateinit var queue: StudyQueue

    @Before
    fun setUp() {
        queue = StudyQueue()
    }

    private fun card(id: Long, state: Int, dueAt: Long, noteId: Long = 0): StudyCard =
        StudyCard(
            card = Card(id = id, noteId = noteId, ordinal = 0, state = state, dueAt = dueAt),
            front = "front$id",
            back = "back$id",
        )

    @Test
    fun removeNoteDropsEveryCardOfThatNote() {
        val forward = card(1, CardState.NEW, 0, noteId = 10)
        val reverse = card(2, CardState.NEW, 0, noteId = 10)
        val learning = card(3, CardState.LEARNING, NOW - MIN, noteId = 10)
        val other = card(4, CardState.NEW, 0, noteId = 11)
        queue.seed(listOf(forward, reverse, learning, other))

        queue.removeNote(10)

        assertEquals(1, queue.size)
        assertEquals(4, queue.next(NOW)!!.card.id)
    }

    @Test
    fun runningCountsStayInStepThroughEveryMutation() {
        val newA = card(1, CardState.NEW, 0)
        val newB = card(2, CardState.NEW, 0, noteId = 7)
        val reviewA = card(3, CardState.REVIEW, NOW - MIN)
        val reviewB = card(4, CardState.REVIEW, NOW - MIN, noteId = 7)
        queue.seed(listOf(newA, newB, reviewA, reviewB))
        assertEquals(2, queue.countNew)
        assertEquals(2, queue.countReview)

        val first = queue.next(NOW)!!              // newA leaves the queue
        assertEquals(1, queue.countNew)
        queue.restore(first)                       // and comes back
        assertEquals(2, queue.countNew)

        queue.remove(reviewA)
        assertEquals(1, queue.countReview)
        queue.remove(reviewA)                      // already gone: no double count
        assertEquals(1, queue.countReview)

        queue.removeNote(7)                        // newB and reviewB together
        assertEquals(1, queue.countNew)
        assertEquals(0, queue.countReview)

        queue.seed(listOf(card(9, CardState.NEW, 0)))
        assertEquals(1, queue.countNew)
        assertEquals(0, queue.countReview)
    }

    @Test
    fun seedPartitionsCardsByState() {
        queue.seed(
            listOf(
                card(1, CardState.NEW, 0),
                card(2, CardState.NEW, 0),
                card(3, CardState.REVIEW, NOW - MIN),
                card(4, CardState.LEARNING, NOW - MIN),
            ),
        )

        assertEquals(2, queue.countNew)
        assertEquals(1, queue.countReview)
        assertEquals(1, queue.countLearning)
        assertEquals(4, queue.size)
    }

    @Test
    fun dueLearningCardOutranksEverythingWaiting() {
        queue.seed(
            listOf(
                card(1, CardState.NEW, 0),
                card(2, CardState.REVIEW, NOW - MIN),
                card(3, CardState.LEARNING, NOW - MIN),
            ),
        )

        assertEquals(3L, queue.next(NOW)!!.card.id)
    }

    @Test
    fun remainingCardsKeepRepositoryOrder() {
        queue.seed(
            listOf(
                card(1, CardState.NEW, 0),
                card(2, CardState.NEW, 0),
                card(3, CardState.REVIEW, NOW - MIN),
            ),
        )

        assertEquals(1L, queue.next(NOW)!!.card.id)
        assertEquals(2L, queue.next(NOW)!!.card.id)
        assertEquals(3L, queue.next(NOW)!!.card.id)
        assertNull(queue.next(NOW))
        assertTrue(queue.isEmpty())
    }

    @Test
    fun earliestLearningCardWinsRegardlessOfInsertionOrder() {
        queue.seed(
            listOf(
                card(1, CardState.LEARNING, NOW + 9 * MIN),
                card(2, CardState.LEARNING, NOW - MIN),
            ),
        )

        assertEquals(2L, queue.next(NOW)!!.card.id)
    }

    @Test
    fun cardAnsweredIntoLearningReturnsToTheSession() {
        queue.seed(listOf(card(1, CardState.NEW, 0)))

        val drawn = queue.next(NOW)!!
        drawn.card.state = CardState.LEARNING
        drawn.card.dueAt = NOW + 10 * MIN
        queue.requeue(drawn)

        assertEquals(1, queue.size)
    }

    @Test
    fun graduatedCardLeavesTheSession() {
        queue.seed(listOf(card(1, CardState.NEW, 0)))

        val drawn = queue.next(NOW)!!
        drawn.card.state = CardState.REVIEW
        drawn.card.dueAt = NOW + 86_400_000L
        queue.requeue(drawn)

        assertTrue(queue.isEmpty())
    }

    @Test
    fun learningCardInsideWindowIsPulledForward() {
        queue.seed(listOf(card(1, CardState.LEARNING, NOW + 5 * MIN)))

        assertEquals(1L, queue.next(NOW)!!.card.id)
    }

    @Test
    fun learningCardBeyondWindowIsWithheldButStillQueued() {
        queue.seed(listOf(card(1, CardState.LEARNING, NOW + 45 * MIN)))

        assertNull(queue.next(NOW))
        assertFalse(queue.isEmpty())
        assertEquals(NOW + 45 * MIN, queue.nextLearningDueAt())
    }

    @Test
    fun undoTakesACardBackOutOfTheLearningStore() {
        queue.seed(
            listOf(
                card(1, CardState.NEW, 0),
                card(2, CardState.NEW, 0),
            ),
        )

        val answered = queue.next(NOW)!!
        answered.card.state = CardState.LEARNING
        answered.card.dueAt = NOW + MIN
        queue.requeue(answered)

        assertTrue(queue.remove(answered))

        answered.card.state = CardState.NEW
        answered.card.dueAt = 0
        val displaced = queue.next(NOW)!!
        queue.restore(displaced)
        queue.restore(answered)

        assertEquals(1L, queue.next(NOW)!!.card.id)
        assertEquals(2L, queue.next(NOW)!!.card.id)
    }

    @Test
    fun removingAnAbsentCardReportsFailureRatherThanThrowing() {
        queue.seed(listOf(card(1, CardState.NEW, 0)))

        assertFalse(queue.remove(card(99, CardState.NEW, 0)))
    }

    @Test
    fun emptyQueueYieldsNothing() {
        queue.seed(emptyList())

        assertNull(queue.next(NOW))
        assertNull(queue.nextLearningDueAt())
        assertTrue(queue.isEmpty())
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val MIN = 60_000L
    }
}
