package com.mowtiie.flashback.data.model;

import com.mowtiie.flashback.data.entity.Tag;

import java.util.List;

/**
 * A deck row as the list screen needs it: counts from one query, tags from
 * another, stitched together in the ViewModel. Not a Room type.
 */
public class DeckListItem {

    public final DeckSummary summary;

    public final List<Tag> tags;

    public DeckListItem(DeckSummary summary, List<Tag> tags) {
        this.summary = summary;
        this.tags = tags;
    }
}
