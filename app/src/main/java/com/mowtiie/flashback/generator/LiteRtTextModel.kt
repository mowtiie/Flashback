package com.mowtiie.flashback.generator

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Runs the model file kept by [LocalModelStore] with LiteRT-LM on the CPU. One request at a time. */
class LiteRtTextModel(
    private val context: Context,
    private val store: LocalModelStore,
    private val dispatcher: CoroutineDispatcher,
) : TextModel {
    private val lock = Mutex()
    private var engine: Engine? = null

    override suspend fun complete(system: String, user: String): String = lock.withLock {
        withContext(dispatcher) {
            val engine = try {
                loaded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                release()
                throw GeneratorException("The on-device model could not start. The file may not be a supported model, or the phone is low on memory.", e)
            }
            try {
                val config = ConversationConfig(
                    systemInstruction = Contents.of(system),
                    samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.3),
                )
                engine.createConversation(config).use { conversation ->
                    conversation.sendMessageAsync(user).toList().joinToString("") { it.toString() }.trim()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                release()
                throw GeneratorException("The on-device model stopped before it finished. Try less text at a time.", e)
            }
        }
    }

    private fun loaded(): Engine {
        engine?.let { return it }
        val path = store.file().takeIf { it.exists() }?.path
            ?: throw GeneratorException("Choose a model file in Settings first.")
        return Engine(
            EngineConfig(
                modelPath = path,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.path,
            ),
        ).also {
            it.initialize()
            engine = it
        }
    }

    /** Frees the model's memory. It loads again on the next request. */
    fun release() {
        runCatching { engine?.close() }
        engine = null
    }
}
