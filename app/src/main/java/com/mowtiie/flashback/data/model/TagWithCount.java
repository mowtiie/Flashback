package com.mowtiie.flashback.data.model;

import androidx.room.Embedded;

import com.mowtiie.flashback.data.entity.Tag;

/** Tag plus how many decks carry it, for the tag list. */
public class TagWithCount {

    @Embedded
    public Tag tag;

    public int deckCount;
}
