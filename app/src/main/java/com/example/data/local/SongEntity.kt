package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val originalKey: String,
    val content: String, // Bracketed format: "Amazing [G]Grace, how [C]sweet..."
    val createdAt: Long = System.currentTimeMillis()
)
