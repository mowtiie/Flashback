package com.mowtiie.flashback.ui.settings

import com.mowtiie.flashback.data.io.DeckArchive
import com.mowtiie.flashback.data.io.ImportResult
import com.mowtiie.flashback.repository.ArchiveDataSource

class FakeArchive(
    var exportText: String? = null,
    var deckExportText: String? = null,
    var importResult: ImportResult? = ImportResult(decks = 1, notes = 2, tagsCreated = 1, tagsReused = 0),
) : ArchiveDataSource {
    val imported = mutableListOf<DeckArchive>()

    override suspend fun exportAllText() = exportText

    override suspend fun exportDeckText(deckId: Long) = deckExportText

    override suspend fun importArchive(archive: DeckArchive): ImportResult? {
        imported += archive
        return importResult
    }
}
