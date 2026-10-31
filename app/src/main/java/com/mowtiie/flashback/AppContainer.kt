package com.mowtiie.flashback

import android.content.Context
import com.mowtiie.flashback.data.FlashbackDatabase
import com.mowtiie.flashback.notifications.ReminderPreferences
import com.mowtiie.flashback.notifications.ReminderScheduler
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.theme.ThemePreferences
import com.mowtiie.flashback.ui.AppMessenger
import com.mowtiie.flashback.ui.study.StudyPreferences

/** Hand-wired dependencies, created once by [FlashbackApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val dispatchers: AppDispatchers = AppDispatchers.default()

    val database: FlashbackDatabase by lazy { FlashbackDatabase.getInstance(appContext) }

    val repository: FlashbackRepository by lazy { FlashbackRepository(database, dispatchers) }

    val messenger = AppMessenger()

    private val preferenceStore: PreferenceStore by lazy { SharedPreferenceStore(appContext) }

    val studyPreferences: StudyPreferences by lazy { StudyPreferences(preferenceStore) }

    val themePreferences: ThemePreferences by lazy { ThemePreferences(preferenceStore) }

    val reminderPreferences: ReminderPreferences by lazy { ReminderPreferences(preferenceStore) }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext, reminderPreferences)
    }
}
