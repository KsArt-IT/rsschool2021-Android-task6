package ru.ksart.musicapp.ui.player.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.RequestManager
import ru.ksart.musicapp.model.data.Track

class PlayListAdapter(
    private val glide: RequestManager,
    private val onItemClickListener: (Track) -> Unit
) : ListAdapter<Track, PlayListViewHolder>(PlayListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        return PlayListViewHolder.create(parent, glide, onItemClickListener)
    }

    override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}
