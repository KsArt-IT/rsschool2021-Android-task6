package ru.ksart.musicapp.ui.player.adapter

import androidx.recyclerview.widget.DiffUtil
import ru.ksart.musicapp.model.data.Track

class PlayListDiffCallback : DiffUtil.ItemCallback<Track>() {

    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.mediaId == newItem.mediaId
    }

    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
    }
}
