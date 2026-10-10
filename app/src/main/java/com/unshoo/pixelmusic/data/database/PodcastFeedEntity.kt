package com.unshoo.pixelmusic.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcast_feeds")
data class PodcastFeedEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)
