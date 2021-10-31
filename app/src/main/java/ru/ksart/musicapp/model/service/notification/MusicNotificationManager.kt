package ru.ksart.musicapp.model.service.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import ru.ksart.musicapp.R

class MusicNotificationManager(
    private val context: Context,
    private val glide: RequestManager,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback: () -> Unit
) {

    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        // v.2.15 - public Builder(Context context,int notificationId,String channelId)
        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID,
            PLAY_CHANNEL_ID,
        )
            .setChannelNameResourceId(R.string.notification_channel_name)
            .setChannelDescriptionResourceId(R.string.notification_channel_description)
            .setSmallIconResourceId(R.drawable.ic_music_24)
            .setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            .setNotificationListener(notificationListener)
            .build().apply {
                setMediaSessionToken(sessionToken)
            }
    }

    fun showNotification(player: Player) {
        notificationManager.setPlayer(player)
    }

    fun removeNotification() {
        notificationManager.setPlayer(null)
    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        private var bitmap: Bitmap? = null
        private var uri: Uri? = null

        override fun getCurrentContentTitle(player: Player): CharSequence {
            newSongCallback()
            return mediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence {
            return mediaController.metadata.description.subtitle.toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            if (uri != mediaController.metadata.description.iconUri) {
                uri = mediaController.metadata.description.iconUri
                glide.asBitmap()
                    .load(mediaController.metadata.description.iconUri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            if (bitmap != resource) {
                                bitmap = resource
                                callback.onBitmap(resource)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) = Unit
                    })
            }
            return bitmap
        }
    }

    companion object {
        const val NOTIFICATION_ID = 153_123
        private const val PLAY_CHANNEL_ID = "play_media_content"
    }
}
