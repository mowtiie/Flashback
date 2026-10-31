package com.mowtiie.flashback.ui.study

/** [value] is what the Java app stored, so an upgrade keeps the user's choice. */
enum class AnswerMode(val value: Int) {
    Flip(0),
    Type(1),
    Choice(2);

    companion object {
        fun fromValue(value: Int): AnswerMode = entries.firstOrNull { it.value == value } ?: Flip
    }
}
