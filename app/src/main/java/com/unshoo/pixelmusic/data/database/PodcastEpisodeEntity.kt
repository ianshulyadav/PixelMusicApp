package com.unshoo.pixelmusic.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "podcast_episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastFeedEntity::class,
            parentColumns = ["id"],
            childColumns = ["feed_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["feed_id", "published_at"])]
)
data class PodcastEpisodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "feed_id") val feedId: String,
    val guid: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "audio_url") val audioUrl: String,
    @ColumnInfo(name = "artwork_url") val artworkUrl: String?,
    @ColumnInfo(name = "published_at") val publishedAt: Long?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long?
)

data class PodcastEpisodeWithFeed(
    @Embedded val episode: PodcastEpisodeEntity,
    @ColumnInfo(name = "feed_title") val feedTitle: String,
    @ColumnInfo(name = "feed_artwork_url") val feedArtworkUrl: String?
)
