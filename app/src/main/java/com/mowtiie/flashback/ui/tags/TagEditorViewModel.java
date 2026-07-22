package com.mowtiie.flashback.ui.tags;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.flashback.data.entity.Tag;
import com.mowtiie.flashback.repository.FlashbackRepository;

public class TagEditorViewModel extends AndroidViewModel {

    private final FlashbackRepository repository;
    private final long tagId;

    private final MutableLiveData<Tag> tag = new MutableLiveData<>();
    private final MutableLiveData<Integer> colour = new MutableLiveData<>();
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>();

    public TagEditorViewModel(@NonNull Application application, long tagId, int defaultColour) {
        super(application);
        this.repository = FlashbackRepository.getInstance(application);
        this.tagId = tagId;

        if (isEditing()) {
            repository.getTag(tagId, loaded -> {
                tag.setValue(loaded);
                if (loaded != null) {
                    colour.setValue(loaded.color);
                }
            });
        } else {
            colour.setValue(defaultColour);
        }
    }

    public boolean isEditing() {
        return tagId != TagEditorFragment.NEW_ID;
    }

    public LiveData<Tag> getTag() {
        return tag;
    }

    public LiveData<Integer> getColour() {
        return colour;
    }

    public LiveData<Boolean> getFinished() {
        return finished;
    }

    public void selectColour(int value) {
        colour.setValue(value);
    }

    public void save(String name, String description) {
        Tag existing = tag.getValue();
        if (existing == null) {
            existing = new Tag();
        }
        existing.name = name.trim();
        existing.description = description == null || description.trim().isEmpty()
                ? null : description.trim();
        Integer chosen = colour.getValue();
        existing.color = chosen == null ? 0 : chosen;

        if (isEditing()) {
            existing.id = tagId;
            repository.updateTag(existing);
            finished.setValue(true);
        } else {
            repository.insertTag(existing, id -> finished.setValue(true));
        }
    }

    public void delete() {
        Tag existing = tag.getValue();
        if (existing != null) {
            repository.deleteTag(existing);
        }
    }
}
