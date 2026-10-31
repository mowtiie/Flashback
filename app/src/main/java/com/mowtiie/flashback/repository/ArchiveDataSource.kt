package com.mowtiie.flashback.repository

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.data.io.ImportResult

/** Import and export as the settings and deck screens use them, so they can be faked in tests. */
interface ArchiveDataSource {
    /** Null when there is nothing to export. */
    suspend fun exportAllText(): String?

    /** Null when the deck no longer exists. */
    suspend fun exportDeckText(deckId: Long): String?

    /** Null when the import failed and nothing was added. */
    suspend fun importArchive(archive: DeckArchive): ImportResult?
}
