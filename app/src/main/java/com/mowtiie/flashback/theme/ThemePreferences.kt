package com.mowtiie.flashback.theme

import androidx.compose.runtime.Immutable
import com.mowtiie.flashback.PreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val value: Int) {
    System(0),
    Light(1),
    Dark(2),
    ;

    companion object {
        fun fromValue(value: Int): ThemeMode = entries.firstOrNull { it.value == value } ?: System
    }
}

@Immutable
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val contrast: ContrastLevel = ContrastLevel.Standard,
)

/** Observable, so a change recomposes the theme without recreating the activity. */
class ThemePreferences(private val store: PreferenceStore) {

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    fun setMode(mode: ThemeMode) {
        store.putInt(KEY_MODE, mode.value)
        _settings.value = _settings.value.copy(mode = mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        store.putBoolean(KEY_DYNAMIC, enabled)
        _settings.value = _settings.value.copy(dynamicColor = enabled)
    }

    fun setContrast(contrast: ContrastLevel) {
        store.putInt(KEY_CONTRAST, contrast.ordinal)
        _settings.value = _settings.value.copy(contrast = contrast)
    }

    private fun read() = ThemeSettings(
        mode = ThemeMode.fromValue(store.getInt(KEY_MODE, ThemeMode.System.value)),
        dynamicColor = store.getBoolean(KEY_DYNAMIC, true),
        contrast = ContrastLevel.entries.getOrElse(store.getInt(KEY_CONTRAST, 0)) { ContrastLevel.Standard },
    )

    private companion object {
        const val KEY_MODE = "theme_mode"
        const val KEY_CONTRAST = "theme_contrast"
        const val KEY_DYNAMIC = "theme_dynamic_color"
    }
}
