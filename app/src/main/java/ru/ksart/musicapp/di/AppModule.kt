package ru.ksart.musicapp.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.ksart.musicapp.R
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

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(R.drawable.ic_placeholder_24)
            .error(R.drawable.ic_placeholder_24)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )
}
