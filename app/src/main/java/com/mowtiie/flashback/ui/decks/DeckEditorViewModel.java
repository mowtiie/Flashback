package com.mowtiie.flashback.ui.decks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.repository.FlashbackRepository;

public class DeckEditorViewModel extends AndroidViewModel {

    private final FlashbackRepository repository;
    private final long deckId;

    private final MutableLiveData<Deck> deck = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>();

    public DeckEditorViewModel(@NonNull Application application, long deckId) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.deckId = deckId;
        if (isEditing()) {
            repository.getDeck(deckId, deck::setValue);
        }
    }

    public boolean isEditing() {
        return deckId != DeckEditorFragment.NEW_ID;
    }

    public LiveData<Deck> getDeck() {
        return deck;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    public void save(String name, String description, boolean reverseByDefault,
                     int newPerDay, int reviewsPerDay) {
        Deck existing = deck.getValue();
        if (existing == null) {
            existing = new Deck();
            existing.createdAt = System.currentTimeMillis();
        }
        existing.name = name.trim();
        existing.description = description == null || description.trim().isEmpty()
                ? null : description.trim();
        existing.reverseByDefault = reverseByDefault;
        existing.newPerDay = newPerDay;
        existing.reviewsPerDay = reviewsPerDay;

        if (isEditing()) {
            existing.id = deckId;
            repository.updateDeck(existing);
            saved.setValue(true);
        } else {
            repository.insertDeck(existing, id -> saved.setValue(true));
        }
    }

}
