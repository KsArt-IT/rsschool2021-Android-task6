package ru.ksart.musicapp.model.repositories

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.ksart.musicapp.model.data.Track
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MusicRepository {

    override suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        Timber.d("MusicRepositoryImpl: loadTracks")
        try {
            val jsonString = context.resources.assets.open("playlist.json")
                .bufferedReader()
                .use {
                    it.readText()
                }
            Timber.d("MusicRepositoryImpl: list jsonString=$jsonString")
            convertJsonListToTrackInstance(jsonString)
        } catch (e: IOException) {
            emptyList()
        }
    }

    private fun convertJsonListToTrackInstance(jsonString: String): List<Track> {
        return try {
            val moshi = Moshi.Builder()
                .build()
            val movieListType = Types.newParameterizedType(
                List::class.java,
                Track::class.java
            )
            val adapter = moshi.adapter<List<Track>>(movieListType).nonNull()
            adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: IOException) {
            Timber.e(e, "list error")
            emptyList()
        }
    }
}
