package com.saikou.teraplay.presentation.play

import android.annotation.SuppressLint
import android.app.Application
import android.media.session.MediaSession
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@SuppressLint("UnsafeOptInUsageError")
class PlayerViewModel(
    private val app: Application,
    val player: ExoPlayer,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"

    private val _animeStreamLink = MutableLiveData<String>()

    @SuppressLint("UnsafeOptInUsageError")
    private var simpleCache: SimpleCache? = null
    @SuppressLint("UnsafeOptInUsageError")
    private val databaseProvider = StandaloneDatabaseProvider(app)

    private val savedDone = savedStateHandle.getStateFlow("done", false)
    private val isAutoPlayEnabled = true
    private val isVideoCacheEnabled = true

    val isLoading    = MutableLiveData(true)
    val keepScreenOn = MutableLiveData(false)
    val showSubsBtn  = MutableLiveData(true)
    val playNextEp   = MutableLiveData(false)
    val isError      = MutableLiveData(false)

    var qualityMapUnsorted = mutableMapOf<String, Int>()
    var qualityMapSorted   = mutableMapOf<String, Int>()
    var qualityTrackGroup: TrackGroup? = null

    init {
        // set up player + media session
        player.prepare()
        player.playWhenReady = true
//        mediaSessionConnector.setPlayer(player)

        player.addListener(getCustomPlayerListener())
        player.addAnalyticsListener(object : AnalyticsListener {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onLoadError(
                @SuppressLint("UnsafeOptInUsageError") eventTime: AnalyticsListener.EventTime,
                @SuppressLint("UnsafeOptInUsageError") loadEventInfo: LoadEventInfo,
                @SuppressLint("UnsafeOptInUsageError") mediaLoadData: MediaLoadData,
                error: IOException,
                wasCanceled: Boolean
            ) {
                Log.d("PlayerVM", "onLoadError: ${error.message}, cause=${error.cause}")
            }
        })

        // set up cache
        simpleCache?.release()
        simpleCache = SimpleCache(
            File(app.cacheDir, "exoplayerSourceCache").apply { deleteOnExit() },
            LeastRecentlyUsedCacheEvictor(300L * 1024L * 1024L),
            databaseProvider
        )
    }

    private fun getCustomPlayerListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            isLoading.postValue(
                state == Player.STATE_BUFFERING ||
                        state == Player.STATE_IDLE ||
                        state == Player.STATE_ENDED
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                    // show snack or retry
                }
                ExoPlaybackException.ERROR_CODE_DECODING_FAILED -> {
                    player.stop(); player.prepare(); player.playWhenReady = true
                    isLoading.postValue(false)
                }
                else -> {
                    Toast.makeText(app, "Player Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            keepScreenOn.postValue(isPlaying)
            if (!isPlaying && isAutoPlayEnabled && player.currentPosition >= player.duration) {
                playNextEp.postValue(true)
            }
        }
    }


    fun updateQuality(newUrl: String) {
        val pos = player.currentPosition
        val wasPlaying = player.isPlaying
        viewModelScope.launch {
            _animeStreamLink.postValue(newUrl)
            withContext(Dispatchers.Main) {
                player.setMediaItem(MediaItem.fromUri(newUrl.toUri()))
                player.prepare()
                player.seekTo(pos)
                if (wasPlaying) player.play()
                isLoading.postValue(false)
            }
        }
    }

    fun setAnimeLink(url: String, nextEp: Boolean = false) {
        if (!savedDone.value || nextEp) {
            savedStateHandle["done"] = true
            prepareMediaSource(url)
        }
    }

    private fun prepareMediaSource(url: String) {
        val dsFactory = DefaultHttpDataSource.Factory()
            .setReadTimeoutMs(20_000)
            .setConnectTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        val mediaItem = MediaItem.fromUri(url.toUri())
        val source =             ProgressiveMediaSource.Factory(dsFactory).createMediaSource(mediaItem)

        player.stop()
        player.setMediaSource(source)
        player.prepare()
        showSubsBtn.postValue(false)
        qualityMapUnsorted.clear()
        qualityMapSorted.clear()
        qualityTrackGroup = null
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        simpleCache?.release()
    }
}
