package com.mowtiie.flashback.ui.decks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.data.model.DeckListItem;
import com.mowtiie.flashback.data.model.DeckSummary;
import com.mowtiie.flashback.data.model.DeckWithTags;
import com.mowtiie.flashback.repository.FlashbackRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeckListViewModel extends AndroidViewModel {

    private final FlashbackRepository repository;

    /**
     * Due counts are relative to "now", so the query is re-issued whenever the
     * screen resumes. Without this the counts freeze at whatever time the
     * ViewModel happened to be created.
     */
    private final MutableLiveData<Long> clock = new MutableLiveData<>();

    /** Null means no filter. */
    private final MutableLiveData<Long> selectedTagId = new MutableLiveData<>();

    private final LiveData<List<DeckSummary>> summaries;
    private final LiveData<List<DeckWithTags>> decksWithTags;
    private final LiveData<List<Tag>> allTags;

    /**
     * Counts and tags come from separate queries, joined here rather than in
     * SQL. Filtering happens on the joined list too, which keeps the tag filter
     * instant instead of round-tripping to the database on every chip tap.
     */
    private final MediatorLiveData<List<DeckListItem>> items = new MediatorLiveData<>();

    public DeckListViewModel(@NonNull Application application) {
        super(application);
        repository = FlashbackRepository.getInstance(application);

        summaries = Transformations.switchMap(clock, repository::observeDeckSummaries);
        decksWithTags = repository.observeDecksWithTags();
        allTags = repository.observeAllTags();

        items.addSource(summaries, ignored -> combine());
        items.addSource(decksWithTags, ignored -> combine());
        items.addSource(selectedTagId, ignored -> combine());

        refresh();
    }

    public LiveData<List<DeckListItem>> getItems() {
        return items;
    }

    public LiveData<List<Tag>> getAllTags() {
        return allTags;
    }

    @Nullable
    public Long getSelectedTagId() {
        return selectedTagId.getValue();
    }

    public void selectTag(@Nullable Long tagId) {
        selectedTagId.setValue(tagId);
    }

    public void refresh() {
        clock.setValue(System.currentTimeMillis());
    }

    private void combine() {
        List<DeckSummary> currentSummaries = summaries.getValue();
        if (currentSummaries == null) {
            return;
        }

        Map<Long, List<Tag>> tagsByDeck = new HashMap<>();
        List<DeckWithTags> withTags = decksWithTags.getValue();
        if (withTags != null) {
            for (DeckWithTags entry : withTags) {
                tagsByDeck.put(entry.deck.id, entry.tags);
            }
        }

        Long filter = selectedTagId.getValue();
        List<DeckListItem> result = new ArrayList<>(currentSummaries.size());

        for (DeckSummary summary : currentSummaries) {
            List<Tag> tags = tagsByDeck.get(summary.deck.id);
            if (tags == null) {
                tags = Collections.emptyList();
            }
            if (filter != null && !hasTag(tags, filter)) {
                continue;
            }
            result.add(new DeckListItem(summary, tags));
        }
        items.setValue(result);
    }

    private boolean hasTag(List<Tag> tags, long tagId) {
        for (Tag tag : tags) {
            if (tag.id == tagId) {
                return true;
            }
        }
        return false;
    }
}
