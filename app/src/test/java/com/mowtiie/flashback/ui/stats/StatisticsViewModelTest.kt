package com.mowtiie.flashback.ui.stats

import com.mowtiie.flashback.data.model.CardStateBreakdown
import com.mowtiie.flashback.data.model.DailyCount
import com.mowtiie.flashback.repository.StatisticsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private class CountingSource : StatisticsSource {
        var total = 0

        override suspend fun reviewsSince(since: Long) = 0

        override suspend fun correctSince(since: Long) = 0

        override suspend fun timeSpentSince(since: Long) = 0L

        override suspend fun totalReviews() = total

        override suspend fun recentStudyDays(since: Long) = emptyList<String>()

        override suspend fun dailyCountsSince(since: Long) = emptyList<DailyCount>()

        override suspend fun cardStateBreakdown() = CardStateBreakdown(0, 0, 0, 0)
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(source: StatisticsSource) =
        StatisticsViewModel(source, clock = { 1_700_000_000_000L }, zone = { TimeZone.getTimeZone("UTC") })

    @Test
    fun loadsOnCreation() {
        val state = viewModel(CountingSource().apply { total = 3 }).uiState.value
        assertTrue(state.loaded)
        assertEquals(3, state.data.totalReviews)
    }

    @Test
    fun emptyHistoryIsLoadedButHasNoActivity() {
        val state = viewModel(CountingSource()).uiState.value
        assertTrue(state.loaded)
        assertFalse(state.data.hasAnyActivity)
    }

    @Test
    fun refreshPicksUpNewReviews() {
        val source = CountingSource()
        val viewModel = viewModel(source)
        source.total = 5
        viewModel.refresh()
        assertEquals(5, viewModel.uiState.value.data.totalReviews)
    }
}
