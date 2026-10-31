package com.mowtiie.flashback.scheduler

data class SchedulingState(
    var state: Int = CardState.NEW,
    var learningStep: Int = 0,
    var intervalDays: Int = 0,
    var easeFactor: Double = 2.5,
    var reps: Int = 0,
    var lapses: Int = 0,
    var dueAt: Long = 0,
    var lastReviewedAt: Long = 0,
)
