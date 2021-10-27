package ru.ksart.musicapp.ui.main

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import ru.ksart.musicapp.R
import ru.ksart.musicapp.ui.player.PlayerViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val viewModel: PlayerViewModel by viewModels()

}
