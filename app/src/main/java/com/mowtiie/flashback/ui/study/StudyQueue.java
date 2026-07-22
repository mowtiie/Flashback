package com.mowtiie.flashback.ui.study;

import com.mowtiie.flashback.data.model.StudyCard;
import com.mowtiie.flashback.scheduler.CardState;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * The in-session card queue.
 *
 * <p>A flashcard session is not a list you walk once. Learning cards are due
 * again in one or ten minutes, so they re-enter the queue mid-session, and the
 * order has to be recomputed against the clock every time a card is requested.
 * That is the whole reason this class exists rather than an index into a list.
 *
 * <p>Two stores: cards waiting their first look this session, in the order the
 * repository returned them, and cards in a learning phase ordered by when they
 * come due. Learning cards win whenever one is ready, because a card scheduled
 * one minute out is worthless if it is shown an hour later.
 *
 * <p>Free of Android and Room types so it can be unit tested on the JVM.
 */
public class StudyQueue {

    /**
     * How far ahead of its due time a learning card may be pulled forward when
     * nothing else is left. Without this the session would either end with
     * cards still owing, or spin waiting on a card due in nine minutes.
     */
    public static final long LEARN_AHEAD_MS = 20 * 60_000L;

    private final ArrayDeque<StudyCard> upcoming = new ArrayDeque<>();

    private final PriorityQueue<StudyCard> learning = new PriorityQueue<>(
            Comparator.comparingLong(c -> c.card.dueAt));

    public void seed(List<StudyCard> cards) {
        upcoming.clear();
        learning.clear();
        for (StudyCard card : cards) {
            place(card, false);
        }
    }

    /**
     * Hands out the next card, or null when nothing is available. Null does not
     * always mean "finished": see {@link #nextLearningDueAt()}.
     */
    public StudyCard next(long now) {
        StudyCard head = learning.peek();
        if (head != null && head.card.dueAt <= now) {
            return learning.poll();
        }
        if (!upcoming.isEmpty()) {
            return upcoming.poll();
        }
        if (head != null && head.card.dueAt - now <= LEARN_AHEAD_MS) {
            return learning.poll();
        }
        return null;
    }

    /** Puts an answered card back if its new state keeps it in the session. */
    public void requeue(StudyCard card) {
        if (CardState.isLearningLike(card.card.state)) {
            learning.add(card);
        }
    }

    /** Returns a card to the front of the queue, used when a review is undone. */
    public void restore(StudyCard card) {
        place(card, true);
    }

    /**
     * Takes a card out wherever it sits. Undo needs this because the card being
     * restored may already have been requeued into the learning store.
     */
    public boolean remove(StudyCard card) {
        return learning.remove(card) || upcoming.remove(card);
    }

    private void place(StudyCard card, boolean front) {
        if (card.card.state == CardState.NEW) {
            if (front) {
                upcoming.addFirst(card);
            } else {
                upcoming.addLast(card);
            }
        } else if (CardState.isLearningLike(card.card.state)) {
            learning.add(card);
        } else if (front) {
            upcoming.addFirst(card);
        } else {
            upcoming.addLast(card);
        }
    }

    /** When the earliest waiting learning card comes due, or null if there is none. */
    public Long nextLearningDueAt() {
        StudyCard head = learning.peek();
        return head == null ? null : head.card.dueAt;
    }

    public boolean isEmpty() {
        return upcoming.isEmpty() && learning.isEmpty();
    }

    public int size() {
        return upcoming.size() + learning.size();
    }

    public int countNew() {
        return countUpcoming(CardState.NEW);
    }

    public int countReview() {
        return countUpcoming(CardState.REVIEW);
    }

    public int countLearning() {
        return learning.size();
    }

    private int countUpcoming(int state) {
        int total = 0;
        for (StudyCard card : upcoming) {
            if (card.card.state == state) {
                total++;
            }
        }
        return total;
    }
}
