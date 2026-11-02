package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive

/** Joins the cards made for each section into one deck. */
object CoverageMerger {
    class Merged(val name: String, val notes: List<DeckArchive.Note>, val perSection: List<Int>)

    /**
     * A null section failed or has not run. The first card with a given front wins. With [nearDuplicates]
     * a reworded repeat of an earlier card is dropped too, which small on-device models produce.
     */
    fun merge(sections: List<DeckArchive.Deck?>, nearDuplicates: Boolean = false): Merged {
        val seen = HashSet<String>()
        val notes = mutableListOf<DeckArchive.Note>()
        val counts = sections.map { deck ->
            var added = 0
            deck?.notes?.forEach { note ->
                val repeat = nearDuplicates &&
                    notes.any { PlainCards.isRepeat(note.front, note.back, it.front, it.back) }
                if (!repeat && seen.add(note.front.trim().lowercase())) {
                    notes += note
                    added++
                }
            }
            added
        }
        val name = sections.firstNotNullOfOrNull { deck -> deck?.name?.takeIf { it.isNotBlank() } }.orEmpty()
        return Merged(name, notes, counts)
    }
}
