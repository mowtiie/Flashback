package com.mowtiie.flashback.ui.settings

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mowtiie.flashback.R
import com.mowtiie.flashback.ui.components.isShortWindow
import java.util.Calendar

@Composable
internal fun RemindersSection(state: SettingsUiState, actions: SettingsActions) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    SectionHeader(R.string.settings_reminders_header)
    SettingsCard {
        SwitchRow(
            title = stringResource(R.string.reminder_toggle_title),
            body = stringResource(R.string.reminder_toggle_body),
            checked = state.reminderEnabled,
            onCheckedChange = actions.onReminderEnabled,
        )
        RowDivider()
        ActionRow(
            title = stringResource(R.string.reminder_time_title),
            value = formatTime(context, state.reminderHour, state.reminderMinute),
            enabled = state.reminderEnabled,
            onClick = { picking = true },
        )
    }

    if (picking) {
        ReminderTimeDialog(
            hour = state.reminderHour,
            minute = state.reminderMinute,
            is24Hour = DateFormat.is24HourFormat(context),
            onConfirm = { hour, minute ->
                picking = false
                actions.onReminderTime(hour, minute)
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimeDialog(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    // The dial is too tall for a short window, so typing the time is offered instead.
    shortWindow: Boolean = isShortWindow(),
) {
    val pickerState = rememberTimePickerState(hour, minute, is24Hour)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_time_title)) },
        text = { if (shortWindow) TimeInput(pickerState) else TimePicker(pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun formatTime(context: Context, hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(cal.time)
}
