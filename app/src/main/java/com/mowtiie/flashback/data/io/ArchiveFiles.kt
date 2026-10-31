package com.mowtiie.flashback.data.io

import android.content.ContentResolver
import android.net.Uri
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/** Reads and writes archive text via the Storage Access Framework, so no storage permission is needed. */
object ArchiveFiles {
    private const val MAX_BYTES = 16L * 1024 * 1024

    /** Blocking; call off the main thread. */
    @Throws(IOException::class)
    fun write(resolver: ContentResolver, uri: Uri, content: String) {
        val out = resolver.openOutputStream(uri, "wt")
            ?: throw IOException("Could not open the chosen location for writing.")
        out.use { it.write(content.toByteArray(Charsets.UTF_8)) }
    }

    /** Blocking; call off the main thread. */
    @Throws(IOException::class)
    fun read(resolver: ContentResolver, uri: Uri): String {
        val input = resolver.openInputStream(uri)
            ?: throw IOException("Could not open the chosen file.")
        // The limit is enforced on bytes read, so a large file is never held twice.
        return input.use { LimitedInputStream(it, MAX_BYTES).readBytes().toString(Charsets.UTF_8) }
    }

    private class LimitedInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
        private var total = 0L

        override fun read(): Int = super.read().also { if (it != -1) count(1) }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            super.read(buffer, offset, length).also { if (it > 0) count(it.toLong()) }

        private fun count(n: Long) {
            total += n
            if (total > limit) throw IOException("This file is too large to be a Flashback export.")
        }
    }
}

/** `flashback-<name>.json` with anything outside letters, digits, `-` and `_` replaced. */
fun deckExportFileName(prefix: String, deckName: String?): String =
    prefix + (deckName ?: "deck").replace(Regex("[^a-zA-Z0-9-_]"), "_") + ".json"
