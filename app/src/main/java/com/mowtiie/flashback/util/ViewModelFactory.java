package com.mowtiie.flashback.util;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * Lets a fragment construct a ViewModel that needs arguments, such as a deck
 * id, without writing a dedicated factory class for each one.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    public interface Creator {
        ViewModel create();
    }

    private final Creator creator;

    public ViewModelFactory(Creator creator) {
        this.creator = creator;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        ViewModel viewModel = creator.create();
        if (!modelClass.isInstance(viewModel)) {
            throw new IllegalArgumentException(
                    "Creator produced " + viewModel.getClass().getName()
                            + " but " + modelClass.getName() + " was requested");
        }
        return (T) viewModel;
    }
}
