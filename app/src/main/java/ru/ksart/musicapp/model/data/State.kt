package ru.ksart.musicapp.model.data

sealed class State<out T>() {

    data class Success<T>(val data: T?) : State<T>()

    data class Loading<T>(val data: T?) : State<T>()

    data class Error<T>(val message: String, val data: T?) : State<T>()

    companion object {
        const val NETWORK_ERROR = "NETWORK_ERROR"
    }
}
