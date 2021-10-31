package ru.ksart.musicapp.ui.player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import ru.ksart.musicapp.databinding.ItemPlayListBinding
import ru.ksart.musicapp.model.data.Track

class PlayListViewHolder(
    private val binding: ItemPlayListBinding,
    private val glide: RequestManager,
    private val onClick: (Track) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun onBind(song: Track) {
        binding.run {
            root.setOnClickListener { onClick(song) }
            artist.text = song.artist
            title.text = song.title
            glide.load(song.bitmapUrl).into(image)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            glide: RequestManager,
            onClick: (Track) -> Unit
        ) = ItemPlayListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).let { PlayListViewHolder(it, glide, onClick) }
    }
}
