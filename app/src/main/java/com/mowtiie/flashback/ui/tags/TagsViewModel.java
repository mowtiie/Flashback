package com.mowtiie.flashback.ui.tags;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mowtiie.flashback.data.model.TagWithCount;
import com.mowtiie.flashback.repository.FlashbackRepository;

import java.util.List;

public class TagsViewModel extends AndroidViewModel {

    private final LiveData<List<TagWithCount>> tags;

    public TagsViewModel(@NonNull Application application) {
        super(application);
        tags = FlashbackRepository.getInstance(application).observeTagsWithCounts();
    }

    public LiveData<List<TagWithCount>> getTags() {
        return tags;
    }
}
