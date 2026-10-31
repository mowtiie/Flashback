package com.mowtiie.flashback.ui.generator

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.mowtiie.flashback.generator.GeneratorException
import com.mowtiie.flashback.generator.GeneratorInput
import com.mowtiie.flashback.generator.PdfInput
import java.io.IOException

/** Reads a PDF the user picked with the Storage Access Framework, so no storage permission is needed. */
object PdfFiles {
    /** Blocking; call off the main thread. */
    @Throws(GeneratorException::class)
    fun read(resolver: ContentResolver, uri: Uri): GeneratorInput.Pdf {
        val unreadable = "The PDF could not be read."
        val bytes = try {
            val input = resolver.openInputStream(uri) ?: throw GeneratorException(unreadable)
            input.use { PdfInput.readLimited(it) }
        } catch (e: IOException) {
            throw GeneratorException(unreadable, e)
        }
        return PdfInput.validated(displayName(resolver, uri) ?: "document.pdf", bytes)
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: RuntimeException) {
        null
    }
}
