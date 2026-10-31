package com.mowtiie.flashback.util

/** Consecutive study days ending at today; Android-free for JVM tests. */
object StreakCalculator {

    fun compute(studyDaysDesc: List<String>?, today: String?): Int {
        if (studyDaysDesc.isNullOrEmpty() || today == null) {
            return 0
        }

        val days = studyDaysDesc.mapNotNull { toEpochDay(it) }.toHashSet()
        val todayEpoch = toEpochDay(today) ?: return 0

        // Still alive today until the user studies: count from yesterday if today has no reviews.
        var cursor = when {
            todayEpoch in days -> todayEpoch
            todayEpoch - 1 in days -> todayEpoch - 1
            else -> return 0
        }

        var streak = 0
        while (cursor in days) {
            streak++
            cursor--
        }
        return streak
    }

    internal fun toEpochDay(date: String?): Long? {
        if (date == null || date.length < 10) {
            return null
        }
        return try {
            val year = date.substring(0, 4).toInt()
            val month = date.substring(5, 7).toInt()
            val day = date.substring(8, 10).toInt()
            epochDay(year, month, day)
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Howard Hinnant's days-from-civil: exact integer date arithmetic. */
    internal fun epochDay(year: Int, month: Int, day: Int): Long {
        val y = year - if (month <= 2) 1 else 0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = (y - era * 400).toLong()
        val doy = (153L * (month + (if (month > 2) -3 else 9)) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097L + doe - 719468
    }
}
