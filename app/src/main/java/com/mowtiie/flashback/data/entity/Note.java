package com.mowtiie.flashback.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notes",
        foreignKeys = @ForeignKey(
                entity = Deck.class,
                parentColumns = "id",
                childColumns = "deckId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("deckId"))
public class Note {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long deckId;

    @NonNull
    public String front = "";

    @NonNull
    public String back = "";

    public boolean reverseEnabled;

    public long createdAt;

    public long modifiedAt;

    public Note() {
    }

    public Note(long deckId, @NonNull String front, @NonNull String back, boolean reverseEnabled) {
        this.deckId = deckId;
        this.front = front;
        this.back = back;
        this.reverseEnabled = reverseEnabled;
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = this.createdAt;
    }
}
