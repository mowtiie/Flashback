package com.mowtiie.flashback

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.mowtiie.flashback.generator.GenerationService
import com.mowtiie.flashback.notifications.ReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FlashbackApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Off the main thread: reading prefs must not delay launch.
        CoroutineScope(SupervisorJob() + container.dispatchers.io).launch {
            ReminderNotifier.ensureChannel(this@FlashbackApp)
            container.reminderScheduler.ensureScheduled()
        }

        keepGenerationAlive()
    }

    /** A running generation starts the foreground service, which stops itself when the run ends. */
    private fun keepGenerationAlive() {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            container.generationSession.progress
                .map { it != null }
                .distinctUntilChanged()
                .collect { running ->
                    // The run starts from a tap, so the app is in front; if the system refuses anyway, the run still works.
                    if (running) {
                        runCatching {
                            ContextCompat.startForegroundService(
                                this@FlashbackApp,
                                Intent(this@FlashbackApp, GenerationService::class.java),
                            )
                        }
                    }
                }
        }
    }
}
