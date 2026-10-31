package com.mowtiie.flashback.ui.study

import java.util.Random

object ChoiceBuilder {

    const val MAX_OPTIONS = 4
    internal const val MAX_LENGTH = 120

    class Choices internal constructor(
        val options: List<String>,
        val correctIndex: Int,
    )

    /** Returns null when the card can't make a fair question, so the caller falls back to flip. */
    fun build(correct: String?, candidates: List<String?>, random: Random): Choices? {
        if (correct == null || AnswerChecker.normalize(correct).isEmpty() ||
            correct.length > MAX_LENGTH
        ) {
            return null
        }

        val seen = hashSetOf(AnswerChecker.normalize(correct))

        val distractors = mutableListOf<String>()
        for (candidate in candidates) {
            if (candidate == null || candidate.length > MAX_LENGTH) {
                continue
            }
            val key = AnswerChecker.normalize(candidate)
            if (key.isNotEmpty() && seen.add(key)) {
                distractors.add(candidate)
            }
        }
        if (distractors.isEmpty()) {
            return null
        }

        distractors.shuffle(random)
        val options = distractors.subList(0, minOf(MAX_OPTIONS - 1, distractors.size)).toMutableList()
        options.add(correct)
        options.shuffle(random)
        return Choices(options, options.indexOf(correct))
    }
}
