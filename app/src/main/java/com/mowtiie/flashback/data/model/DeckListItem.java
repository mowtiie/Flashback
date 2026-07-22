package com.mowtiie.flashback.data.model;

import com.mowtiie.flashback.data.entity.Tag;

import java.util.List;

public class DeckListItem {

    public final DeckSummary summary;

    public final List<Tag> tags;

    public DeckListItem(DeckSummary summary, List<Tag> tags) {
        this.summary = summary;
        this.tags = tags;
    }
}
