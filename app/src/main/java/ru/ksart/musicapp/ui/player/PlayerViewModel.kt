package ru.ksart.musicapp.ui.player

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.ksart.musicapp.model.data.State
import ru.ksart.musicapp.model.data.Track
import ru.ksart.musicapp.model.service.PlayerForegroundService
import ru.ksart.musicapp.model.service.PlayerServiceConnection
import ru.ksart.musicapp.model.service.currentPlaybackPosition
import ru.ksart.musicapp.model.service.isPlayEnabled
import ru.ksart.musicapp.model.service.isPlaying
import ru.ksart.musicapp.model.service.isPrepared
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerServiceConnection: PlayerServiceConnection
) : ViewModel() {

    private val _trackList = MutableStateFlow<State<List<Track>>>(State.Loading(null))
    val trackList = _trackList.asStateFlow()

    private val _currentPlayingSongPosition = MutableStateFlow<Pair<Long, Long>>(0L to 0L)
    val currentPlayingSongPosition = _currentPlayingSongPosition.asStateFlow()

    val connectedState = playerServiceConnection.connectedState
    val networkState = playerServiceConnection.networkState
    val currentPlayingSong = playerServiceConnection.currentPlayingSong
    val playbackState = playerServiceConnection.playbackState

    private val isPlayerPrepared get() = playbackState.value?.isPrepared ?: false

    private var jobPosition: Job? = null

    init {
        playerServiceConnection.subscribe(
            PlayerForegroundService.MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    super.onChildrenLoaded(parentId, children)
                    val items = children.map {
                        Track(
                            mediaId = it.mediaId ?: "",
                            title = it.description.title.toString(),
                            artist = it.description.subtitle.toString(),
                            bitmapUrl = it.description.iconUri.toString(),
                            duration = 0,
                            mediaUrl = it.description.mediaUri.toString()
                        )
                    }
                    _trackList.value = State.Success(items)
                }
            }
        )
    }

    fun skipToNextSong() {
        playerServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        playerServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos: Long) {
        playerServiceConnection.transportControls.seekTo(pos)
    }

    private fun playOrPauseCurrentSong(toggle: Boolean = false) {
        playbackState.value?.let { state ->
            when {
                state.isPlaying -> if (toggle) playerServiceConnection.transportControls.pause()
                state.isPlayEnabled -> playerServiceConnection.transportControls.play()
                else -> Unit
            }
        }
    }

    fun playOrToggleCurrentSong(toggle: Boolean = false) {
        if (isPlayerPrepared && currentPlayingSong.value != null) {
            playOrPauseCurrentSong(toggle)
        }
    }

    fun playOrToggleSong(mediaItem: Track, toggle: Boolean = false) {
        if (isPlayerPrepared &&
            mediaItem.mediaId == currentPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)
        ) playOrPauseCurrentSong(toggle)
        else playerServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId, null)
    }

    fun stopCurrentSong() {
        if (isPlayerPrepared && currentPlayingSong.value != null) {
            // stop() - останавливает сервис
            playerServiceConnection.transportControls.pause()
            seekTo(0)
        }
    }

    fun trackPosition(track: Boolean) {
        jobPosition?.takeIf { it.isActive }?.cancel()
        jobPosition = viewModelScope.launch(Dispatchers.Default) {
            do {
                _currentPlayingSongPosition.value = PlayerForegroundService.currentSongDuration to (
                    playbackState.value?.currentPlaybackPosition ?: 0
                    )
                Timber.d(
                    "PlayerViewModel: position=${_currentPlayingSongPosition.value.second}" +
                        " duration=${_currentPlayingSongPosition.value.first}"
                )
                delay(Companion.UPDATE_PLAYER_POSITION_INTERVAL)
            } while (track)
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobPosition?.takeIf { it.isActive }?.cancel()
        playerServiceConnection.unsubscribe(
            PlayerForegroundService.MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
    }

    companion object {
        private const val UPDATE_PLAYER_POSITION_INTERVAL = 250L
    }
}
