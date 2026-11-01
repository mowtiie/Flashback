package com.mowtiie.flashback.generator

import org.json.JSONArray
import org.json.JSONObject

/**
 * Small on-device models cannot hold a nested JSON shape, so they write "Q:" and "A:" lines instead.
 * This reads them, even while they are still being written.
 */
internal object PlainCards {
    class Card(val front: String, val back: String)

    class Parsed(val title: String?, val cards: List<Card>)

    private val marker = Regex("""^[\s>*#_\-•]*(?:\d+[.)]\s*)?[*_]*""")
    private val question = Regex("""^(?:q|question|front)\s*\d*\s*[*_]*\s*[:：]\s*[*_]*\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val answer = Regex("""^(?:a|answer|back)\s*[*_]*\s*[:：]\s*[*_]*\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val title = Regex("""^(?:title|deck)\s*[*_]*\s*[:：]\s*[*_]*\s*(.*)$""", RegexOption.IGNORE_CASE)

    /**
     * With [complete] false the text is still streaming, so the card being written is left out
     * until its line ends and another card starts.
     */
    fun parse(text: String, complete: Boolean): Parsed {
        val source = if (complete) text else text.substringBeforeLast('\n', "")
        var deckTitle: String? = null
        val cards = mutableListOf<Card>()
        var front: String? = null
        val back = StringBuilder()
        var hasBack = false

        fun finish() {
            val f = front
            if (f != null && hasBack && back.isNotBlank()) cards += Card(f.trim(), back.toString().trim())
            front = null
            back.setLength(0)
            hasBack = false
        }

        for (raw in source.lineSequence()) {
            val line = raw.replace(marker, "").trim().trimEnd('*', '_').trim()
            if (line.isEmpty() || line.startsWith("```")) continue
            title.matchEntire(line)?.let { if (deckTitle == null) deckTitle = it.groupValues[1].trim(); continue }
            question.matchEntire(line)?.let {
                finish()
                front = it.groupValues[1]
                continue
            }
            answer.matchEntire(line)?.let {
                if (front != null && !hasBack) {
                    back.append(it.groupValues[1])
                    hasBack = true
                }
                continue
            }
            if (front != null && hasBack) back.append(' ').append(line)
        }
        // The last card is only trusted once the text is complete.
        if (complete) finish()
        return Parsed(deckTitle?.takeIf { it.isNotBlank() }, cards)
    }

    /** True when the model has started to repeat itself, or has made enough cards. */
    fun shouldStop(text: String, maxCards: Int): Boolean {
        if (!text.contains('\n')) return false
        val cards = parse(text, complete = false).cards
        if (cards.size >= maxCards) return true
        val fronts = cards.map { it.front.lowercase().trim() }
        if (fronts.size != fronts.distinct().size) return true
        return cards.size - withoutRepeats(cards).size >= REPEATS_BEFORE_STOP
    }

    private const val REPEATS_BEFORE_STOP = 2

    private val wordPattern = Regex("""[\p{L}\p{N}]+""")
    private val stopWords = setOf(
        "what", "is", "are", "the", "a", "an", "of", "in", "to", "and", "or", "for", "does", "do", "did",
        "which", "who", "how", "by", "as", "on", "at", "with", "that", "this", "its", "it", "be", "can",
        "when", "into", "from", "was", "were", "has", "have",
    )

    private fun contentWords(text: String): Set<String> =
        wordPattern.findAll(text.lowercase()).map { it.value }.filter { it !in stopWords }.toSet()

    private fun plainBack(text: String): String =
        text.lowercase().replace(Regex("""\(.*?\)"""), "").filter { it.isLetterOrDigit() || it == ' ' }.trim()

    /**
     * A reworded question the model has already asked: its key words sit inside an earlier question,
     * or the answer is the same and the questions share most of their key words.
     * Questions that differ by a word, like "genotype" and "phenotype", are kept.
     */
    private fun repeats(card: Card, earlier: Card): Boolean =
        isRepeat(card.front, card.back, earlier.front, earlier.back)

    internal fun isRepeat(front: String, back: String, earlierFront: String, earlierBack: String): Boolean {
        val a = contentWords(front)
        val b = contentWords(earlierFront)
        val small = if (a.size <= b.size) a else b
        val large = if (a.size <= b.size) b else a
        if (small.size >= 2 && large.containsAll(small)) return true
        val sameBack = plainBack(back).let { it.length >= 6 && it == plainBack(earlierBack) }
        return sameBack && a.intersect(b).size >= 3
    }

    internal fun withoutRepeats(cards: List<Card>): List<Card> {
        val kept = mutableListOf<Card>()
        for (card in cards) if (kept.none { repeats(card, it) }) kept += card
        return kept
    }

    const val DEFAULT_TITLE = "Generated cards"

    /** A deck name from the first line of the material, cut at a word. */
    fun titleFrom(material: String): String {
        val line = material.lineSequence()
            .map { it.trim().trimStart('#', '*', '-', '>', ' ').trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: return DEFAULT_TITLE
        if (line.length <= MAX_TITLE) return line.trimEnd('.', ':', ',', ';')
        val cut = line.take(MAX_TITLE)
        return cut.substringBeforeLast(' ', cut).trimEnd('.', ':', ',', ';', ' ').ifEmpty { cut }
    }

    private const val MAX_TITLE = 40

    /** The archive JSON for [deckFromModelText], so validation stays in one place. */
    fun toArchiveJson(parsed: Parsed, fallbackTitle: String = DEFAULT_TITLE): String {
        val notes = JSONArray()
        for (card in withoutRepeats(parsed.cards)) {
            notes.put(JSONObject().put("front", card.front).put("back", card.back))
        }
        val deck = JSONObject().put("name", parsed.title ?: fallbackTitle).put("notes", notes)
        return JSONObject().put("version", 1).put("decks", JSONArray().put(deck)).toString()
    }
}
