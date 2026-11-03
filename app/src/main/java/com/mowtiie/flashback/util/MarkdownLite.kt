package com.mowtiie.flashback.util

data class MdSpan(val text: String, val bold: Boolean = false)

sealed interface MdBlock {
    val spans: List<MdSpan>

    data class Heading(val level: Int, override val spans: List<MdSpan>) : MdBlock
    data class Paragraph(override val spans: List<MdSpan>) : MdBlock
    data class Bullet(override val spans: List<MdSpan>) : MdBlock
}

/** Just enough Markdown for the privacy policy: headings, bullets, paragraphs, **bold** and `code`. */
object MarkdownLite {

    fun parse(text: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val paragraph = StringBuilder()

        fun flush() {
            if (paragraph.isNotEmpty()) blocks += MdBlock.Paragraph(spans(paragraph.toString()))
            paragraph.clear()
        }

        for (raw in text.lines()) {
            val line = raw.trim()
            val level = line.takeWhile { it == '#' }.length
            when {
                line.isEmpty() -> flush()
                level in 1..6 && line.getOrNull(level) == ' ' -> {
                    flush()
                    blocks += MdBlock.Heading(level, spans(line.drop(level + 1)))
                }
                line.startsWith("- ") -> {
                    flush()
                    blocks += MdBlock.Bullet(spans(line.drop(2)))
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line)
                }
            }
        }
        flush()
        return blocks
    }

    private fun spans(line: String): List<MdSpan> {
        val clean = line.replace("`", "")
        return clean.split("**")
            .mapIndexedNotNull { index, part -> if (part.isEmpty()) null else MdSpan(part, bold = index % 2 == 1) }
    }
}
