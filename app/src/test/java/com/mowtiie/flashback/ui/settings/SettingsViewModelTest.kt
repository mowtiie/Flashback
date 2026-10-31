package com.mowtiie.flashback.ui.settings

import com.mowtiie.flashback.FakePreferenceStore
import com.mowtiie.flashback.notifications.ReminderPreferences
import com.mowtiie.flashback.notifications.ReminderScheduling
import com.mowtiie.flashback.theme.ContrastLevel
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.theme.ThemePreferences
import com.mowtiie.flashback.ui.study.AnswerMode
import com.mowtiie.flashback.ui.study.StudyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val store = FakePreferenceStore()
    private var reschedules = 0
    private val scheduler = ReminderScheduling { reschedules++ }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(dynamicColorAvailable: Boolean = true) = SettingsViewModel(
        studyPrefs = StudyPreferences(store),
        themePrefs = ThemePreferences(store),
        reminderPrefs = ReminderPreferences(store),
        scheduler = scheduler,
        archive = FakeArchive(),
        ioDispatcher = UnconfinedTestDispatcher(),
        dynamicColorAvailable = dynamicColorAvailable,
    )

    @Test
    fun startsFromSavedPreferences() {
        store.values["study_answer_mode"] = 1
        store.values["theme_mode"] = 2
        store.values["reminder_enabled"] = true
        store.values["reminder_hour"] = 6
        store.values["reminder_minute"] = 15

        val state = viewModel().uiState.value
        assertEquals(AnswerMode.Type, state.answerMode)
        assertEquals(ThemeMode.Dark, state.themeMode)
        assertTrue(state.reminderEnabled)
        assertEquals(6, state.reminderHour)
        assertEquals(15, state.reminderMinute)
    }

    @Test
    fun answerModeIsSavedForTheStudyScreen() {
        viewModel().setAnswerMode(AnswerMode.Choice)
        assertEquals(AnswerMode.Choice, StudyPreferences(store).answerMode)
    }

    @Test
    fun themeChangesReachThePublishedThemeSettings() {
        val themePrefs = ThemePreferences(store)
        val viewModel = SettingsViewModel(
            StudyPreferences(store), themePrefs, ReminderPreferences(store), scheduler,
            FakeArchive(), UnconfinedTestDispatcher(), true,
        )
        viewModel.setThemeMode(ThemeMode.Light)
        viewModel.setContrast(ContrastLevel.Medium)
        assertEquals(ThemeMode.Light, themePrefs.settings.value.mode)
        assertEquals(ContrastLevel.Medium, themePrefs.settings.value.contrast)
    }

    @Test
    fun contrastIsDisabledWhileDynamicColorIsOn() {
        val viewModel = viewModel()
        viewModel.setDynamicColor(true)
        assertFalse(viewModel.uiState.value.contrastEnabled)
        viewModel.setDynamicColor(false)
        assertTrue(viewModel.uiState.value.contrastEnabled)
    }

    @Test
    fun savedDynamicColorIsIgnoredWhereUnavailable() {
        val state = viewModel(dynamicColorAvailable = false).uiState.value
        assertFalse(state.dynamicColor)
        assertTrue(state.contrastEnabled)
    }

    @Test
    fun dynamicColorCannotBeTurnedOnWhereUnavailable() {
        val viewModel = viewModel(dynamicColorAvailable = false)
        viewModel.setDynamicColor(true)
        assertFalse(viewModel.uiState.value.dynamicColor)
        assertEquals(null, store.values["theme_dynamic_color"])
    }

    @Test
    fun enablingAndDisablingTheReminderReschedules() {
        val viewModel = viewModel()
        viewModel.setReminderEnabled(true)
        assertTrue(ReminderPreferences(store).enabled)
        assertEquals(1, reschedules)

        viewModel.setReminderEnabled(false)
        assertFalse(ReminderPreferences(store).enabled)
        assertEquals(2, reschedules)
    }

    @Test
    fun changingTheTimeReschedulesOnlyWhileEnabled() {
        val viewModel = viewModel()
        viewModel.setReminderTime(8, 5)
        assertEquals(0, reschedules)
        assertEquals(8, ReminderPreferences(store).hour)

        viewModel.setReminderEnabled(true)
        viewModel.setReminderTime(9, 10)
        assertEquals(2, reschedules)
        assertEquals(9, viewModel.uiState.value.reminderHour)
        assertEquals(10, viewModel.uiState.value.reminderMinute)
    }

    @Test
    fun deniedPermissionLeavesTheReminderOffAndTellsTheScreen() = runTest {
        val viewModel = viewModel()
        viewModel.onNotificationsDenied()
        assertEquals(SettingsEvent.NotificationsDenied, viewModel.eventFlow.first())
        assertFalse(viewModel.uiState.value.reminderEnabled)
        assertEquals(0, reschedules)
    }
}
