package com.mowtiie.flashback.generator

import com.mowtiie.flashback.data.io.DeckArchive

/** Joins the cards made for each section into one deck. */
object CoverageMerger {
    class Merged(val name: String, val notes: List<DeckArchive.Note>, val perSection: List<Int>)

    /** A null section failed or has not run. The first card with a given front wins. */
    fun merge(sections: List<DeckArchive.Deck?>): Merged {
        val seen = HashSet<String>()
        val notes = mutableListOf<DeckArchive.Note>()
        val counts = sections.map { deck ->
            var added = 0
            deck?.notes?.forEach { note ->
                if (seen.add(note.front.trim().lowercase())) {
                    notes += note
                    added++
                }
            }
            added
        }
        val name = sections.firstOrNull { it != null }?.name.orEmpty()
        return Merged(name, notes, counts)
    }
}
