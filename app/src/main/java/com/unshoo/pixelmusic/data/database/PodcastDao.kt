package com.unshoo.pixelmusic.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcast_feeds ORDER BY title COLLATE NOCASE")
    fun observeFeeds(): Flow<List<PodcastFeedEntity>>

    @Query("SELECT * FROM podcast_feeds ORDER BY title COLLATE NOCASE")
    suspend fun getFeeds(): List<PodcastFeedEntity>

    @Query(
        """
        SELECT podcast_episodes.*, podcast_feeds.title AS feed_title,
            podcast_feeds.artwork_url AS feed_artwork_url
        FROM podcast_episodes
        INNER JOIN podcast_feeds ON podcast_feeds.id = podcast_episodes.feed_id
        ORDER BY podcast_episodes.published_at DESC, podcast_episodes.title COLLATE NOCASE
        """
    )
    fun observeEpisodes(): Flow<List<PodcastEpisodeWithFeed>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceFeed(feed: PodcastFeedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceEpisodes(episodes: List<PodcastEpisodeEntity>)

    @Transaction
    suspend fun saveFeedWithEpisodes(
        feed: PodcastFeedEntity,
        episodes: List<PodcastEpisodeEntity>
    ) {
        insertOrReplaceFeed(feed)
        insertOrReplaceEpisodes(episodes)
    }

    @Query("DELETE FROM podcast_feeds WHERE id = :feedId")
    suspend fun deleteFeed(feedId: String)
}
