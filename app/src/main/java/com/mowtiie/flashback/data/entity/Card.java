package com.mowtiie.flashback.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.mowtiie.flashback.scheduler.CardState;
import com.mowtiie.flashback.scheduler.SchedulingState;

@Entity(
        tableName = "cards",
        foreignKeys = @ForeignKey(
                entity = Note.class,
                parentColumns = "id",
                childColumns = "noteId",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index("dueAt"),
                @Index(value = {"noteId", "ordinal"}, unique = true)
        })
public class Card {

    public static final int ORDINAL_FORWARD = 0;
    public static final int ORDINAL_REVERSE = 1;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long noteId;

    public int ordinal;

    public int state = CardState.NEW;

    public int learningStep;

    public int intervalDays;

    public double easeFactor = 2.5d;

    public int reps;

    public int lapses;

    public long dueAt;

    public long lastReviewedAt;

    public boolean suspended;

    public Card() {
    }

    @Ignore
    public Card(long noteId, int ordinal) {
        this.noteId = noteId;
        this.ordinal = ordinal;
    }

    public SchedulingState toSchedulingState() {
        SchedulingState s = new SchedulingState();
        s.state = state;
        s.learningStep = learningStep;
        s.intervalDays = intervalDays;
        s.easeFactor = easeFactor;
        s.reps = reps;
        s.lapses = lapses;
        s.dueAt = dueAt;
        s.lastReviewedAt = lastReviewedAt;
        return s;
    }

    public void applySchedulingState(SchedulingState s) {
        state = s.state;
        learningStep = s.learningStep;
        intervalDays = s.intervalDays;
        easeFactor = s.easeFactor;
        reps = s.reps;
        lapses = s.lapses;
        dueAt = s.dueAt;
        lastReviewedAt = s.lastReviewedAt;
    }
}
