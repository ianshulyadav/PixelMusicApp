package com.unshoo.pixelmusic.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.unshoo.pixelmusic.R
import com.unshoo.pixelmusic.data.database.PodcastEpisodeWithFeed
import com.unshoo.pixelmusic.data.database.PodcastFeedEntity
import com.unshoo.pixelmusic.data.repository.toSong
import com.unshoo.pixelmusic.presentation.viewmodel.PlayerViewModel
import com.unshoo.pixelmusic.presentation.viewmodel.PodcastsViewModel
import com.unshoo.pixelmusic.ui.theme.RoundedSans

@Composable
fun PodcastsTab(
    playerViewModel: PlayerViewModel,
    bottomBarHeight: Dp,
    showAddFeedDialog: Boolean,
    onAddFeedDialogDismiss: () -> Unit,
    viewModel: PodcastsViewModel = hiltViewModel()
) {
    val feeds by viewModel.feeds.collectAsState(initial = emptyList())
    val episodes by viewModel.episodes.collectAsState(initial = emptyList())
    val isWorking by viewModel.isWorking.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var feedToRemove by remember { mutableStateOf<PodcastFeedEntity?>(null) }
    val episodesByFeed = remember(episodes) { episodes.groupBy { it.episode.feedId } }

    if (showAddFeedDialog) {
        AddPodcastFeedDialog(
            isWorking = isWorking,
            onDismiss = onAddFeedDialogDismiss,
            onAdd = { url ->
                viewModel.addFeed(url)
                onAddFeedDialogDismiss()
            }
        )
    }

    feedToRemove?.let { feed ->
        AlertDialog(
            onDismissRequest = { feedToRemove = null },
            title = { Text(stringResource(R.string.podcast_feed_remove_title)) },
            text = {
                Text(stringResource(R.string.podcast_feed_remove_message, feed.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFeed(feed)
                        feedToRemove = null
                    }
                ) {
                    Text(stringResource(R.string.podcast_feed_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { feedToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = bottomBarHeight + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "podcast-actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = viewModel::refreshFeeds,
                    enabled = feeds.isNotEmpty() && !isWorking
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.podcast_feed_refresh)
                    )
                }
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.podcast_refreshing),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (errorMessage != null) {
            item(key = "podcast-error") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    onClick = viewModel::clearError
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.podcast_error_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        if (feeds.isEmpty()) {
            item(key = "podcast-empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_headphones_24),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.podcast_feed_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = RoundedSans,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.podcast_feed_empty_message),
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            feeds.forEach { feed ->
                item(key = "feed-${feed.id}") {
                    PodcastFeedHeader(
                        feed = feed,
                        onRemove = { feedToRemove = feed }
                    )
                }
                val feedEpisodes = episodesByFeed[feed.id].orEmpty()
                if (feedEpisodes.isEmpty()) {
                    item(key = "feed-empty-${feed.id}") {
                        Text(
                            text = stringResource(R.string.podcast_episodes_empty),
                            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(feedEpisodes, key = { it.episode.id }) { episode ->
                        PodcastEpisodeCard(
                            episode = episode,
                            onClick = {
                                val songQueue = feedEpisodes.map(PodcastEpisodeWithFeed::toSong)
                                playerViewModel.showAndPlaySong(
                                    song = episode.toSong(),
                                    contextSongs = songQueue,
                                    queueName = feed.title
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastFeedHeader(
    feed: PodcastFeedEntity,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = feed.artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = feed.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            feed.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.podcast_feed_remove),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PodcastEpisodeCard(
    episode: PodcastEpisodeWithFeed,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = episode.episode.artworkUrl ?: episode.feedArtworkUrl,
                contentDescription = stringResource(R.string.podcast_play_episode),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = episode.episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                episode.episode.description?.takeIf(String::isNotBlank)?.let { description ->
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPodcastFeedDialog(
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.podcast_add_feed_title)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.podcast_feed_url_label)) },
                placeholder = { Text(stringResource(R.string.podcast_feed_url_hint)) },
                singleLine = true,
                enabled = !isWorking
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(url) },
                enabled = url.isNotBlank() && !isWorking
            ) {
                Text(stringResource(R.string.podcast_feed_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
