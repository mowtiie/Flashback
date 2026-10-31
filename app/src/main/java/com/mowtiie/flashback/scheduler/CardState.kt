package com.mowtiie.flashback.scheduler

object CardState {
    const val NEW = 0
    const val LEARNING = 1
    const val REVIEW = 2
    const val RELEARNING = 3

    fun isLearningLike(state: Int): Boolean =
        state == NEW || state == LEARNING || state == RELEARNING
}
