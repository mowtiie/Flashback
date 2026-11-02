package com.mowtiie.flashback

import android.content.Context
import com.mowtiie.flashback.data.FlashbackDatabase
import com.mowtiie.flashback.generator.AnthropicCardGenerator
import com.mowtiie.flashback.generator.CardGenerator
import com.mowtiie.flashback.generator.GeminiCardGenerator
import com.mowtiie.flashback.generator.GenerationSession
import com.mowtiie.flashback.generator.GeneratorPreferences
import com.mowtiie.flashback.generator.GeneratorProvider
import com.mowtiie.flashback.generator.KeystoreSecretCipher
import com.mowtiie.flashback.generator.LiteRtTextModel
import com.mowtiie.flashback.generator.LocalCardGenerator
import com.mowtiie.flashback.generator.LocalModelStore
import com.mowtiie.flashback.generator.SelectedCardGenerator
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

    val generatorPreferences: GeneratorPreferences by lazy {
        GeneratorPreferences(
            SharedPreferenceStore(appContext, SharedPreferenceStore.SECRETS_FILE),
            KeystoreSecretCipher(),
        )
    }

    val generationSession = GenerationSession()

    val localModelStore: LocalModelStore by lazy { LocalModelStore(appContext, dispatchers.io) }

    private val localTextModelLazy = lazy { LiteRtTextModel(appContext, localModelStore, dispatchers.io) }
    private val localTextModel: LiteRtTextModel by localTextModelLazy

    /** The model takes about as much memory as its file, so it is let go when the generator is left. */
    fun releaseLocalModel() {
        if (localTextModelLazy.isInitialized()) localTextModel.releaseWhenIdle()
    }

    val cardGenerator: CardGenerator by lazy {
        SelectedCardGenerator(
            generatorPreferences,
            mapOf(
                GeneratorProvider.Anthropic to AnthropicCardGenerator(
                    apiKey = { generatorPreferences.apiKey(GeneratorProvider.Anthropic) },
                    ioDispatcher = dispatchers.io,
                ),
                GeneratorProvider.Gemini to GeminiCardGenerator(
                    apiKey = { generatorPreferences.apiKey(GeneratorProvider.Gemini) },
                    ioDispatcher = dispatchers.io,
                ),
                GeneratorProvider.OnDevice to LocalCardGenerator(localTextModel),
            ),
        )
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(appContext, reminderPreferences)
    }
}
