package ru.ksart.musicapp.model.service.data

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import ru.ksart.musicapp.model.data.PlayListState
import ru.ksart.musicapp.model.repositories.MusicRepository
import timber.log.Timber
import javax.inject.Inject

class PlayListMusicSourceImpl @Inject constructor(
    private val repository: MusicRepository
) : MusicSource {

    private var songs = emptyList<MediaMetadataCompat>()

    override fun getMediaByIdOrNull(mediaId: String): MediaMetadataCompat? {
        return songs.firstOrNull { it.description.mediaId == mediaId }
    }

    @NotNull
    override fun getMediaDescriptionByIndex(index: Int): MediaDescriptionCompat {
        return songs[index].description
    }

    override fun getMediaIndex(media: MediaMetadataCompat?) = media?.let { songs.indexOf(it) } ?: -1

    override suspend fun fetchMediaData() = withContext(Dispatchers.IO) {
        Timber.d("PlayListMusicSourceImpl: fetchMediaData STATE_INITIALIZING")
        state = PlayListState.STATE_INITIALIZING
        val allSongs = repository.loadTracks()
        songs = if (allSongs.isNotEmpty()) {
            allSongs.map { song ->
                MediaMetadataCompat.Builder()
                    .putString(METADATA_KEY_ARTIST, song.artist)
                    .putString(METADATA_KEY_MEDIA_ID, song.mediaUrl)
                    .putString(METADATA_KEY_TITLE, song.title)
                    .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                    .putString(METADATA_KEY_DISPLAY_ICON_URI, song.bitmapUrl)
                    .putString(METADATA_KEY_MEDIA_URI, song.mediaUrl)
                    .putString(METADATA_KEY_ALBUM_ART_URI, song.bitmapUrl)
                    .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
                    .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.artist)
                    .build()
            }
        } else emptyList()
        state = PlayListState.STATE_INITIALIZED
        Timber.d("PlayListMusicSourceImpl: fetchMediaData STATE_INITIALIZED")
    }

    override fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.fromUri(
                        song.getString(METADATA_KEY_MEDIA_URI).toUri()
                    )
                )
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    override fun asMediaItems() = songs.map { song ->
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .build()
        MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
    }

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: PlayListState = PlayListState.STATE_CREATED
        set(value) {
            if (value == PlayListState.STATE_INITIALIZED || value == PlayListState.STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == PlayListState.STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    override fun whenReady(action: (Boolean) -> Unit): Boolean {
        return if (state == PlayListState.STATE_CREATED || state == PlayListState.STATE_INITIALIZING) {
            onReadyListeners += action
            false
        } else {
            action(state == PlayListState.STATE_INITIALIZED)
            true
        }
    }
}
