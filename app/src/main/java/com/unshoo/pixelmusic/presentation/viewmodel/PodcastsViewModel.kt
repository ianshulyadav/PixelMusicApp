package com.unshoo.pixelmusic.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unshoo.pixelmusic.data.database.PodcastEpisodeWithFeed
import com.unshoo.pixelmusic.data.database.PodcastFeedEntity
import com.unshoo.pixelmusic.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository
) : ViewModel() {
    val feeds = podcastRepository.feeds
    val episodes = podcastRepository.episodes

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun addFeed(url: String) {
        performOperation { podcastRepository.addFeed(url) }
    }

    fun refreshFeeds() {
        performOperation { podcastRepository.refreshFeeds() }
    }

    fun removeFeed(feed: PodcastFeedEntity) {
        performOperation { podcastRepository.removeFeed(feed.id) }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun performOperation(operation: suspend () -> Unit) {
        if (_isWorking.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isWorking.value = true
            _errorMessage.value = null
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "An unexpected podcast error occurred."
            } finally {
                _isWorking.value = false
            }
        }
    }
}
