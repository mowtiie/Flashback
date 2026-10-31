package com.mowtiie.flashback

import android.content.Context
import com.mowtiie.flashback.data.FlashbackDatabase
import com.mowtiie.flashback.repository.FlashbackRepository
import com.mowtiie.flashback.ui.AppMessenger
import com.mowtiie.flashback.ui.study.StudyPreferences

/** Hand-wired dependencies, created once by [FlashbackApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val dispatchers: AppDispatchers = AppDispatchers.default()

    val database: FlashbackDatabase by lazy { FlashbackDatabase.getInstance(appContext) }

    val repository: FlashbackRepository by lazy { FlashbackRepository(database, dispatchers) }

    val messenger = AppMessenger()

    val studyPreferences: StudyPreferences by lazy { StudyPreferences(appContext) }
}
