package ru.ksart.musicapp.model.service

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.MediaBrowserServiceCompat
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
class PlayerForegroundService() : MediaBrowserServiceCompat() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    private var isPlayerInitialized = false
    private var currentPlayingSong: MediaMetadataCompat? = null
    private val playerListener by lazy { MusicPlayerListener(this) }

    // источник музыки
    @Inject
    lateinit var musicSource: MusicSource

    // инициализируется при создании сервиса
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var notificationManager: MusicNotificationManager

    var isForegroundService = false
    private var isServiceStarted = false

    private val stateBuilder by lazy {
        PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
    }

/*
    private var _audioManager: AudioManager? = null
    private val audioManager get() = checkNotNull(_audioManager) { "Audio Manager isn`t initialized" }
*/

    private var audioFocusRequested = false
    private var _audioFocusRequest: AudioFocusRequestCompat? = null
    private val audioFocusRequest
        get() = checkNotNull(_audioFocusRequest) { "Audio Focus Request isn`t initialized" }

    override fun onCreate() {
        super.onCreate()
        Timber.d("service on: onCreate")

        // подготовка play list
        coroutineScope.launch {
            Timber.d("service on: load fetchMediaData")
            musicSource.fetchMediaData()
        }

/*
        if (isAndroid8) {
            val audioAttributes = AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .build()
            _audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
//                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(true)
                .setAudioAttributes(audioAttributes)
                .build();
        }
*/
//        _audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
/*
        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        Timber.d("service: notificationManager=$notificationManager")
*/

/*
        val appContext = applicationContext

        val mediaButtonIntent = Intent(
            Intent.ACTION_MEDIA_BUTTON, null, appContext,
            MediaButtonReceiver::class.java
        )
        mediaSession = MediaSessionCompat(baseContext, getString(R.string.service_name)).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(stateBuilder.build())
            setCallback(mediaSessionCallback)
            setSessionActivity(getPendingIntent())
            setMediaButtonReceiver(
                PendingIntent.getBroadcast(
                    appContext,
                    0,
                    mediaButtonIntent,
                    0
                )
            )
        }
*/
        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(getPendingIntent())
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        notificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
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
                    } else {
                        result.sendResult(null)
                    }
                }
                if (!resultsSent) {
                    result.detach()
                }
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

/*
    private val audioFocusChangeListener: OnAudioFocusChangeListener =
        OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> mediaSessionCallback.onPlay() // Не очень красиво
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaSessionCallback.onPause()
                else -> mediaSessionCallback.onPause()
            }
        }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // Disconnecting headphones - stop playback
            if ((AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action)) {
                mediaSessionCallback.onPause()
            }
        }
    }
*/

/*
    private fun getBitmapFromUrl(url: String): Bitmap? {
        Timber.d("service getBitmapFromUrl")
        var bitmap: Bitmap? = null
        val request = ImageRequest.Builder(this)
            .data(url)
            .target { result ->
                bitmap = (result as BitmapDrawable).bitmap
            }
            .build()
        val disposable = imageLoader.enqueue(request)
        return bitmap
    }
*/

    private fun getPendingIntent(): PendingIntent? = packageManager
        ?.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }?.let {
            PendingIntent.getActivity(this, 0, it, FLAG_UPDATE_CURRENT)
        }

/*
    private fun moveToStartedState() {
        Timber.d("service moveToStartedState")
        if (isAndroid8) {
            startForegroundService(Intent(this, PlayerForegroundService::class.java))
        } else {
            startService(Intent(this, PlayerForegroundService::class.java))
        }
    }
*/

/*
    fun refreshNotificationAndForegroundStatus(playbackState: Int) {
        Timber.d("service refreshNotificationAndForegroundStatus")
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                NotificationManagerCompat.from(this)
                    .notify(NOTIFICATION_ID, getNotification(playbackState))
                stopForeground(false)
            }
            else -> {
                stopForeground(true)
            }
        }
    }
*/

/*
    private fun getNotification(playbackState: Int): Notification {
        Timber.d("service: getNotification")
        val builder = MediaStyleHelper.from(this, mediaSession)
        builder.addAction(
            Action(
                android.R.drawable.ic_media_previous,
                getString(R.string.prev_button_text),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
        )
        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(
                Action(
                    android.R.drawable.ic_media_pause,
                    getString(R.string.pause_button_text),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
        } else {
            builder.addAction(
                Action(
                    android.R.drawable.ic_media_play,
                    getString(R.string.play_button_text),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
        }
        builder.addAction(
            Action(
                android.R.drawable.ic_media_next,
                getString(R.string.next_button_text),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
        )
        builder.setStyle(
            NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setMediaSession(mediaSession.sessionToken) // setMediaSession требуется для Android Wear
        )
        builder.color = getThemeColor(com.google.android.material.R.attr.colorPrimaryVariant)
        builder.setShowWhen(false)
        builder.setOnlyAlertOnce(true)
        return builder.build()
    }
*/

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return musicSource.getMediaDescriptionByIndex(windowIndex)
        }
    }

    companion object {
        private const val SERVICE_TAG = "MusicService"
        const val MEDIA_ROOT_ID = "media_root_id"
        private const val EMPTY_MEDIA_ROOT_ID = "empty_media_root_id"

        private const val PLAY_URI = "play_uri_file"

        const val PLAY_LIST = "play_list"

        const val COMMAND_ID = "COMMAND_ID"
        const val COMMAND_START = "COMMAND_START"
        const val COMMAND_STOP = "COMMAND_STOP"

        var currentSongDuration = 0L
            private set

    }
}
/*
Gen0ciD (Алекс, PC.Home) — Вчера, в 21:06
Кто там спрашивал - как запускать следующий трек по нажатию кнопки:
---
private fun nextSongs() {
    musicMediaPlayer?.run {
      stop()
      release()
    }
    setNextSongs()
    startMusic()
  }
  private fun startMusic() {
    initializeMediaPlayer()
    musicMediaPlayer?.start()
  }
  ---
  yaromchikV (Vladislav) — Сегодня, в 0:46
Там есть OnCompletionListener
 */
