package ru.ksart.musicapp.model.service.data

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

interface MusicSource {

//    val size: Int

    suspend fun fetchMediaData()

    fun whenReady(action: (Boolean) -> Unit): Boolean

    fun getMediaByIdOrNull(mediaId: String): MediaMetadataCompat?

    fun getMediaDescriptionByIndex(index: Int): MediaDescriptionCompat

    fun getMediaIndex(media: MediaMetadataCompat?): Int

    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource

    fun asMediaItems(): List<MediaBrowserCompat.MediaItem>
}
