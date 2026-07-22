package com.mowtiie.flashback.ui.decks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.repository.FlashbackRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeckEditorViewModel extends AndroidViewModel {

    private final FlashbackRepository repository;
    private final long deckId;

    private final MutableLiveData<Deck> deck = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>();
    private final LiveData<List<Tag>> allTags;

    private final MediatorLiveData<Set<Long>> selectedTagIds = new MediatorLiveData<>();

    /**
     * The deck's saved tags arrive asynchronously and the source may re-emit.
     * Seeding the selection more than once would wipe changes the user has
     * already made in the chip row.
     */
    private boolean selectionSeeded;

    public DeckEditorViewModel(@NonNull Application application, long deckId) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.deckId = deckId;
        this.allTags = repository.observeAllTags();

        selectedTagIds.setValue(new HashSet<>());

        if (isEditing()) {
            repository.getDeck(deckId, deck::setValue);

            LiveData<List<Tag>> existing = repository.observeTagsForDeck(deckId);
            selectedTagIds.addSource(existing, tags -> {
                if (selectionSeeded || tags == null) {
                    return;
                }
                selectionSeeded = true;
                Set<Long> ids = new HashSet<>();
                for (Tag tag : tags) {
                    ids.add(tag.id);
                }
                selectedTagIds.setValue(ids);
                selectedTagIds.removeSource(existing);
            });
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

    public LiveData<List<Tag>> getAllTags() {
        return allTags;
    }

    public LiveData<Set<Long>> getSelectedTagIds() {
        return selectedTagIds;
    }

    public void toggleTag(long tagId, boolean checked) {
        Set<Long> current = selectedTagIds.getValue();
        Set<Long> updated = current == null ? new HashSet<>() : new HashSet<>(current);
        if (checked) {
            updated.add(tagId);
        } else {
            updated.remove(tagId);
        }
        selectedTagIds.setValue(updated);
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

        Set<Long> tagIds = selectedTagIds.getValue();
        List<Long> ids = tagIds == null ? new ArrayList<>() : new ArrayList<>(tagIds);

        if (isEditing()) {
            existing.id = deckId;
            repository.updateDeck(existing);
            repository.setDeckTags(deckId, ids);
            saved.setValue(true);
        } else {
            // Tags can only be linked once the insert has produced an id.
            repository.insertDeck(existing, newId -> {
                repository.setDeckTags(newId, ids);
                saved.setValue(true);
            });
        }
    }
}
