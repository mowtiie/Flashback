package com.mowtiie.flashback.ui.decks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.entity.Note;
import com.mowtiie.flashback.repository.FlashbackRepository;

import java.util.List;

public class DeckDetailViewModel extends AndroidViewModel {

    private final FlashbackRepository repository;
    private final long deckId;
    private final LiveData<Deck> deck;
    private final LiveData<List<Note>> notes;

    public DeckDetailViewModel(@NonNull Application application, long deckId) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.deckId = deckId;
        this.deck = repository.observeDeck(deckId);
        this.notes = repository.observeNotes(deckId);
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
