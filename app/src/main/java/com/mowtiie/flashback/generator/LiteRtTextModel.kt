package com.mowtiie.flashback.generator

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Runs the model file kept by [LocalModelStore] with LiteRT-LM on the CPU. One request at a time. */
class LiteRtTextModel(
    private val context: Context,
    private val store: LocalModelStore,
    private val dispatcher: CoroutineDispatcher,
) : TextModel {
    private val lock = Mutex()
    private val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private var engine: Engine? = null
    private var loadedStamp: String? = null

    override suspend fun complete(system: String, user: String, shouldStop: (String) -> Boolean): String = lock.withLock {
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
                    maxOutputToken = MAX_OUTPUT_TOKENS,
                )
                engine.createConversation(config).use { conversation -> stream(conversation, user, shouldStop) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                release()
                throw GeneratorException("The on-device model stopped before it finished. Try less text at a time.", e)
            }
        }
    }

    /**
     * Uses the callback API, because the library's Flow needs a newer kotlinx-coroutines than the app has.
     * Waits for the native run to end before returning, so the conversation is never closed mid-run.
     */
    private suspend fun stream(conversation: Conversation, user: String, shouldStop: (String) -> Boolean): String {
        val out = StringBuilder()
        val finished = CompletableDeferred<Unit>()
        val started = System.nanoTime()
        var chunks = 0
        var stopped = false

        conversation.sendMessageAsync(
            user,
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    if (stopped) return
                    val text = message.toString()
                    chunks++
                    out.append(text)
                    if (debuggable && (chunks == 1 || chunks % 20 == 0)) {
                        val ms = (System.nanoTime() - started) / 1_000_000
                        Log.d(TAG, "chunk $chunks at $ms ms, ${out.length} chars, tail: ${out.takeLast(100).toString().replace("\n", " ")}")
                    }
                    if (shouldStop(out.toString())) {
                        stopped = true
                        runCatching { conversation.cancelProcess() }
                    }
                }

                override fun onDone() {
                    finished.complete(Unit)
                }

                override fun onError(throwable: Throwable) {
                    if (stopped) finished.complete(Unit) else finished.completeExceptionally(throwable)
                }
            },
        )

        try {
            finished.await()
        } catch (e: CancellationException) {
            runCatching { conversation.cancelProcess() }
            withContext(NonCancellable) { withTimeoutOrNull(CANCEL_WAIT_MS) { finished.await() } }
            throw e
        }
        Log.d(TAG, "reply: ${out.length} chars, $chunks chunks, ${(System.nanoTime() - started) / 1_000_000} ms")
        // An early stop can leave a half-written line.
        return (if (stopped) out.toString().substringBeforeLast("\n", "") else out.toString()).trim()
    }

    private fun loaded(): Engine {
        val file = store.file().takeIf { it.exists() }
            ?: throw GeneratorException("Choose a model file in Settings first.")
        // The model is picked in Settings while this object lives on, so a different file means a reload.
        val stamp = "${file.length()}-${file.lastModified()}"
        engine?.let { if (stamp == loadedStamp) return it else release() }

        val cache = File(context.cacheDir, "litertlm").apply {
            listFiles()?.forEach { it.deleteRecursively() }
            mkdirs()
        }
        return Engine(
            EngineConfig(
                modelPath = file.path,
                backend = Backend.CPU(),
                cacheDir = cache.path,
            ),
        ).also {
            it.initialize()
            engine = it
            loadedStamp = stamp
        }
    }

    private companion object {
        const val MAX_OUTPUT_TOKENS = 2048
        const val TAG = "LiteRtTextModel"
        const val CANCEL_WAIT_MS = 3_000L
    }

    /** Frees the model's memory. It loads again on the next request. */
    fun release() {
        runCatching { engine?.close() }
        engine = null
        loadedStamp = null
    }
}
