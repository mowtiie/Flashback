package com.mowtiie.flashback.scheduler

enum class Rating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    companion object {
        fun fromValue(value: Int): Rating =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown rating value: $value")
    }
}
