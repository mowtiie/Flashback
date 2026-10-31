package com.mowtiie.flashback.ui.settings

import androidx.compose.runtime.Composable
import com.mowtiie.flashback.theme.ContrastLevel
import com.mowtiie.flashback.theme.FlashbackTheme
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.ui.components.LightDarkPreviews
import com.mowtiie.flashback.ui.study.AnswerMode

@Composable
private fun Preview(state: SettingsUiState) {
    FlashbackTheme(dynamicColor = false) { SettingsScreen(state, SettingsActions()) }
}

@LightDarkPreviews
@Composable
private fun SettingsPreview() = Preview(SettingsUiState(dynamicColorAvailable = true))

@LightDarkPreviews
@Composable
private fun SettingsDynamicColorOnPreview() = Preview(
    SettingsUiState(dynamicColorAvailable = true, dynamicColor = true, themeMode = ThemeMode.Dark),
)

@LightDarkPreviews
@Composable
private fun SettingsRemindersOnPreview() = Preview(
    SettingsUiState(
        answerMode = AnswerMode.Type,
        contrast = ContrastLevel.High,
        reminderEnabled = true,
        reminderHour = 7,
        reminderMinute = 30,
    ),
)
