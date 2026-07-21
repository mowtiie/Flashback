package com.mowtiie.flashback.ui.study;

import com.mowtiie.flashback.data.model.StudyCard;
import com.mowtiie.flashback.scheduler.CardState;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class StudyQueue {

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

    public void requeue(StudyCard card) {
        if (CardState.isLearningLike(card.card.state)) {
            learning.add(card);
        }
    }

    public void restore(StudyCard card) {
        place(card, true);
    }

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
