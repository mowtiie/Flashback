package com.mowtiie.flashback.generator

import java.io.InputStream

/** Pure checks for a chosen PDF, so they run without Android. */
object PdfInput {
    private const val CHUNK = 64 * 1024

    /** Reads at most [GeneratorLimits.MAX_PDF_BYTES]; a bigger stream is refused, never held whole. */
    @Throws(GeneratorException::class)
    fun readLimited(input: InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(CHUNK)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > GeneratorLimits.MAX_PDF_BYTES) throw tooLarge()
        }
        return out.toByteArray()
    }

    @Throws(GeneratorException::class)
    fun validated(name: String, bytes: ByteArray): GeneratorInput.Pdf {
        if (bytes.size > GeneratorLimits.MAX_PDF_BYTES) throw tooLarge()
        if (!looksLikePdf(bytes)) throw GeneratorException("That file does not look like a PDF.")
        return GeneratorInput.Pdf(name, bytes)
    }

    private fun tooLarge() = GeneratorException(
        "That PDF is larger than ${GeneratorLimits.MAX_PDF_BYTES / (1024 * 1024)} MB. Use a smaller file.",
    )
}
