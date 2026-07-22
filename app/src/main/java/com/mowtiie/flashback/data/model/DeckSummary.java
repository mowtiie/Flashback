package com.mowtiie.flashback.data.model;

import androidx.room.Embedded;

import com.mowtiie.flashback.data.entity.Deck;

/** Deck plus the counts the deck list needs, gathered in one query. */
public class DeckSummary {

    @Embedded
    public Deck deck;

    public int newCount;

    public int learnCount;

    public int dueCount;

    public int totalCount;

    public int studyableCount() {
        return Math.min(newCount, deck.newPerDay) + learnCount + dueCount;
    }
}
