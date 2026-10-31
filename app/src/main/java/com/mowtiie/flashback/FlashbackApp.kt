package com.mowtiie.flashback

import android.app.Application
import androidx.work.Configuration
import com.mowtiie.flashback.notifications.ReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlashbackApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Off the main thread: reading prefs and touching WorkManager must not delay launch.
        CoroutineScope(SupervisorJob() + container.dispatchers.io).launch {
            ReminderNotifier.ensureChannel(this@FlashbackApp)
            container.reminderScheduler.ensureScheduled()
        }
    }

    /** On-demand WorkManager init; the start-up provider is removed in the manifest. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
