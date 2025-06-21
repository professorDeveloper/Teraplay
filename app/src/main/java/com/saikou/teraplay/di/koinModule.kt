// AppModule.kt (or KoinModule.kt)
package com.saikou.teraplay.di

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.room.Room
import com.saikou.teraplay.broadcast.DownloadReceiver
import com.saikou.teraplay.data.local.room.database.AppDatabase
import com.saikou.teraplay.data.repository.DownloadRepositoryImpl
import com.saikou.teraplay.domain.repository.DownloadRepository
import com.saikou.teraplay.presentation.home.HomeViewModel
import com.saikou.teraplay.presentation.play.PlayerViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.io.File

@SuppressLint("UnsafeOptInUsageError")
val appModule = module {
    // repository interface → implementation
    single<DownloadRepository> {
        DownloadRepositoryImpl(apiService = get())
    }

    // ViewModel depends only on the interface
    viewModel {
        HomeViewModel(repo = get())
    }

//        single {
//            SimpleCache(
//                File(androidContext().cacheDir, "exoplayerSourceCache").apply { deleteOnExit() },
//                LeastRecentlyUsedCacheEvictor(300L * 1024L * 1024L),
//                StandaloneDatabaseProvider(androidContext())
//            333
//        }
        single {
            DefaultHttpDataSource.Factory()
                .setReadTimeoutMs(20_000)
                .setConnectTimeoutMs(20_000)
                .setAllowCrossProtocolRedirects(true)
        }

        single<ExoPlayer> {
            val app: Application = androidApplication()
            val renderersFactory: RenderersFactory =
                DefaultRenderersFactory(app).setEnableDecoderFallback(true)
            val trackSelector = DefaultTrackSelector(app).apply {
                parameters = buildUponParameters()
                    .setMaxVideoBitrate(5_000_000)
                    .build()
            }
            ExoPlayer.Builder(app, renderersFactory)
                .setSeekForwardIncrementMs(10_000)
                .setSeekBackIncrementMs(10_000)
                .setTrackSelector(trackSelector)
                .build()
        }

        viewModel { (savedStateHandle: SavedStateHandle) ->
            PlayerViewModel(
                app = androidApplication(),
                player = get(),
//                repository = get(),
                savedStateHandle = savedStateHandle
            )
        }
    single { DownloadReceiver() }
    single { provideDatabase(androidContext()) }
}

fun provideDatabase(context: Context) = Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "teraplay-db"
).build()
