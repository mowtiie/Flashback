package com.mowtiie.flashback.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.mowtiie.flashback.scheduler.SchedulingState;

@Entity(
        tableName = "review_log",
        foreignKeys = @ForeignKey(
                entity = Card.class,
                parentColumns = "id",
                childColumns = "cardId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("cardId"), @Index("reviewedAt")})
public class ReviewLog {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long cardId;

    public long reviewedAt;

    public int rating;

    public long elapsedMs;

    public int prevState;
    public int prevLearningStep;
    public int prevIntervalDays;
    public double prevEaseFactor;
    public int prevReps;
    public int prevLapses;
    public long prevDueAt;
    public long prevLastReviewedAt;

    public int newIntervalDays;

    public ReviewLog() {
    }

    public static ReviewLog of(long cardId, int rating, long elapsedMs,
                               SchedulingState before, SchedulingState after, long now) {
        ReviewLog log = new ReviewLog();
        log.cardId = cardId;
        log.reviewedAt = now;
        log.rating = rating;
        log.elapsedMs = elapsedMs;
        log.prevState = before.state;
        log.prevLearningStep = before.learningStep;
        log.prevIntervalDays = before.intervalDays;
        log.prevEaseFactor = before.easeFactor;
        log.prevReps = before.reps;
        log.prevLapses = before.lapses;
        log.prevDueAt = before.dueAt;
        log.prevLastReviewedAt = before.lastReviewedAt;
        log.newIntervalDays = after.intervalDays;
        return log;
    }

    public SchedulingState toPreviousState() {
        SchedulingState s = new SchedulingState();
        s.state = prevState;
        s.learningStep = prevLearningStep;
        s.intervalDays = prevIntervalDays;
        s.easeFactor = prevEaseFactor;
        s.reps = prevReps;
        s.lapses = prevLapses;
        s.dueAt = prevDueAt;
        s.lastReviewedAt = prevLastReviewedAt;
        return s;
    }
}
