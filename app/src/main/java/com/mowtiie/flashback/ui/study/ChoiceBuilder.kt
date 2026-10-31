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
    fun build(
        correct: String?,
        candidates: List<String?>,
        random: Random,
        preferred: List<String> = emptyList(),
    ): Choices? {
        if (correct == null || AnswerChecker.normalize(correct).isEmpty() ||
            correct.length > MAX_LENGTH
        ) {
            return null
        }

        val seen = hashSetOf(AnswerChecker.normalize(correct))

        // Wrong options the author wrote come first; borrowed answers only fill what is left.
        val authored = unseen(preferred, seen)
        val borrowed = unseen(candidates, seen)
        if (authored.isEmpty() && borrowed.isEmpty()) {
            return null
        }

        borrowed.shuffle(random)
        val options = (authored + borrowed).take(MAX_OPTIONS - 1).toMutableList()
        options.add(correct)
        options.shuffle(random)
        return Choices(options, options.indexOf(correct))
    }

    private fun unseen(options: List<String?>, seen: MutableSet<String>): MutableList<String> {
        val result = mutableListOf<String>()
        for (option in options) {
            if (option == null || option.length > MAX_LENGTH) continue
            val key = AnswerChecker.normalize(option)
            if (key.isNotEmpty() && seen.add(key)) result.add(option)
        }
        return result
    }
}
