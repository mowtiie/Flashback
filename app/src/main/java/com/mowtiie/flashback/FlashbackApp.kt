package com.mowtiie.flashback

import android.app.Application
import androidx.work.Configuration

class FlashbackApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** On-demand WorkManager init; the start-up provider is removed in the manifest. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
