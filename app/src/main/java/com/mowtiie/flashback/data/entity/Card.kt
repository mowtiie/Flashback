package com.mowtiie.flashback.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mowtiie.flashback.scheduler.CardState
import com.mowtiie.flashback.scheduler.SchedulingState

/** Ordinal 0 asks front then back; ordinal 1 the reverse. Scheduled independently. */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dueAt"),
        Index(value = ["noteId", "ordinal"], unique = true),
    ],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val ordinal: Int,
    var state: Int = CardState.NEW,
    var learningStep: Int = 0,
    var intervalDays: Int = 0,
    var easeFactor: Double = 2.5,
    var reps: Int = 0,
    var lapses: Int = 0,
    var dueAt: Long = 0,
    var lastReviewedAt: Long = 0,
    val suspended: Boolean = false,
) {
    fun toSchedulingState() = SchedulingState(
        state = state,
        learningStep = learningStep,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
        reps = reps,
        lapses = lapses,
        dueAt = dueAt,
        lastReviewedAt = lastReviewedAt,
    )

    fun applySchedulingState(s: SchedulingState) {
        state = s.state
        learningStep = s.learningStep
        intervalDays = s.intervalDays
        easeFactor = s.easeFactor
        reps = s.reps
        lapses = s.lapses
        dueAt = s.dueAt
        lastReviewedAt = s.lastReviewedAt
    }

    companion object {
        const val ORDINAL_FORWARD = 0
        const val ORDINAL_REVERSE = 1
    }
}
