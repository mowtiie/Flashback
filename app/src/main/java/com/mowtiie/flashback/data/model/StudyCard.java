package com.mowtiie.flashback.data.model;

import androidx.room.Embedded;

import com.mowtiie.flashback.data.entity.Card;

public class StudyCard {

    @Embedded
    public Card card;

    public String front;

    public String back;

    public String question() {
        return card.ordinal == Card.ORDINAL_REVERSE ? back : front;
    }

    public String answer() {
        return card.ordinal == Card.ORDINAL_REVERSE ? front : back;
    }
}
