package com.mowtiie.flashback.ui.study

import com.mowtiie.flashback.scheduler.Rating
import java.text.Normalizer
import java.util.Locale

object AnswerChecker {

    enum class Result {
        CORRECT,
        CLOSE,
        WRONG,
    }

    // Latin-style accents only: other scripts use combining marks as part of the letter.
    private val accents = Regex("[\\u0300-\\u036F]+")
    private val whitespace = Regex("\\s+")
    private val trailingPunctuation = Regex("[\\p{P}\\s]+$")

    fun check(typed: String?, expected: String?): Result {
        val a = normalize(typed)
        val b = normalize(expected)
        if (b.isEmpty() || a.isEmpty()) {
            return Result.WRONG
        }
        if (a == b) {
            return Result.CORRECT
        }
        return if (distance(a, b) <= allowedEdits(b.length)) Result.CLOSE else Result.WRONG
    }

    fun suggested(result: Result): Rating = when (result) {
        Result.CORRECT -> Rating.GOOD
        Result.CLOSE -> Rating.HARD
        Result.WRONG -> Rating.AGAIN
    }

    internal fun normalize(text: String?): String {
        if (text == null) {
            return ""
        }
        return Normalizer.normalize(Normalizer.normalize(text, Normalizer.Form.NFD).replace(accents, ""), Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .replace(whitespace, " ")
            .trim()
            .replace(trailingPunctuation, "")
    }

    private fun allowedEdits(length: Int): Int = when {
        length < 4 -> 0
        length <= 8 -> 1
        else -> 2
    }

    private fun distance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val swap = prev
            prev = curr
            curr = swap
        }
        return prev[b.length]
    }
}
