package com.mowtiie.flashback.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mowtiie.flashback.scheduler.SchedulingState

/** Pre-answer snapshot of a card, so undo is a straight restore. Also the source for statistics. */
@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = Card::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cardId"), Index("reviewedAt")],
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val reviewedAt: Long,
    val rating: Int,
    val elapsedMs: Long,
    val prevState: Int,
    val prevLearningStep: Int,
    val prevIntervalDays: Int,
    val prevEaseFactor: Double,
    val prevReps: Int,
    val prevLapses: Int,
    val prevDueAt: Long,
    val prevLastReviewedAt: Long,
    val newIntervalDays: Int,
) {
    fun toPreviousState() = SchedulingState(
        state = prevState,
        learningStep = prevLearningStep,
        intervalDays = prevIntervalDays,
        easeFactor = prevEaseFactor,
        reps = prevReps,
        lapses = prevLapses,
        dueAt = prevDueAt,
        lastReviewedAt = prevLastReviewedAt,
    )

    companion object {
        fun of(
            cardId: Long,
            rating: Int,
            elapsedMs: Long,
            before: SchedulingState,
            after: SchedulingState,
            now: Long,
        ) = ReviewLog(
            cardId = cardId,
            reviewedAt = now,
            rating = rating,
            elapsedMs = elapsedMs,
            prevState = before.state,
            prevLearningStep = before.learningStep,
            prevIntervalDays = before.intervalDays,
            prevEaseFactor = before.easeFactor,
            prevReps = before.reps,
            prevLapses = before.lapses,
            prevDueAt = before.dueAt,
            prevLastReviewedAt = before.lastReviewedAt,
            newIntervalDays = after.intervalDays,
        )
    }
}
