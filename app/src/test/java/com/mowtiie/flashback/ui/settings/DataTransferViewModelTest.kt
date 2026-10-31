package com.mowtiie.flashback.ui.settings

import com.mowtiie.flashback.FakePreferenceStore
import com.mowtiie.flashback.notifications.ReminderPreferences
import com.mowtiie.flashback.theme.ThemePreferences
import com.mowtiie.flashback.ui.study.StudyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsArchiveTest {

    private val archive = FakeArchive()
    private val validJson = "{\"version\":1,\"decks\":[{\"name\":\"D\",\"tags\":[{\"name\":\"t\"}]," +
        "\"notes\":[{\"front\":\"a\",\"back\":\"b\"},{\"front\":\"c\",\"back\":\"d\"}]}]}"

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): SettingsViewModel {
        val store = FakePreferenceStore()
        return SettingsViewModel(
            studyPrefs = StudyPreferences(store),
            themePrefs = ThemePreferences(store),
            reminderPrefs = ReminderPreferences(store),
            scheduler = {},
            archive = archive,
            ioDispatcher = UnconfinedTestDispatcher(),
            dynamicColorAvailable = false,
        )
    }

    @Test
    fun exportWithNothingToExportSaysSo() = runTest {
        val viewModel = viewModel()
        viewModel.prepareExportAll()
        assertEquals(SettingsEvent.ExportNothing, viewModel.eventFlow.first())
        assertFalse(viewModel.uiState.value.working)
    }

    @Test
    fun exportWritesTheHeldTextOnce() = runTest {
        archive.exportText = "{export}"
        val viewModel = viewModel()
        viewModel.prepareExportAll()
        assertEquals(SettingsEvent.ExportReady, viewModel.eventFlow.first())

        val written = mutableListOf<String>()
        viewModel.saveExport { written += it }
        assertEquals(SettingsEvent.ExportSaved, viewModel.eventFlow.first())
        assertEquals(listOf("{export}"), written)

        viewModel.saveExport { written += it }
        assertEquals(SettingsEvent.ExportWriteFailed, viewModel.eventFlow.first())
        assertEquals(1, written.size)
    }

    @Test
    fun aFailedWriteIsReported() = runTest {
        archive.exportText = "{export}"
        val viewModel = viewModel()
        viewModel.prepareExportAll()
        viewModel.eventFlow.first()

        viewModel.saveExport { throw IOException("disk full") }
        assertEquals(SettingsEvent.ExportWriteFailed, viewModel.eventFlow.first())
        assertFalse(viewModel.uiState.value.working)
    }

    @Test
    fun cancellingTheSaveDialogDropsTheHeldText() = runTest {
        archive.exportText = "{export}"
        val viewModel = viewModel()
        viewModel.prepareExportAll()
        viewModel.eventFlow.first()

        viewModel.discardExport()
        var wrote = false
        viewModel.saveExport { wrote = true }
        assertEquals(SettingsEvent.ExportWriteFailed, viewModel.eventFlow.first())
        assertFalse(wrote)
    }

    @Test
    fun pastedTextThatParsesBecomesAPendingImportWithCounts() {
        val viewModel = viewModel()
        viewModel.previewImportFromText(validJson)

        val pending = viewModel.uiState.value.pendingImport
        assertNotNull(pending)
        assertEquals(1, pending!!.counts.decks)
        assertEquals(2, pending.counts.notes)
        assertEquals(1, pending.counts.tags)
        assertNull(viewModel.uiState.value.pasteError)
        assertTrue(archive.imported.isEmpty())
    }

    @Test
    fun pastedTextThatFailsStaysInTheDialogWithAMessage() {
        val viewModel = viewModel()
        viewModel.previewImportFromText("not json")

        assertNull(viewModel.uiState.value.pendingImport)
        assertTrue(viewModel.uiState.value.pasteError!!.contains("valid JSON"))
    }

    @Test
    fun editingThePastedTextClearsItsError() {
        val viewModel = viewModel()
        viewModel.previewImportFromText("")
        assertNotNull(viewModel.uiState.value.pasteError)

        viewModel.clearPasteError()
        assertNull(viewModel.uiState.value.pasteError)
    }

    @Test
    fun aRejectedFileIsReportedAsAnEventNotInTheDialog() = runTest {
        val viewModel = viewModel()
        viewModel.previewImportFromFile { "{\"version\":99,\"decks\":[{\"name\":\"x\"}]}" }

        val event = viewModel.eventFlow.first() as SettingsEvent.ImportRejected
        assertTrue(event.message.contains("newer version"))
        assertNull(viewModel.uiState.value.pasteError)
        assertNull(viewModel.uiState.value.pendingImport)
    }

    @Test
    fun anUnreadableFileIsReported() = runTest {
        val viewModel = viewModel()
        viewModel.previewImportFromFile { throw IOException("too large") }

        val event = viewModel.eventFlow.first() as SettingsEvent.ImportReadFailed
        assertEquals("too large", event.message)
        assertFalse(viewModel.uiState.value.working)
    }

    @Test
    fun aValidFileBecomesAPendingImport() {
        val viewModel = viewModel()
        viewModel.previewImportFromFile { validJson }
        assertEquals(2, viewModel.uiState.value.pendingImport!!.counts.notes)
    }

    @Test
    fun confirmingImportsExactlyThePreviewedArchive() = runTest {
        val viewModel = viewModel()
        viewModel.previewImportFromText(validJson)
        val pending = viewModel.uiState.value.pendingImport!!

        viewModel.confirmImport()

        val event = viewModel.eventFlow.first() as SettingsEvent.ImportDone
        assertEquals(2, event.result.notes)
        assertSame(pending.archive, archive.imported.single())
        assertNull(viewModel.uiState.value.pendingImport)
        assertFalse(viewModel.uiState.value.working)
    }

    @Test
    fun aFailedImportIsReported() = runTest {
        archive.importResult = null
        val viewModel = viewModel()
        viewModel.previewImportFromText(validJson)

        viewModel.confirmImport()
        assertEquals(SettingsEvent.ImportFailed, viewModel.eventFlow.first())
    }

    @Test
    fun cancellingThePreviewWritesNothing() {
        val viewModel = viewModel()
        viewModel.previewImportFromText(validJson)
        viewModel.clearPendingImport()

        viewModel.confirmImport()
        assertNull(viewModel.uiState.value.pendingImport)
        assertTrue(archive.imported.isEmpty())
    }
}
