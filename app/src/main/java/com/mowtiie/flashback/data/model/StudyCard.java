package com.mowtiie.flashback.data.model;

import androidx.room.Embedded;

import com.mowtiie.flashback.data.entity.Card;

/** A card joined to its note's text, ready to render on the study screen. */
public class StudyCard {

    @Embedded
    public Card card;

    public String front;

    public String back;

    /** Text shown before the reveal, honouring the card's direction. */
    public String question() {
        return card.ordinal == Card.ORDINAL_REVERSE ? back : front;
    }

    /** Text shown after the reveal. */
    public String answer() {
        return card.ordinal == Card.ORDINAL_REVERSE ? front : back;
    }
}
