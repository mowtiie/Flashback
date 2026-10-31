package com.mowtiie.flashback.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    @ColumnInfo(defaultValue = "0") val color: Int = 0,
)
