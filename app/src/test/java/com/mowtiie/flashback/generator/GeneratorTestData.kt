package com.mowtiie.flashback.generator

fun textRequest(text: String, count: CardCount = CardCount.Normal) =
    GeneratorRequest(GeneratorInput.Text(text), count)

fun pdfRequest(bytes: ByteArray, name: String = "handout.pdf", count: CardCount = CardCount.Normal) =
    GeneratorRequest(GeneratorInput.Pdf(name, bytes), count)

/** The smallest bytes that pass the PDF check. */
val FAKE_PDF = "%PDF-1.4\nfake".toByteArray()
