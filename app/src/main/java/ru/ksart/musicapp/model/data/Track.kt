package ru.ksart.musicapp.model.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Track(
    val mediaId: String = "",
    @Json(name = "title")
    val title: String = "",
    @Json(name = "artist")
    val artist: String = "",
    @Json(name = "bitmapUri")
    val bitmapUrl: String = "",
    @Json(name = "duration")
    val duration: Long = -1L, // in ms
    @Json(name = "trackUri")
    val mediaUrl: String = "",
)
