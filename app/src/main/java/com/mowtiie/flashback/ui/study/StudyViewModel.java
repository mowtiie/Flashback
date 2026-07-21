package com.mowtiie.flashback.ui.study;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.flashback.data.model.StudyCard;
import com.mowtiie.flashback.repository.FlashbackRepository;
import com.mowtiie.flashback.scheduler.CardState;
import com.mowtiie.flashback.scheduler.Rating;
import com.mowtiie.flashback.scheduler.SchedulingState;
import com.mowtiie.flashback.scheduler.Sm2Scheduler;
import com.mowtiie.flashback.util.IntervalFormatter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

public class StudyViewModel extends AndroidViewModel {

    public static class Counts {

        public final int newCards;
        public final int learning;
        public final int due;

        Counts(int newCards, int learning, int due) {
            this.newCards = newCards;
            this.learning = learning;
            this.due = due;
        }
    }

    public enum Ending {
        NOTHING_DUE,
        FINISHED,
        WAITING
    }

    private static class Answered {

        final StudyCard card;
        final SchedulingState before;

        Answered(StudyCard card, SchedulingState before) {
            this.card = card;
            this.before = before;
        }
    }

    private final FlashbackRepository repository;
    private final long deckId;
    private final StudyQueue queue = new StudyQueue();
    private final Deque<Answered> history = new ArrayDeque<>();

    private final Sm2Scheduler previewScheduler = new Sm2Scheduler();

    private final MutableLiveData<StudyCard> current = new MutableLiveData<>();
    private final MutableLiveData<Boolean> revealed = new MutableLiveData<>(false);
    private final MutableLiveData<Counts> counts = new MutableLiveData<>();
    private final MutableLiveData<Ending> ending = new MutableLiveData<>();
    private final MutableLiveData<Boolean> canUndo = new MutableLiveData<>(false);
    private final MutableLiveData<Map<Rating, String>> previews = new MutableLiveData<>();
    private final MutableLiveData<Long> waitingUntil = new MutableLiveData<>();

    private long cardShownAt;
    private int answeredThisSession;

    private boolean busy;

    public StudyViewModel(@NonNull Application application, long deckId) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.deckId = deckId;
        loadSession();
    }

    public LiveData<StudyCard> getCurrent() {
        return current;
    }

    public LiveData<Boolean> getRevealed() {
        return revealed;
    }

    public LiveData<Counts> getCounts() {
        return counts;
    }

    public LiveData<Ending> getEnding() {
        return ending;
    }

    public LiveData<Boolean> getCanUndo() {
        return canUndo;
    }

    public LiveData<Map<Rating, String>> getPreviews() {
        return previews;
    }

    public LiveData<Long> getWaitingUntil() {
        return waitingUntil;
    }

    public int getAnsweredThisSession() {
        return answeredThisSession;
    }

    private void loadSession() {
        long now = System.currentTimeMillis();
        repository.buildStudyQueue(deckId, now, cards -> {
            queue.seed(cards);
            if (cards.isEmpty()) {
                ending.setValue(Ending.NOTHING_DUE);
                publishCounts();
                return;
            }
            advance();
        });
    }

    public void reveal() {
        StudyCard card = current.getValue();
        if (card == null || Boolean.TRUE.equals(revealed.getValue())) {
            return;
        }
        revealed.setValue(true);
        publishPreviews(card);
    }

    private void publishPreviews(StudyCard card) {
        long now = System.currentTimeMillis();
        Map<Rating, String> labels = new EnumMap<>(Rating.class);
        for (Rating rating : Rating.values()) {
            SchedulingState projected = previewScheduler.preview(card.card.toSchedulingState(), rating, now);
            labels.put(rating, IntervalFormatter.format(getApplication(), projected, now));
        }
        previews.setValue(labels);
    }

    public void answer(Rating rating) {
        StudyCard card = current.getValue();
        if (card == null || busy || !Boolean.TRUE.equals(revealed.getValue())) {
            return;
        }
        busy = true;

        long now = System.currentTimeMillis();
        SchedulingState before = card.card.toSchedulingState();

        repository.answerCard(card.card.id, rating, now - cardShownAt, now, result -> {
            busy = false;
            if (result == null) {
                advance();
                return;
            }
            card.card.applySchedulingState(result.after);
            queue.requeue(card);

            history.push(new Answered(card, before));
            answeredThisSession++;
            canUndo.setValue(true);
            advance();
        });
    }

    public void undo() {
        if (history.isEmpty() || busy) {
            return;
        }
        busy = true;

        repository.undoLastReview(cardId -> {
            busy = false;
            if (cardId == null || cardId < 0) {
                return;
            }
            Answered entry = history.pop();

            queue.remove(entry.card);
            entry.card.card.applySchedulingState(entry.before);

            StudyCard onScreen = current.getValue();
            if (onScreen != null) {
                queue.restore(onScreen);
            }

            answeredThisSession = Math.max(0, answeredThisSession - 1);
            canUndo.setValue(!history.isEmpty());
            ending.setValue(null);
            show(entry.card);
        });
    }

    private void advance() {
        long now = System.currentTimeMillis();
        StudyCard next = queue.next(now);

        if (next != null) {
            show(next);
            return;
        }

        current.setValue(null);
        revealed.setValue(false);
        publishCounts();

        Long dueAt = queue.nextLearningDueAt();
        if (dueAt != null) {
            waitingUntil.setValue(dueAt);
            ending.setValue(Ending.WAITING);
        } else {
            ending.setValue(Ending.FINISHED);
        }
    }

    private void show(StudyCard card) {
        cardShownAt = System.currentTimeMillis();
        revealed.setValue(false);
        current.setValue(card);
        publishCounts();
    }

    private void publishCounts() {
        int newCards = queue.countNew();
        int learning = queue.countLearning();
        int due = queue.countReview();

        StudyCard onScreen = current.getValue();
        if (onScreen != null) {
            if (onScreen.card.state == CardState.NEW) {
                newCards++;
            } else if (onScreen.card.state == CardState.REVIEW) {
                due++;
            } else {
                learning++;
            }
        }
        counts.setValue(new Counts(newCards, learning, due));
    }
}
