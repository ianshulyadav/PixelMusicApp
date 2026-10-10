package com.unshoo.pixelmusic.data.repository

import android.util.Xml
import okhttp3.HttpUrl
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class ParsedPodcastFeed(
    val title: String,
    val description: String?,
    val artworkUrl: String?,
    val episodes: List<ParsedPodcastEpisode>
)

internal data class ParsedPodcastEpisode(
    val guid: String,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val artworkUrl: String?,
    val publishedAt: Long?,
    val durationMs: Long?
)

internal object RssPodcastFeedParser {
    private const val ITUNES_NAMESPACE = "http://www.itunes.com/dtds/podcast-1.0.dtd"

    fun parse(input: InputStream, feedUrl: HttpUrl): ParsedPodcastFeed {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
            setInput(input, null)
        }

        var title: String? = null
        var description: String? = null
        var artworkUrl: String? = null
        val episodes = mutableListOf<ParsedPodcastEpisode>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name.localName()) {
                "item", "entry" -> parseEpisode(parser, feedUrl)?.let(episodes::add)
                "title" -> if (title == null) title = readElementText(parser).cleanText()
                "description", "summary" -> if (description == null) {
                    description = readElementText(parser).cleanText()
                }
                "image" -> if (artworkUrl == null) {
                    artworkUrl = readFeedImage(parser, feedUrl)
                }
            }
        }

        val feedTitle = title?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("The feed does not include a podcast title.")
        return ParsedPodcastFeed(
            title = feedTitle,
            description = description?.takeIf(String::isNotBlank),
            artworkUrl = artworkUrl,
            episodes = episodes
        )
    }

    private fun parseEpisode(parser: XmlPullParser, feedUrl: HttpUrl): ParsedPodcastEpisode? {
        var title: String? = null
        var guid: String? = null
        var description: String? = null
        var audioUrl: String? = null
        var artworkUrl: String? = null
        var publishedAt: Long? = null
        var durationMs: Long? = null
        val entryTag = parser.name

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.name == entryTag) break
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name.localName()) {
                "enclosure" -> {
                    val candidate = parser.getAttributeValue(null, "url")
                    val mime = parser.getAttributeValue(null, "type")
                    val resolved = resolveUrl(feedUrl, candidate)
                    if (resolved != null && isAudioUrl(resolved, mime)) {
                        audioUrl = resolved
                    }
                }
                "link" -> {
                    val candidate = parser.getAttributeValue(null, "href")
                    val rel = parser.getAttributeValue(null, "rel")
                    val mime = parser.getAttributeValue(null, "type")
                    val resolved = resolveUrl(feedUrl, candidate)
                    if (
                        resolved != null &&
                        (rel == "enclosure" || isAudioUrl(resolved, mime))
                    ) {
                        audioUrl = resolved
                    } else if (candidate == null) {
                        val linkText = readElementText(parser).trim()
                        val textUrl = resolveUrl(feedUrl, linkText)
                        if (textUrl != null && isAudioUrl(textUrl, null)) audioUrl = textUrl
                    }
                }
                "title" -> if (title == null) title = readElementText(parser).cleanText()
                "guid", "id" -> if (guid == null) guid = readElementText(parser).trim()
                "description", "summary", "content" -> if (description == null) {
                    description = readElementText(parser).cleanText()
                }
                "pubDate", "published", "updated" -> {
                    publishedAt = parseDate(readElementText(parser))
                }
                "duration" -> durationMs = parseDuration(readElementText(parser))
                "image" -> {
                    artworkUrl = resolveUrl(
                        feedUrl,
                        parser.getAttributeValue(ITUNES_NAMESPACE, "href")
                            ?: parser.getAttributeValue(null, "href")
                    ) ?: readFeedImage(parser, feedUrl)
                }
            }
        }

        val enclosureUrl = audioUrl ?: return null
        val episodeTitle = title?.takeIf(String::isNotBlank) ?: "Untitled episode"
        val episodeGuid = guid?.takeIf(String::isNotBlank) ?: enclosureUrl
        return ParsedPodcastEpisode(
            guid = episodeGuid,
            title = episodeTitle,
            description = description?.takeIf(String::isNotBlank),
            audioUrl = enclosureUrl,
            artworkUrl = artworkUrl,
            publishedAt = publishedAt,
            durationMs = durationMs
        )
    }

    private fun readFeedImage(parser: XmlPullParser, feedUrl: HttpUrl): String? {
        val imageTag = parser.name
        val href = parser.getAttributeValue(ITUNES_NAMESPACE, "href")
            ?: parser.getAttributeValue(null, "href")
        if (href != null) return resolveUrl(feedUrl, href)

        var imageUrl: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.name == imageTag) break
            if (parser.eventType == XmlPullParser.START_TAG && parser.name.localName() == "url") {
                imageUrl = resolveUrl(feedUrl, readElementText(parser))
            }
        }
        return imageUrl
    }

    private fun readElementText(parser: XmlPullParser): String {
        val text = StringBuilder()
        var nestedDepth = 0
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> nestedDepth++
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (nestedDepth == 0) return text.toString()
                    nestedDepth--
                }
            }
        }
        return text.toString()
    }

    private fun resolveUrl(base: HttpUrl, candidate: String?): String? {
        val value = candidate?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val resolved = base.resolve(value) ?: return null
        return resolved.takeIf { it.scheme == "http" || it.scheme == "https" }?.toString()
    }

    private fun isAudioUrl(url: String, mimeType: String?): Boolean {
        if (mimeType?.startsWith("audio/", ignoreCase = true) == true) return true
        val path = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        return listOf(".mp3", ".m4a", ".m4b", ".mp4", ".aac", ".ogg", ".opus", ".wav", ".flac")
            .any(path::endsWith)
    }

    private fun parseDuration(value: String): Long? {
        val parts = value.trim().split(':')
        val seconds = parts.fold(0L) { total, part ->
            val number = part.toLongOrNull() ?: return null
            if (number < 0) return null
            total * 60 + number
        }
        return (seconds * 1000).takeIf { it > 0 }
    }

    private fun parseDate(value: String): Long? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        runCatching { Instant.parse(trimmed).toEpochMilli() }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }
            .getOrNull()?.let { return it }

        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (pattern in patterns) {
            val formatter = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            val parsed: Date = formatter.parse(trimmed, position) ?: continue
            if (position.index == trimmed.length) return parsed.time
        }
        return null
    }

    private fun String.localName(): String = substringAfter(':')

    private fun String.cleanText(): String =
        replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
}
