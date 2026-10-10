package com.unshoo.pixelmusic.data.repository

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class RssPodcastFeedParserTest {
    @Test
    fun parsesRssEnclosuresAndResolvesRelativeArtwork() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Sample show</title>
                <description>Weekly news</description>
                <itunes:image href="/cover.jpg"/>
                <item>
                  <title>First episode</title>
                  <guid>episode-1</guid>
                  <description>Episode details</description>
                  <enclosure url="audio/episode-1.mp3" type="audio/mpeg"/>
                  <pubDate>Thu, 01 Jan 2026 12:00:00 GMT</pubDate>
                  <itunes:duration>01:02:03</itunes:duration>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val parsed = RssPodcastFeedParser.parse(
            ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)),
            "https://example.com/podcasts/feed.xml".toHttpUrl()
        )

        assertThat(parsed.title).isEqualTo("Sample show")
        assertThat(parsed.description).isEqualTo("Weekly news")
        assertThat(parsed.artworkUrl).isEqualTo("https://example.com/cover.jpg")
        assertThat(parsed.episodes).hasSize(1)
        assertThat(parsed.episodes.single().guid).isEqualTo("episode-1")
        assertThat(parsed.episodes.single().audioUrl)
            .isEqualTo("https://example.com/podcasts/audio/episode-1.mp3")
        assertThat(parsed.episodes.single().durationMs).isEqualTo(3_723_000L)
    }
}
