package ru.ksart.musicapp.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.ksart.musicapp.model.service.PlayerServiceConnection
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Singleton
    @Provides
    fun providePlayerServiceConnection(
        @ApplicationContext context: Context
    ) = PlayerServiceConnection(context)

}
