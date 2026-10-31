package com.mowtiie.flashback.repository

import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.data.model.DailyCount

/** The reads the statistics screen needs, so it can be tested without Room. */
interface StatisticsSource {
    suspend fun reviewsSince(since: Long): Int

    suspend fun correctSince(since: Long): Int

    suspend fun timeSpentSince(since: Long): Long

    suspend fun totalReviews(): Int

    suspend fun recentStudyDays(since: Long): List<String>

    suspend fun dailyCountsSince(since: Long): List<DailyCount>

    suspend fun cardStateBreakdown(): CardStateBreakdown
}
