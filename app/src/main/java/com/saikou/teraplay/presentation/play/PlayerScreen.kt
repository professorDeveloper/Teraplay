package com.saikou.teraplay.presentation.play

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerControlView
import androidx.navigation.fragment.navArgs
import com.saikou.teraplay.R
import com.saikou.teraplay.data.local.UserPreferenceManager
import com.saikou.teraplay.databinding.ContentControllerBinding
import com.saikou.teraplay.databinding.PlayerBottomSheetBinding
import com.saikou.teraplay.databinding.PlayerScreenBinding
import com.saikou.teraplay.utils.gone
import com.saikou.teraplay.utils.ignoreAllSSLErrors
import com.saikou.teraplay.utils.snackString
import com.saikou.teraplay.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class PlayerScreen : Fragment() {

    private var _binding: PlayerScreenBinding? = null
    private val binding get() = _binding!!

    private val PlayerControlView.binding
        @OptIn(UnstableApi::class) get() = ContentControllerBinding.bind(this.findViewById(R.id.exo_controller))

    private val args by navArgs<PlayerScreenArgs>()
    private lateinit var player: ExoPlayer
    private lateinit var httpDataSource: HttpDataSource.Factory
    private lateinit var dataSourceFactory: DataSource.Factory
//    private val model by viewModel<PlayViewModel>()
    private val userPreferenceManager by lazy { UserPreferenceManager(requireContext()) }
    private lateinit var mediaSession: MediaSession

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = PlayerScreenBinding.inflate(inflater, container, false)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().hideSystemBars()
//        val window = requireActivity().window
//        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.pvPlayer.controller.binding.exoAnimeTitle.text = args.args.fileName
        initializeVideo()
        displayVideo()

    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (player.currentPosition > 10) {
            lifecycleScope.launch {
            }
        }
        if (::player.isInitialized) {
            player.release()
            mediaSession.release()
        }
        _binding = null
    }


    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) {
            player.pause()
        }
    }

    override fun onResume() {
        super.onResume()
    }


    @SuppressLint("WrongConstant")
    @OptIn(UnstableApi::class)
    private fun initializeVideo() {

        dataSourceFactory =
            DefaultDataSource.Factory(requireContext())
        val renderersFactory =
            DefaultRenderersFactory(requireContext()).setEnableDecoderFallback(true)
                .setMediaCodecSelector(MediaCodecSelector.DEFAULT).setEnableAudioFloatOutput(false)
        httpDataSource = DefaultHttpDataSource.Factory()
        player = ExoPlayer.Builder(requireContext(), renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setRenderersFactory(renderersFactory).setVideoChangeFrameRateStrategy(
                C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
            ).build().also { player ->
                player.setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                    true,
                )
                mediaSession = MediaSession.Builder(requireContext(), player).build()

            }

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
//                Bugsnag.notify(error)
            }
        })


//        binding.pvPlayer.controller.binding.exoNextTenContainer.setOnClickListener {
//            player.seekTo(player.currentPosition + 10_000)
//        }
//        binding.pvPlayer.controller.binding.exoPrevTenContainer.setOnClickListener {
//            player.seekTo(player.currentPosition - 10_000)
//        }

        binding.pvPlayer.player = player
        binding.pvPlayer.controller.binding.exoPlay.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                binding.pvPlayer.controller.binding.exoPlay.setImageResource(R.drawable.anim_play_to_pause)
            } else {
                player.play()
                binding.pvPlayer.controller.binding.exoPlay.setImageResource(R.drawable.anim_pause_to_play)
            }

        }

        binding.pvPlayer.controller.findViewById<ExtendedTimeBar>(androidx.media3.ui.R.id.exo_progress)
            .setKeyTimeIncrement(10_000)

    }

    @OptIn(UnstableApi::class)
    private fun displayVideo() {
        lifecycleScope.launch {
            val videoUrl = args.args.directLink
            val mediaItem = MediaItem.Builder().setUri(videoUrl).build()
            val mediaSource =
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.setMediaItem(mediaItem)

//            if (model.isWatched) {
//                player.seekTo(lastPosition)
//            }
            player.prepare()
            player.play()
            binding.apply {
                binding.pvPlayer.doubleTapOverlay = binding.doubleTapOverlay
                pvPlayer.setLongPressListenerEvent {
                    val currentSpeed = player.playbackParameters.speed
                    if (currentSpeed == 1f && player.playWhenReady) {
                        val params = PlaybackParameters(2f)
                        player.playbackParameters = params
                        snackString("Speed 2x", requireActivity())
                    }
                }




                pvPlayer.setActionUpListener {
                    val currentSpeed = player.playbackParameters.speed
                    if (currentSpeed == 2f) {
                        val params = PlaybackParameters(1f)
                        player.playbackParameters = params
                        snackString("Speed 1x", requireActivity())
                    }
                }

            }

//            if (model.isWatched && model.getWatchedHistoryEntity != null && model.getWatchedHistoryEntity!!.lastPosition > 0 && !model.doNotAsk) {
//                player.pause()
//                val dialog = AlertPlayerDialog(model.getWatchedHistoryEntity!!)
//                dialog.setNoClearListener {
//                    lifecycleScope.launch {
//                        dialog.dismiss()
//                        model.removeHistory(args.id)
//                        withContext(Dispatchers.Main) {
//                            player.seekTo(0)
//                            player.play()
//                        }
//                    }
//                }
//                dialog.setYesContinueListener {
//                    dialog.dismiss()
//                    player.play()
//                }
//                dialog.show(parentFragmentManager, "ConfirmationDialog")
//
//            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    class ExtendedTimeBar(
        context: Context, attrs: AttributeSet?
    ) : androidx.media3.ui.DefaultTimeBar(context, attrs) {

        private var previewBitmap: Bitmap? = null
        private val previewPaint = Paint().apply { isFilterBitmap = true }
        private var videoDuration: Long = 0L
        private var videoPosition: Long = 0L
        private var enabled = false
        private var forceDisabled = false

        override fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
            super.setEnabled(!forceDisabled && this.enabled)
        }

        fun setForceDisabled(forceDisabled: Boolean) {
            this.forceDisabled = forceDisabled
            isEnabled = enabled
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            if (videoDuration > 0) {
                val relativePos = videoPosition.toFloat() / videoDuration.toFloat()
                val previewWidth = previewBitmap?.width ?: 100
                val previewHeight = previewBitmap?.height ?: 60
                val previewX = (relativePos * width - previewWidth / 2).toInt()
                val previewY = height - previewHeight - 20 // Adjust for padding

                previewBitmap?.let {
                    canvas.drawBitmap(it, previewX.toFloat(), previewY.toFloat(), previewPaint)
                }
            }
        }
    }


    fun Activity.hideSystemBars() {
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }
}