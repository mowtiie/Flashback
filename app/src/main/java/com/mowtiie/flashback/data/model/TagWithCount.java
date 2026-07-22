package com.mowtiie.flashback.data.model;

import androidx.room.Embedded;

import com.mowtiie.flashback.data.entity.Tag;

public class TagWithCount {

    @Embedded
    public Tag tag;

    public int deckCount;
}
