package com.mowtiie.flashback.ui.notes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.flashback.data.entity.Note;
import com.mowtiie.flashback.repository.FlashbackRepository;

public class NoteEditorViewModel extends AndroidViewModel {

    /** What the fragment should do once a save completes. */
    public enum SaveOutcome {
        CLOSE,
        CLEAR_FOR_NEXT
    }

    private final FlashbackRepository repository;
    private final long deckId;
    private final long noteId;

    private final MutableLiveData<Note> note = new MutableLiveData<>();
    private final MutableLiveData<SaveOutcome> saveOutcome = new MutableLiveData<>();

    /** Seeds the reverse switch for new cards from the deck's own default. */
    private boolean reverseDefault;

    public NoteEditorViewModel(@NonNull Application application, long deckId, long noteId) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.deckId = deckId;
        this.noteId = noteId;

        if (isEditing()) {
            repository.getNote(noteId, note::setValue);
        } else {
            repository.getDeck(deckId, deck -> {
                if (deck != null) {
                    reverseDefault = deck.reverseByDefault;
                    note.setValue(null);
                }
            });
        }
    }

    public boolean isEditing() {
        return noteId != NoteEditorFragment.NEW_ID;
    }

    public boolean getReverseDefault() {
        return reverseDefault;
    }

    public LiveData<Note> getNote() {
        return note;
    }

    public LiveData<SaveOutcome> getSaveOutcome() {
        return saveOutcome;
    }

    /**
     * Persists the card. Adding a reverse card to an existing note creates it;
     * removing one deletes that card along with its scheduling history, which
     * is why the editor warns about it in the helper text.
     */
    public void save(String front, String back, boolean reverseEnabled, boolean addAnother) {
        SaveOutcome outcome = addAnother ? SaveOutcome.CLEAR_FOR_NEXT : SaveOutcome.CLOSE;

        if (isEditing()) {
            Note existing = note.getValue();
            if (existing == null) {
                return;
            }
            existing.front = front;
            existing.back = back;
            existing.reverseEnabled = reverseEnabled;
            repository.updateNote(existing);
            saveOutcome.setValue(outcome);
        } else {
            Note fresh = new Note(deckId, front, back, reverseEnabled);
            repository.addNote(fresh, id -> saveOutcome.setValue(outcome));
        }
    }

    public void delete() {
        Note existing = note.getValue();
        if (existing != null) {
            repository.deleteNote(existing);
        }
    }
}
