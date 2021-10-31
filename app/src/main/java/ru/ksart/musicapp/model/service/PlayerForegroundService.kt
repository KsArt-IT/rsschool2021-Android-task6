package ru.ksart.musicapp.model.service

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.bumptech.glide.RequestManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.ksart.musicapp.model.service.callbacks.MusicPlaybackPreparer
import ru.ksart.musicapp.model.service.callbacks.MusicPlayerListener
import ru.ksart.musicapp.model.service.callbacks.MusicPlayerNotificationListener
import ru.ksart.musicapp.model.service.data.MusicSource
import ru.ksart.musicapp.model.service.notification.MusicNotificationManager
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PlayerForegroundService : MediaBrowserServiceCompat() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    private var isPlayerInitialized = false
    private var currentPlayingSong: MediaMetadataCompat? = null
    private val playerListener by lazy { MusicPlayerListener(this) }

    @Inject
    lateinit var musicSource: MusicSource

    @Inject
    lateinit var glide: RequestManager

    // инициализируется при создании сервиса
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var notificationManager: MusicNotificationManager

    var isForegroundService = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("service on: onCreate")

        // подготовка play list
        coroutineScope.launch {
            Timber.d("service on: load fetchMediaData")
            musicSource.fetchMediaData()
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(getPendingIntent())
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        notificationManager = MusicNotificationManager(
            this,
            glide,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this),
        ) {
            currentSongDuration = if (exoPlayer.duration > 0) exoPlayer.duration else 0
            Timber.d("service on: notification duration=$currentSongDuration")
        }
        exoPlayer.addListener(playerListener)

        val musicPlaybackPreparer = MusicPlaybackPreparer(musicSource) {
            currentPlayingSong = it
            preparePlayer()
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(musicPlaybackPreparer)
            setQueueNavigator(MusicQueueNavigator())
            setPlayer(exoPlayer)
        }

        notificationManager.showNotification(exoPlayer)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot = BrowserRoot(MEDIA_ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent = musicSource.whenReady { isInitialized ->
                    if (isInitialized) {
                        val mediaItems = musicSource.asMediaItems().toMutableList()
                        result.sendResult(mediaItems)
                        if (!isPlayerInitialized && mediaItems.size > 0) {
                            preparePlayer(index = 0, playNow = false)
                            isPlayerInitialized = true
                        }
                    } else result.sendResult(null)
                }
                if (!resultsSent) result.detach()
            }
            EMPTY_MEDIA_ROOT_ID -> {
                result.sendResult(null)
            }
            else -> result.sendError(null)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
        notificationManager.removeNotification()
    }

    override fun onDestroy() {
        Timber.d("service on: onDestroy")
        coroutineScope.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }

    private fun preparePlayer(index: Int = -1, playNow: Boolean = true) {
        val currentSongIndex = if (index >= 0) index
        else currentPlayingSong?.let { musicSource.getMediaIndex(it) } ?: 0
        exoPlayer.setMediaSource(musicSource.asMediaSource(dataSourceFactory))
        exoPlayer.prepare()
        exoPlayer.seekTo(currentSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }

    private fun getPendingIntent(): PendingIntent? = packageManager
        ?.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }?.let {
            PendingIntent.getActivity(this, 0, it, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
        }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return musicSource.getMediaDescriptionByIndex(windowIndex)
        }
    }

    companion object {
        private const val SERVICE_TAG = "MusicService"
        const val MEDIA_ROOT_ID = "media_root_id"
        private const val EMPTY_MEDIA_ROOT_ID = "empty_media_root_id"

        var currentSongDuration = 0L
            private set
    }
}
