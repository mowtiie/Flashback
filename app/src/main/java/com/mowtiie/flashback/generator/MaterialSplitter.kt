package com.mowtiie.flashback.generator

/** Cuts long study material into sections that fit one request, without losing or reordering text. */
object MaterialSplitter {
    private val PARAGRAPH_BREAK = Regex("""\n\s*\n""")
    private val SENTENCE_BREAK = Regex("""(?<=[.!?])\s+""")
    private val WHITESPACE = Regex("""\s+""")

    /** Sections are at most [target] characters. Paragraphs stay whole when they fit. */
    fun split(text: String, target: Int): List<String> {
        require(target > 0) { "target must be positive" }
        val paragraphs = text.split(PARAGRAPH_BREAK).map { it.trim() }.filter { it.isNotEmpty() }
        val units = paragraphs.flatMap { if (it.length <= target) listOf(it) else splitLong(it, target) }
        return group(units, "\n\n", target)
    }

    private fun splitLong(paragraph: String, target: Int): List<String> {
        val sentences = paragraph.split(SENTENCE_BREAK).map { it.trim() }.filter { it.isNotEmpty() }
        val units = sentences.flatMap { if (it.length <= target) listOf(it) else splitWords(it, target) }
        return group(units, " ", target)
    }

    private fun splitWords(sentence: String, target: Int): List<String> {
        val words = sentence.split(WHITESPACE).filter { it.isNotEmpty() }
        val units = words.flatMap { if (it.length <= target) listOf(it) else it.chunked(target) }
        return group(units, " ", target)
    }

    /** Joins neighbours while the result still fits. */
    private fun group(units: List<String>, separator: String, target: Int): List<String> {
        val sections = mutableListOf<String>()
        val current = StringBuilder()
        for (unit in units) {
            if (current.isNotEmpty() && current.length + separator.length + unit.length > target) {
                sections += current.toString()
                current.clear()
            }
            if (current.isNotEmpty()) current.append(separator)
            current.append(unit)
        }
        if (current.isNotEmpty()) sections += current.toString()
        return sections
    }
}
