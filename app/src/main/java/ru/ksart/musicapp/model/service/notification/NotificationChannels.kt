package ru.ksart.musicapp.model.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import ru.ksart.musicapp.utils.isAndroid8

object NotificationChannels {

    const val NOTIFICATION_ID = 153123
    const val PLAY_CHANNEL_ID = "play_media_content"
    private const val PLAY_CHANNEL_NAME = "Play"
    private const val PLAY_CHANNEL_DESCRIPTION = "Play media"

    fun create(context: Context) {
        // создаем каналы для Android 8+ (O)
        if (isAndroid8) {
            createPlayChannel(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPlayChannel(context: Context) {
        val channel = NotificationChannel(
            PLAY_CHANNEL_ID,
            PLAY_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = PLAY_CHANNEL_DESCRIPTION
            // включим вибрацию
//            enableVibration(true)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

}
