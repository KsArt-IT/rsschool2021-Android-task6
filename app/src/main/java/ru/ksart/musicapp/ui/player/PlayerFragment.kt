package ru.ksart.musicapp.ui.player

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.ksart.musicapp.R
import ru.ksart.musicapp.databinding.FragmentPlayerBinding
import ru.ksart.musicapp.model.data.State
import ru.ksart.musicapp.model.service.isPlaying
import ru.ksart.musicapp.ui.player.adapter.PlayListAdapter
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PlayerFragment : Fragment(R.layout.fragment_player) {

    private val binding by viewBinding(FragmentPlayerBinding::bind)

    private val viewModel: PlayerViewModel by activityViewModels()

    private val playListAdapter
        get() = checkNotNull(binding.playList.adapter as? PlayListAdapter) { "adapter isn`t initialized" }

    private var shouldUpdateSeekbar = true

    private var currentMediaId = ""

    @Inject
    lateinit var glide: RequestManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAdapter()
        bindViewModel()
        initListener()
    }

    private fun initAdapter() {
        binding.playList.run {
            adapter = PlayListAdapter(glide, viewModel::playOrToggleSong)
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        }
    }

    private fun bindViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.trackList.collectLatest { result ->
                        when (result) {
                            is State.Success -> {
                                binding.progress.isVisible = false
                                result.data?.let { playList ->
                                    playListAdapter.submitList(playList)
                                    if (playList.isNotEmpty()) {
                                        val song = playList[0]
                                        updateTitleAndSongImage(
                                            mediaId = song.mediaId,
                                            artist = song.artist,
                                            title = song.title,
                                            bitmapUrl = song.bitmapUrl
                                        )
                                    }
                                }
                            }
                            is State.Loading -> {
                                binding.progress.isVisible = true
                            }
                            is State.Error -> Unit
                        }
                    }
                }
                launch {
                    viewModel.currentPlayingSong.collectLatest { song ->
                        song?.description?.run {
                            updateTitleAndSongImage(
                                mediaId = mediaId ?: "",
                                artist = subtitle ?: "",
                                title = title ?: "",
                                bitmapUrl = iconUri.toString()
                            )
                        }
                    }
                }
                launch {
                    viewModel.playbackState.collectLatest(::setButtonState)
                }
                launch {
                    viewModel.currentPlayingSongPosition.collectLatest { (duration, position) ->
                        setSeekBarPosition(max = duration, position = position)
                    }
                }
                launch {
                    viewModel.connectedState.collectLatest { state ->
                        when (state) {
                            is State.Error -> {
                                Snackbar.make(
                                    binding.root,
                                    state.message,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            else -> Unit
                        }
                    }
                }
                launch {
                    viewModel.networkState.collectLatest { state ->
                        when (state) {
                            is State.Error -> {
                                Snackbar.make(
                                    binding.root,
                                    state.message,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun initListener() {
        binding.run {
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    shouldUpdateSeekbar = false
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        viewModel.seekTo(it.progress.toLong())
                        shouldUpdateSeekbar = true
                    }
                }
            })
            playButton.setOnClickListener { viewModel.playOrToggleCurrentSong(true) }
            pauseButton.setOnClickListener { viewModel.playOrToggleCurrentSong(true) }
            stopButton.setOnClickListener { viewModel.stopCurrentSong() }
            nextButton.setOnClickListener { viewModel.skipToNextSong() }
            prevButton.setOnClickListener { viewModel.skipToPreviousSong() }
        }
    }

    private fun updateTitleAndSongImage(
        mediaId: String,
        artist: CharSequence,
        title: CharSequence,
        bitmapUrl: String
    ) {
        if (currentMediaId == mediaId) return
        currentMediaId = mediaId
        binding.run {
            glide.load(bitmapUrl)
                .into(songImage)
            songArtist.text = artist
            songTitle.text = title
        }
    }

    private fun setButtonState(state: PlaybackStateCompat?) {
        binding.run {
            if (state == null) {
                playButton.isEnabled = false
                pauseButton.isEnabled = false
                stopButton.isEnabled = false
                prevButton.isEnabled = false
                nextButton.isEnabled = false
            } else {
                playButton.isEnabled = state.isPlaying.not()
                pauseButton.isEnabled = state.isPlaying
                stopButton.isEnabled = state.isPlaying
                prevButton.isEnabled = true
                nextButton.isEnabled = true
            }
            viewModel.trackPosition(playButton.isEnabled.not())
        }
    }

    private fun setSeekBarPosition(max: Long = -1, position: Long) {
        Timber.d("PlayerFragment: seekBar max=$max progress=$position")
        if (max >= 0) binding.seekBar.max = max.toInt()
        if (shouldUpdateSeekbar) binding.seekBar.progress = position.toInt()
    }
}
