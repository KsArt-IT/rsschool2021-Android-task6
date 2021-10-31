package ru.ksart.musicapp.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import ru.ksart.musicapp.model.repositories.MusicRepository
import ru.ksart.musicapp.model.repositories.MusicRepositoryImpl
import ru.ksart.musicapp.model.service.data.MusicSource
import ru.ksart.musicapp.model.service.data.PlayListMusicSourceImpl

@Module
@InstallIn(ServiceComponent::class)
interface RepositoryModule {

    @ServiceScoped
    @Binds
    fun provideMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @ServiceScoped
    @Binds
    fun provideMusicSource(impl: PlayListMusicSourceImpl): MusicSource
}
