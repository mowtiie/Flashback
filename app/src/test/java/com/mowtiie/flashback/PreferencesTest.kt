package com.mowtiie.flashback

import com.mowtiie.flashback.notifications.ReminderPreferences
import com.mowtiie.flashback.theme.ContrastLevel
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.theme.ThemePreferences
import com.mowtiie.flashback.theme.ThemeSettings
import com.mowtiie.flashback.ui.study.AnswerMode
import com.mowtiie.flashback.ui.study.StudyPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** The keys and values are the Java app's, so existing installs keep their settings. */
class PreferencesTest {

    private val store = FakePreferenceStore()

    @Test
    fun studyDefaultsToFlip() {
        assertEquals(AnswerMode.Flip, StudyPreferences(store).answerMode)
    }

    @Test
    fun studyModeIsStoredUnderTheJavaKeyAndValue() {
        StudyPreferences(store).answerMode = AnswerMode.Choice
        assertEquals(2, store.values["study_answer_mode"])
        assertEquals(AnswerMode.Choice, StudyPreferences(store).answerMode)
    }

    @Test
    fun unknownStudyValueFallsBackToFlip() {
        store.values["study_answer_mode"] = 9
        assertEquals(AnswerMode.Flip, StudyPreferences(store).answerMode)
    }

    @Test
    fun reminderDefaultsToOffAtEightPm() {
        val prefs = ReminderPreferences(store)
        assertFalse(prefs.enabled)
        assertEquals(20, prefs.hour)
        assertEquals(0, prefs.minute)
    }

    @Test
    fun reminderRoundTripsUnderTheJavaKeys() {
        val prefs = ReminderPreferences(store)
        prefs.enabled = true
        prefs.setTime(7, 45)
        assertEquals(true, store.values["reminder_enabled"])
        assertEquals(7, store.values["reminder_hour"])
        assertEquals(45, store.values["reminder_minute"])
        assertEquals(7, ReminderPreferences(store).hour)
    }

    @Test
    fun themeDefaultsToSystemDynamicStandard() {
        assertEquals(ThemeSettings(ThemeMode.System, true, ContrastLevel.Standard), ThemePreferences(store).settings.value)
    }

    @Test
    fun themeReadsJavaValues() {
        store.values["theme_mode"] = 2
        store.values["theme_contrast"] = 1
        store.values["theme_dynamic_color"] = false
        assertEquals(
            ThemeSettings(ThemeMode.Dark, false, ContrastLevel.Medium),
            ThemePreferences(store).settings.value,
        )
    }

    @Test
    fun themeSettersPersistAndPublish() {
        val prefs = ThemePreferences(store)
        prefs.setMode(ThemeMode.Light)
        prefs.setContrast(ContrastLevel.High)
        prefs.setDynamicColor(false)

        assertEquals(ThemeSettings(ThemeMode.Light, false, ContrastLevel.High), prefs.settings.value)
        assertEquals(1, store.values["theme_mode"])
        assertEquals(2, store.values["theme_contrast"])
        assertEquals(false, store.values["theme_dynamic_color"])
    }

    @Test
    fun outOfRangeThemeValuesFallBackToDefaults() {
        store.values["theme_mode"] = 7
        store.values["theme_contrast"] = -1
        val settings = ThemePreferences(store).settings.value
        assertEquals(ThemeMode.System, settings.mode)
        assertEquals(ContrastLevel.Standard, settings.contrast)
    }
}
