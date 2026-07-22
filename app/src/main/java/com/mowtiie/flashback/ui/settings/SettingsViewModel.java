package com.mowtiie.flashback.ui.settings;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.flashback.AppExecutors;
import com.mowtiie.flashback.data.io.ArchiveException;
import com.mowtiie.flashback.data.io.ArchiveFiles;
import com.mowtiie.flashback.data.io.ArchiveParser;
import com.mowtiie.flashback.data.io.ArchiveRepository;
import com.mowtiie.flashback.data.io.ArchiveSerializer;
import com.mowtiie.flashback.data.io.DeckArchive;
import com.mowtiie.flashback.repository.FlashbackRepository;

import java.io.IOException;

/**
 * Drives export and import. Import is deliberately two-staged: parse and
 * preview first, then commit only after the user confirms the counts. Nothing
 * touches the database until confirmation.
 */
public class SettingsViewModel extends AndroidViewModel {

    /** A one-shot message for the UI to show and then clear. */
    public static class Event {
        public final boolean success;
        public final String message;

        Event(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /** A parsed-but-not-yet-written archive awaiting the user's confirmation. */
    public static class Pending {
        public final DeckArchive archive;
        public final DeckArchive.Counts counts;

        Pending(DeckArchive archive) {
            this.archive = archive;
            this.counts = archive.counts();
        }
    }

    private final FlashbackRepository repository;
    private final AppExecutors executors;
    private final ArchiveSerializer serializer = new ArchiveSerializer();
    private final ArchiveParser parser = new ArchiveParser();

    private final MutableLiveData<Boolean> working = new MutableLiveData<>(false);
    private final MutableLiveData<String> exportPayload = new MutableLiveData<>();
    private final MutableLiveData<Pending> pendingImport = new MutableLiveData<>();
    private final MutableLiveData<Event> event = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.executors = AppExecutors.getInstance();
    }

    public LiveData<Boolean> getWorking() {
        return working;
    }

    public LiveData<String> getExportPayload() {
        return exportPayload;
    }

    public LiveData<Pending> getPendingImport() {
        return pendingImport;
    }

    public LiveData<Event> getEvent() {
        return event;
    }

    public void clearEvent() {
        event.setValue(null);
    }

    public void clearExportPayload() {
        exportPayload.setValue(null);
    }

    public void clearPendingImport() {
        pendingImport.setValue(null);
    }

    // --------------------------------------------------------------- export

    /**
     * Serializes the collection, then hands the text back so the fragment can
     * launch the SAF create-document picker. The file is written only once the
     * user has chosen a destination.
     */
    public void prepareExportAll() {
        working.setValue(true);
        repository.exportAll(archive -> {
            working.setValue(false);
            if (archive.decks.isEmpty()) {
                event.setValue(new Event(false, "There are no decks to export yet."));
                return;
            }
            exportPayload.setValue(serializer.serialize(archive));
        });
    }

    public void prepareExportDeck(long deckId) {
        working.setValue(true);
        repository.exportDeck(deckId, archive -> {
            working.setValue(false);
            if (archive.decks.isEmpty()) {
                event.setValue(new Event(false, "That deck could not be found."));
                return;
            }
            exportPayload.setValue(serializer.serialize(archive));
        });
    }

    /** Writes the prepared text to the chosen Uri off the main thread. */
    public void writeExport(Uri destination, String content) {
        working.setValue(true);
        executors.diskIO().execute(() -> {
            try {
                ArchiveFiles.write(getApplication().getContentResolver(), destination, content);
                post(new Event(true, "Export saved."));
            } catch (IOException e) {
                post(new Event(false, e.getMessage() == null
                        ? "Could not save the file." : e.getMessage()));
            } finally {
                executors.mainThread().execute(() -> working.setValue(false));
            }
        });
    }

    // --------------------------------------------------------------- import

    /** Reads a chosen file, parses it, and stages a preview. No writes yet. */
    public void previewImportFromUri(Uri source) {
        working.setValue(true);
        executors.diskIO().execute(() -> {
            try {
                String raw = ArchiveFiles.read(getApplication().getContentResolver(), source);
                stagePreview(raw);
            } catch (IOException e) {
                post(new Event(false, e.getMessage() == null
                        ? "Could not read the file." : e.getMessage()));
            } finally {
                executors.mainThread().execute(() -> working.setValue(false));
            }
        });
    }

    /** Parses pasted text and stages a preview. */
    public void previewImportFromText(String raw) {
        working.setValue(true);
        executors.diskIO().execute(() -> {
            stagePreview(raw);
            executors.mainThread().execute(() -> working.setValue(false));
        });
    }

    private void stagePreview(String raw) {
        try {
            DeckArchive archive = parser.parse(raw);
            executors.mainThread().execute(() ->
                    pendingImport.setValue(new Pending(archive)));
        } catch (ArchiveException e) {
            post(new Event(false, e.getMessage()));
        }
    }

    /** Commits the staged import. Called only from the confirmation dialog. */
    public void confirmImport() {
        Pending pending = pendingImport.getValue();
        if (pending == null) {
            return;
        }
        pendingImport.setValue(null);
        working.setValue(true);
        repository.importArchive(pending.archive, result -> {
            working.setValue(false);
            event.setValue(new Event(true, summarize(result)));
        });
    }

    private String summarize(ArchiveRepository.ImportResult result) {
        StringBuilder message = new StringBuilder();
        message.append("Imported ").append(result.decks)
                .append(result.decks == 1 ? " deck" : " decks")
                .append(" and ").append(result.notes)
                .append(result.notes == 1 ? " card" : " cards");
        if (result.tagsReused > 0) {
            message.append(". ").append(result.tagsReused)
                    .append(result.tagsReused == 1 ? " tag was" : " tags were")
                    .append(" merged with existing ones");
        }
        message.append('.');
        return message.toString();
    }

    private void post(Event value) {
        executors.mainThread().execute(() -> event.setValue(value));
    }
}
