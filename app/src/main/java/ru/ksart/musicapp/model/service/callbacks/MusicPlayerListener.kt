package ru.ksart.musicapp.model.service.callbacks

import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import ru.ksart.musicapp.model.service.PlayerForegroundService

class MusicPlayerListener(
    private val musicService: PlayerForegroundService
) : Player.Listener {
    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        super.onSeekBackIncrementChanged(seekBackIncrementMs)
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        super.onSeekForwardIncrementChanged(seekForwardIncrementMs)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
    }
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(
            musicService,
            "An unknown error has occurred",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            ExoPlayer.STATE_ENDED -> Unit //                mediaSessionCallback.onSkipToNext()
            Player.STATE_READY -> musicService.stopForeground(false)
            else -> Unit
        }
    }
}
