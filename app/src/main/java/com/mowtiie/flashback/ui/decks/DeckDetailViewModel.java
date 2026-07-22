package com.mowtiie.flashback.ui.decks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.entity.Note;
import com.mowtiie.flashback.data.model.DeckSummary;
import com.mowtiie.flashback.repository.FlashbackRepository;

import java.util.List;

public class DeckDetailViewModel extends AndroidViewModel {

    private final FlashbackRepository repository;
    private final long deckId;
    private final LiveData<Deck> deck;
    private final LiveData<List<Note>> notes;

    /**
     * Study counts depend on the current time, so the query is reissued on
     * resume. Returning from a session must not leave a stale "Study 12".
     */
    private final MutableLiveData<Long> clock = new MutableLiveData<>();
    private final LiveData<DeckSummary> summary;

    public DeckDetailViewModel(@NonNull Application application, long deckId) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.deckId = deckId;
        this.deck = repository.observeDeck(deckId);
        this.notes = repository.observeNotes(deckId);
        this.summary = Transformations.switchMap(clock,
                now -> repository.observeDeckSummary(deckId, now));
        refresh();
    }

    public void refresh() {
        clock.setValue(System.currentTimeMillis());
    }

    public LiveData<DeckSummary> getSummary() {
        return summary;
    }

    public long getDeckId() {
        return deckId;
    }

    public LiveData<Deck> getDeck() {
        return deck;
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }

    public void deleteDeck() {
        Deck current = deck.getValue();
        if (current != null) {
            repository.deleteDeck(current);
        }
    }
}
