package com.mowtiie.flashback.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mowtiie.flashback.notifications.ReminderPreferences
import com.mowtiie.flashback.notifications.ReminderScheduling
import com.mowtiie.flashback.theme.ContrastLevel
import com.mowtiie.flashback.theme.ThemeMode
import com.mowtiie.flashback.theme.ThemePreferences
import com.mowtiie.flashback.ui.study.AnswerMode
import com.mowtiie.flashback.ui.study.StudyPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class SettingsUiState(
    val answerMode: AnswerMode = AnswerMode.Flip,
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColorAvailable: Boolean = false,
    val dynamicColor: Boolean = false,
    val contrast: ContrastLevel = ContrastLevel.Standard,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
) {
    /** Dynamic colour brings its own palette, so the contrast choice is ignored while it is on. */
    val contrastEnabled: Boolean get() = !dynamicColor
}

enum class SettingsEvent { NotificationsDenied }

/** Study, appearance and reminder preferences. Import and export live in [DataTransferViewModel]. */
class SettingsViewModel(
    private val studyPrefs: StudyPreferences,
    private val themePrefs: ThemePreferences,
    private val reminderPrefs: ReminderPreferences,
    private val scheduler: ReminderScheduling,
    private val ioDispatcher: CoroutineDispatcher,
    dynamicColorAvailable: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        themePrefs.settings.value.let { theme ->
            SettingsUiState(
                answerMode = studyPrefs.answerMode,
                themeMode = theme.mode,
                dynamicColorAvailable = dynamicColorAvailable,
                dynamicColor = dynamicColorAvailable && theme.dynamicColor,
                contrast = theme.contrast,
                reminderEnabled = reminderPrefs.enabled,
                reminderHour = reminderPrefs.hour,
                reminderMinute = reminderPrefs.minute,
            )
        },
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    fun setAnswerMode(mode: AnswerMode) {
        studyPrefs.answerMode = mode
        _uiState.update { it.copy(answerMode = mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        themePrefs.setMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        if (!_uiState.value.dynamicColorAvailable) return
        themePrefs.setDynamicColor(enabled)
        _uiState.update { it.copy(dynamicColor = enabled) }
    }

    fun setContrast(contrast: ContrastLevel) {
        themePrefs.setContrast(contrast)
        _uiState.update { it.copy(contrast = contrast) }
    }

    /** The screen asks for the notification permission first when turning this on. */
    fun setReminderEnabled(enabled: Boolean) {
        reminderPrefs.enabled = enabled
        _uiState.update { it.copy(reminderEnabled = enabled) }
        reschedule()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        reminderPrefs.setTime(hour, minute)
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
        if (_uiState.value.reminderEnabled) reschedule()
    }

    fun onNotificationsDenied() {
        events.trySend(SettingsEvent.NotificationsDenied)
    }

    private fun reschedule() {
        viewModelScope.launch { withContext(ioDispatcher + NonCancellable) { scheduler.reschedule() } }
    }
}
