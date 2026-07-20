package com.mowtiie.flashback.data.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.mowtiie.flashback.data.entity.Deck;
import com.mowtiie.flashback.data.entity.DeckTagCrossRef;
import com.mowtiie.flashback.data.entity.Tag;

import java.util.List;

public class DeckWithTags {

    @Embedded
    public Deck deck;

    @Relation(
            parentColumn = "id",
            entityColumn = "id",
            associateBy = @Junction(
                    value = DeckTagCrossRef.class,
                    parentColumn = "deckId",
                    entityColumn = "tagId"))
    public List<Tag> tags;
}
