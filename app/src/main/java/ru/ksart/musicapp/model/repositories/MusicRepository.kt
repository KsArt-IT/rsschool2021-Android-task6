package ru.ksart.musicapp.model.repositories

import ru.ksart.musicapp.model.data.Track

interface MusicRepository {

    suspend fun loadTracks(): List<Track>
}
