package com.unshoo.pixelmusic.data.repository

import com.unshoo.pixelmusic.data.database.PodcastDao
import com.unshoo.pixelmusic.data.database.PodcastEpisodeEntity
import com.unshoo.pixelmusic.data.database.PodcastEpisodeWithFeed
import com.unshoo.pixelmusic.data.database.PodcastFeedEntity
import com.unshoo.pixelmusic.data.model.Song
import kotlinx.coroutines.flow.Flow
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    private val podcastDao: PodcastDao,
    private val okHttpClient: OkHttpClient
) {
    val feeds: Flow<List<PodcastFeedEntity>> = podcastDao.observeFeeds()
    val episodes: Flow<List<PodcastEpisodeWithFeed>> = podcastDao.observeEpisodes()

    suspend fun addFeed(rawUrl: String) {
        val url = parseFeedUrl(rawUrl)
        refreshFeed(url)
    }

    suspend fun refreshFeeds() {
        val currentFeeds = podcastDao.getFeeds()
        for (feed in currentFeeds) {
            refreshFeed(parseFeedUrl(feed.id))
        }
    }

    suspend fun removeFeed(feedId: String) {
        podcastDao.deleteFeed(feedId)
    }

    private suspend fun refreshFeed(feedUrl: HttpUrl) {
        val request = Request.Builder()
            .url(feedUrl)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Feed request failed with HTTP ${response.code}.")
            }
            val body = response.body ?: throw IllegalStateException("The feed response was empty.")
            val parsed = RssPodcastFeedParser.parse(body.byteStream(), response.request.url)
            if (parsed.episodes.isEmpty()) {
                throw IllegalArgumentException("This feed does not contain any playable audio episodes.")
            }

            val feed = PodcastFeedEntity(
                id = feedUrl.toString(),
                title = parsed.title,
                description = parsed.description,
                artworkUrl = parsed.artworkUrl
            )
            val episodes = parsed.episodes.map { episode ->
                PodcastEpisodeEntity(
                    id = "${feedUrl}|${episode.guid}",
                    feedId = feedUrl.toString(),
                    guid = episode.guid,
                    title = episode.title,
                    description = episode.description,
                    audioUrl = episode.audioUrl,
                    artworkUrl = episode.artworkUrl ?: parsed.artworkUrl,
                    publishedAt = episode.publishedAt,
                    durationMs = episode.durationMs
                )
            }
            podcastDao.saveFeedWithEpisodes(feed, episodes)
        }
    }

    private fun parseFeedUrl(rawUrl: String): HttpUrl {
        val url = rawUrl.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Enter a valid RSS feed URL beginning with http:// or https://.")
        if (url.scheme != "http" && url.scheme != "https") {
            throw IllegalArgumentException("Podcast feeds must use HTTP or HTTPS.")
        }
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            throw IllegalArgumentException("Feed URLs cannot contain a username or password.")
        }
        return url
    }
}

fun PodcastEpisodeWithFeed.toSong(): Song = Song(
    id = episode.id,
    title = episode.title,
    artist = feedTitle,
    artistId = 0L,
    album = feedTitle,
    albumId = 0L,
    path = episode.audioUrl,
    contentUriString = episode.audioUrl,
    albumArtUriString = episode.artworkUrl ?: feedArtworkUrl,
    duration = episode.durationMs ?: 0L,
    genre = "Podcast",
    dateAdded = episode.publishedAt ?: 0L,
    mimeType = null,
    bitrate = null,
    sampleRate = null
)
