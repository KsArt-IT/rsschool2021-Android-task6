package ru.ksart.musicapp.model.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import ru.ksart.musicapp.model.data.State

class PlayerServiceConnection(
    context: Context
) {
    private val isConnected = MutableStateFlow<State<Boolean>?>(null)
    private val networkError = MutableStateFlow<State<Boolean>?>(null)

    private val _networkState = Channel<State<Boolean>>()
    val networkState = _networkState.receiveAsFlow()

    private val _connectedState = Channel<State<Boolean>>()
    val connectedState = _connectedState.receiveAsFlow()

    /*
        private val _isConnected = Channel<State<Boolean>>()
        val isConnected = _isConnected.receiveAsFlow()

        private val _networkError = Channel<State<Boolean>>()
        val networkError = _networkError.receiveAsFlow()

    */
    private val _playbackState = MutableStateFlow<PlaybackStateCompat?>(null)
    val playbackState = _playbackState.asStateFlow()

    private val _currentPlayingSong = MutableStateFlow<MediaMetadataCompat?>(null)
    val currentPlayingSong = _currentPlayingSong.asStateFlow()

    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(
            context,
            PlayerForegroundService::class.java
        ),
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    init {
        isConnected
            .filterNotNull()
            .onEach { _connectedState.send(it) }
        networkError
            .filterNotNull()
            .onEach { _networkState.send(it) }
    }

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            isConnected.value = State.Success(true)
        }

        override fun onConnectionSuspended() {
            isConnected.value = State.Error("The connection was suspended", false)
        }

        override fun onConnectionFailed() {
            isConnected.value = State.Error("Couldn't connect to media browser", false)
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.value = state
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _currentPlayingSong.value = metadata
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            when (event) {
                State.NETWORK_ERROR -> networkError.value = State.Error(
                    "Couldn't connect to the server. Please check your internet connection.",
                    null
                )
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}
