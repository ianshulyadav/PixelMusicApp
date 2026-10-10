package com.unshoo.pixelmusic.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = PixelPlayerDatabase::class.java
    )

    @Test
    fun migrationFromSixCreatesPodcastTablesAndCascadesFeedDeletion() {
        migrationHelper.createDatabase(DATABASE_NAME, 6).close()

        migrationHelper.runMigrationsAndValidate(
            name = DATABASE_NAME,
            version = 7,
            validateDroppedTables = true,
            MIGRATION_6_7
        ).use { database ->
            database.execSQL(
                "INSERT INTO podcast_feeds (id, title, description, artwork_url, added_at) " +
                    "VALUES ('https://example.com/feed.xml', 'Sample', NULL, NULL, 1)"
            )
            database.execSQL(
                "INSERT INTO podcast_episodes " +
                    "(id, feed_id, guid, title, description, audio_url, artwork_url, published_at, duration_ms) " +
                    "VALUES ('episode-1', 'https://example.com/feed.xml', 'guid-1', 'Episode', NULL, " +
                    "'https://example.com/episode.mp3', NULL, NULL, NULL)"
            )

            database.execSQL("DELETE FROM podcast_feeds WHERE id = 'https://example.com/feed.xml'")
            database.query("SELECT COUNT(*) FROM podcast_episodes").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "podcast-migration-test"
    }
}
