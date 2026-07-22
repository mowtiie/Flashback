package com.mowtiie.flashback.ui.study;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.mowtiie.flashback.data.entity.Card;
import com.mowtiie.flashback.data.model.StudyCard;
import com.mowtiie.flashback.scheduler.CardState;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Pure JVM tests: the queue deliberately has no Android or Room dependencies. */
public class StudyQueueTest {

    private static final long NOW = 1_700_000_000_000L;
    private static final long MIN = 60_000L;

    private StudyQueue queue;

    @Before
    public void setUp() {
        queue = new StudyQueue();
    }

    private static StudyCard card(long id, int state, long dueAt) {
        StudyCard studyCard = new StudyCard();
        studyCard.card = new Card();
        studyCard.card.id = id;
        studyCard.card.state = state;
        studyCard.card.dueAt = dueAt;
        studyCard.front = "front" + id;
        studyCard.back = "back" + id;
        return studyCard;
    }

    @Test
    public void seedPartitionsCardsByState() {
        queue.seed(Arrays.asList(
                card(1, CardState.NEW, 0),
                card(2, CardState.NEW, 0),
                card(3, CardState.REVIEW, NOW - MIN),
                card(4, CardState.LEARNING, NOW - MIN)));

        assertEquals(2, queue.countNew());
        assertEquals(1, queue.countReview());
        assertEquals(1, queue.countLearning());
        assertEquals(4, queue.size());
    }

    @Test
    public void dueLearningCardOutranksEverythingWaiting() {
        queue.seed(Arrays.asList(
                card(1, CardState.NEW, 0),
                card(2, CardState.REVIEW, NOW - MIN),
                card(3, CardState.LEARNING, NOW - MIN)));

        assertEquals(3L, queue.next(NOW).card.id);
    }

    @Test
    public void remainingCardsKeepRepositoryOrder() {
        queue.seed(Arrays.asList(
                card(1, CardState.NEW, 0),
                card(2, CardState.NEW, 0),
                card(3, CardState.REVIEW, NOW - MIN)));

        assertEquals(1L, queue.next(NOW).card.id);
        assertEquals(2L, queue.next(NOW).card.id);
        assertEquals(3L, queue.next(NOW).card.id);
        assertNull(queue.next(NOW));
        assertTrue(queue.isEmpty());
    }

    @Test
    public void earliestLearningCardWinsRegardlessOfInsertionOrder() {
        queue.seed(Arrays.asList(
                card(1, CardState.LEARNING, NOW + 9 * MIN),
                card(2, CardState.LEARNING, NOW - MIN)));

        assertEquals(2L, queue.next(NOW).card.id);
    }

    @Test
    public void cardAnsweredIntoLearningReturnsToTheSession() {
        queue.seed(new ArrayList<>(Arrays.asList(card(1, CardState.NEW, 0))));

        StudyCard drawn = queue.next(NOW);
        drawn.card.state = CardState.LEARNING;
        drawn.card.dueAt = NOW + 10 * MIN;
        queue.requeue(drawn);

        assertEquals(1, queue.size());
    }

    @Test
    public void graduatedCardLeavesTheSession() {
        queue.seed(new ArrayList<>(Arrays.asList(card(1, CardState.NEW, 0))));

        StudyCard drawn = queue.next(NOW);
        drawn.card.state = CardState.REVIEW;
        drawn.card.dueAt = NOW + 86_400_000L;
        queue.requeue(drawn);

        assertTrue(queue.isEmpty());
    }

    @Test
    public void learningCardInsideWindowIsPulledForward() {
        queue.seed(Arrays.asList(card(1, CardState.LEARNING, NOW + 5 * MIN)));

        assertEquals(1L, queue.next(NOW).card.id);
    }

    @Test
    public void learningCardBeyondWindowIsWithheldButStillQueued() {
        queue.seed(Arrays.asList(card(1, CardState.LEARNING, NOW + 45 * MIN)));

        assertNull(queue.next(NOW));
        assertFalse(queue.isEmpty());
        assertEquals(Long.valueOf(NOW + 45 * MIN), queue.nextLearningDueAt());
    }

    @Test
    public void undoTakesACardBackOutOfTheLearningStore() {
        queue.seed(new ArrayList<>(Arrays.asList(
                card(1, CardState.NEW, 0),
                card(2, CardState.NEW, 0))));

        StudyCard answered = queue.next(NOW);
        answered.card.state = CardState.LEARNING;
        answered.card.dueAt = NOW + MIN;
        queue.requeue(answered);

        assertTrue(queue.remove(answered));

        answered.card.state = CardState.NEW;
        answered.card.dueAt = 0;
        StudyCard displaced = queue.next(NOW);
        queue.restore(displaced);
        queue.restore(answered);

        assertEquals(1L, queue.next(NOW).card.id);
        assertEquals(2L, queue.next(NOW).card.id);
    }

    @Test
    public void removingAnAbsentCardReportsFailureRatherThanThrowing() {
        queue.seed(new ArrayList<>(Arrays.asList(card(1, CardState.NEW, 0))));

        assertFalse(queue.remove(card(99, CardState.NEW, 0)));
    }

    @Test
    public void emptyQueueYieldsNothing() {
        queue.seed(new ArrayList<StudyCard>());

        assertNull(queue.next(NOW));
        assertNull(queue.nextLearningDueAt());
        assertTrue(queue.isEmpty());
    }
}
