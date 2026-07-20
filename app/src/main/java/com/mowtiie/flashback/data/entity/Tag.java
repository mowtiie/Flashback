package com.mowtiie.flashback.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "tags",
        indices = @Index(value = "name", unique = true))
public class Tag {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    public String description;

    @ColumnInfo(defaultValue = "0")
    public int color;

    public Tag() {
    }

    public Tag(@NonNull String name, String description, int color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }
}
