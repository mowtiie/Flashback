package com.mowtiie.flashback.generator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

interface LocalModels {
    /** Copies the picked file in and returns its display name. */
    @Throws(IOException::class)
    suspend fun install(uri: Uri): String

    fun remove()
}

/** The one model file the student picked, copied into private storage. Nothing is downloaded. */
class LocalModelStore(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : LocalModels {

    fun file(): File = File(File(context.filesDir, "models"), "model.litertlm")

    fun isInstalled(): Boolean = file().exists()

    fun sizeBytes(): Long = file().takeIf { it.exists() }?.length() ?: 0L

    /** Returns the file's display name. Copies to a temporary file first, so a failed copy keeps the old model. */
    @Throws(IOException::class)
    override suspend fun install(uri: Uri): String = withContext(ioDispatcher) {
        val target = file()
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "model.partial")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Could not open the file.")
            input.use { source -> partial.outputStream().use { source.copyTo(it) } }
            if (!target.delete() && target.exists()) throw IOException("Could not replace the old model.")
            if (!partial.renameTo(target)) throw IOException("Could not save the model.")
        } finally {
            partial.delete()
        }
        displayName(uri) ?: "model.litertlm"
    }

    override fun remove() {
        file().delete()
    }

    private fun displayName(uri: Uri): String? = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
}
