package com.mowtiie.flashback.ui.decks;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mowtiie.flashback.data.model.DeckSummary;
import com.mowtiie.flashback.repository.FlashbackRepository;

import java.util.List;

public class DeckListViewModel extends AndroidViewModel {

    private final FlashbackRepository repository;

    private final MutableLiveData<Long> clock = new MutableLiveData<>();

    private final LiveData<List<DeckSummary>> summaries;

    public DeckListViewModel(@NonNull Application application) {
        super(application);
        repository = FlashbackRepository.getInstance(application);
        summaries = Transformations.switchMap(clock, repository::observeDeckSummaries);
        refresh();
    }

    public LiveData<List<DeckSummary>> getSummaries() {
        return summaries;
    }

    public void refresh() {
        clock.setValue(System.currentTimeMillis());
    }
}
