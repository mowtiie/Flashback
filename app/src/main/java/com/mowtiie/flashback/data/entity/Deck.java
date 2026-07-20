package com.mowtiie.flashback.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "decks")
public class Deck {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    public String description;

    @ColumnInfo(defaultValue = "0")
    public boolean reverseByDefault = false;

    @ColumnInfo(defaultValue = "20")
    public int newPerDay = 20;

    @ColumnInfo(defaultValue = "200")
    public int reviewsPerDay = 200;

    public long createdAt;

    public Deck() {
    }

    public Deck(@NonNull String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
    }
}
