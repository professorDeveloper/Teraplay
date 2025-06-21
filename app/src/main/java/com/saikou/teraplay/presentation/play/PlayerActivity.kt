package com.saikou.teraplay.presentation.play

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.saikou.teraplay.R
import com.saikou.teraplay.custom.PlayerView
import com.saikou.teraplay.data.models.DownloadResponse
import com.saikou.teraplay.databinding.ActivityPlayerBinding
import com.saikou.teraplay.utils.CustomAdapter
import com.saikou.teraplay.utils.hideSystemBars
import com.saikou.teraplay.utils.readData
import com.saikou.teraplay.utils.saveData
import com.saikou.teraplay.utils.snackString
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import kotlin.math.min

class PlayerActivity : AppCompatActivity(), Player.Listener {
    private var notchHeight: Int = 1
    private var epChanging = false

    private val model: PlayerViewModel by viewModel()
    private var quality: String = "Auto"
    private var episodeLength: Float = 0f
    private var playbackPosition: Long = 0
    private lateinit var binding: ActivityPlayerBinding
    private var mBackstackLost = true
    private lateinit var exoTopControllers: LinearLayout
    private lateinit var exoMiddleControllers: LinearLayout
    private lateinit var exoBottomControllers: LinearLayout
    private var isFullscreen: Int = 0
    private var orientationListener: OrientationEventListener? = null
    private var isPlayerPlaying = true

    private var isNormal = true
    private val resumeWindow = "resumeWindow"
    private val resumePosition = "resumePosition"
    private val playerFullscreen = "playerFullscreen"
    private val playerOnPlay = "playerOnPlay"

    // Top buttons
    private lateinit var loadingLayout: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var exoPlay: ImageView
    private lateinit var scaleBtn: ImageButton
    private lateinit var exoQuality: ImageButton
    private lateinit var exoRotate: ImageButton
    private lateinit var downloadBtn: ImageButton
    private lateinit var prevEpBtn: ImageButton
    private lateinit var nextEpBtn: ImageButton
    private lateinit var videoEpTextView: TextView
    private lateinit var exoPip: ImageButton
    private lateinit var exoSpeed: ImageButton
    private lateinit var exoProgress: ExtendedTimeBar
    private lateinit var exoLock: ImageButton
    private var isInit: Boolean = false
    private val mCookieManager = CookieManager()
    private lateinit var exoBrightness: Slider
    private lateinit var exoVolume: Slider
    private lateinit var exoBrightnessCont: View
    private lateinit var exoVolumeCont: View
    var rotation = 0
    private lateinit var videoName: TextView
    private lateinit var videoInfo: TextView
    private lateinit var serverInfo: TextView

    override fun onAttachedToWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = window.decorView.rootWindowInsets.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    notchHeight = min(
                        displayCutout.boundingRects[0].width(),
                        displayCutout.boundingRects[0].height()
                    )
                    checkNotch()
                }
            }
        }
        super.onAttachedToWindow()
    }

    private fun checkNotch() {
        if (notchHeight != 0) {
            val orientation = resources.configuration.orientation
            playerView.findViewById<View>(R.id.exo_controller_cont)
                .updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        marginStart = notchHeight
                        marginEnd = notchHeight
                        topMargin = 0
                    } else {
                        topMargin = notchHeight
                        marginStart = 0
                        marginEnd = 0
                    }
                }
            exoBrightnessCont.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd =
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) notchHeight else 0
            }
            exoVolumeCont.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart =
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) notchHeight else 0
            }
        }
    }

    @SuppressLint("DefaultLocale", "UnsafeOptInUsageError")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        ) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        //Initialize
        hideSystemBars()
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }



        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(mCookieManager)
        playerView = binding.exoPlayerView
        playerView.doubleTapOverlay = binding.doubleTapOverlay
        loadingLayout = binding.loadingLayout

        exoPip = playerView.findViewById(R.id.exo_pip)
        exoSpeed = playerView.findViewById(androidx.media3.ui.R.id.exo_playback_speed)
        prevEpBtn = playerView.findViewById(R.id.exo_prev_ep)
        exoRotate = playerView.findViewById(R.id.exo_rotate)
        nextEpBtn = playerView.findViewById(R.id.exo_next_ep)
        videoEpTextView = playerView.findViewById(R.id.exo_anime_title)
        downloadBtn = playerView.findViewById(R.id.exo_download)
        exoLock = playerView.findViewById(R.id.exo_lock)
        exoPlay = playerView.findViewById(androidx.media3.ui.R.id.exo_play)
        exoTopControllers = findViewById(R.id.exo_top_cont)
        exoBottomControllers = findViewById(R.id.exo_bottom_cont)
        exoBrightness = findViewById(R.id.exo_brightness)
        exoVolume = findViewById(R.id.exo_volume)
        exoBrightnessCont = findViewById(R.id.exo_brightness_cont)
        exoVolumeCont = findViewById(R.id.exo_volume_cont)
        exoProgress = findViewById(androidx.media3.ui.R.id.exo_progress)
        exoQuality = findViewById(R.id.exo_quality)
        videoName = playerView.findViewById(R.id.exo_video_name)
        videoInfo = playerView.findViewById(R.id.exo_video_info)
        serverInfo = playerView.findViewById(R.id.exo_server_info)

        playerView.keepScreenOn = true
        playerView.player = model.player
        playerView.subtitleView?.visibility = View.VISIBLE
        playerView.findViewById<ExtendedTimeBar>(androidx.media3.ui.R.id.exo_progress)
            .setKeyTimeIncrement(10000)
        prepareButtons()
        videoEpTextView.text = animePlayingDetails!!.fileName
//        model.downloadLink.observe(this) { link ->
//            downloadBtn.show()
//            downloadBtn.setOnClickListener {
//                download(
//                    this,
//                    movieInfo!!,
//                    link,
//                    epListByName.get(currentEpIndex).first
//                )
//            }
//        }

        playbackPosition = readData("${animePlayingDetails?.fileName}_${currentEpIndex}", this) ?: 0

        println("Position$playbackPosition")
        model.keepScreenOn.observe(this) { keepScreenOn ->
            playerView.keepScreenOn = keepScreenOn
        }

        model.isError.observe(this) { isError ->
            if (isError) {
                finishAndRemoveTask()
            }
        }

//        model.isLoading.observe(this) { isLoading ->
//            loadingLayout.isVisible = isLoading
//            playerView.isVisible = !isLoading
//
//        }

        if (!isInit) {
            model.setAnimeLink(
                animePlayingDetails.directLink
            )
            model.player.trackSelectionParameters =
                DefaultTrackSelector.ParametersBuilder(this)
                    .apply {
                        setMinVideoBitrate(4000)
                        setMaxVideoBitrate(1000000)
                        setForceLowestBitrate(true)
                        setForceHighestSupportedBitrate(true)
                    }
                    .build()
            videoName.text = "Auto"
            videoInfo.text = "Bitrate Auto"
            serverInfo.text = "Auto"
            playbackPosition =
                readData("${animePlayingDetails?.fileName}_${currentEpIndex}", this) ?: 0
            prevEpBtn.setImageViewEnabled(PlayerActivity.currentEpIndex.toInt() >= 2)
            nextEpBtn.setImageViewEnabled(currentEpIndex.toInt() + 1 != epCount.toInt())
        }
        isInit = true

        if (animePlayingDetails == null) {
            videoName.visibility = View.GONE
            videoInfo.visibility = View.GONE
            serverInfo.visibility = View.GONE
        } else {
            videoName.isSelected = true
//            videoName.text = animePlayingDetails.urlobj.get(currentEpIndex).hdtv
//            videoInfo.text = animePlayingDetails.urlobj.get(currentEpIndex).hdtv
//            serverInfo.text = animePlayingDetails.urlobj.get(currentEpIndex).typeName
        }


        model.showSubsBtn.observe(this) {
            if (!it) {
                if (playbackPosition != 0L) {
                    val time = String.format(
                        "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(playbackPosition),
                        TimeUnit.MILLISECONDS.toMinutes(playbackPosition) - TimeUnit.HOURS.toMinutes(
                            TimeUnit.MILLISECONDS.toHours(
                                playbackPosition
                            )
                        ),
                        TimeUnit.MILLISECONDS.toSeconds(playbackPosition) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(
                                playbackPosition
                            )
                        )
                    )
                    AlertDialog.Builder(this, R.style.DialogTheme)
                        .setTitle("Continue from ${time}?").apply {
                            setCancelable(false)
                            setPositiveButton("Yes") { d, _ ->
                                buildExoplayer()
                                d.dismiss()
                            }
                            setNegativeButton("No") { d, _ ->
                                playbackPosition = 0L
                                buildExoplayer()
                                d.dismiss()
                            }
                        }.show()
                } else {

                }
            }
        }

    }


    private fun buildExoplayer() {
        model.player.playWhenReady = true
        model.player
            .apply {
                seekTo(playbackPosition)
                play()
            }
    }


    @SuppressLint("StringFormatInvalid")
    private fun changeVideoSpeed(byInt: Float) {
        model.player.playbackParameters = PlaybackParameters(byInt)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        if (isInit) {
            outState.putInt(resumeWindow, model.player.currentMediaItemIndex)
            outState.putLong(resumePosition, model.player.currentPosition)
        }
        outState.putInt(playerFullscreen, isFullscreen)
        outState.putBoolean(playerOnPlay, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    @SuppressLint("WrongConstant", "UnsafeOptInUsageError")
    private fun prepareButtons() {
        // For Screen Rotation
        var flag = true
        exoRotate.setOnClickListener {
            if (flag) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                flag = false
            } else {
                this.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                flag = true

            }
        }




        exoQuality.setOnClickListener {
//            initPopupQuality(
//                animePlayingDetails = animePlayingDetails,
//                currentEpIndex, player
//            ).show()
        }

        playerView.setLongPressListenerEvent {
            val currentSpeed = model.player.playbackParameters.speed
            if (currentSpeed == 1f && model.player.playWhenReady && isNormal) {
                val params = PlaybackParameters(2f)
                model.player.setPlaybackParameters(params)
                snackString("Speed 2x", this@PlayerActivity)
            }
        }




        playerView.setActionUpListener {
            val currentSpeed = model.player.playbackParameters.speed
            if (currentSpeed == 2f && model.player.playWhenReady && isNormal) {
                val params = PlaybackParameters(1f)
                model.player.setPlaybackParameters(params)
                snackString("Speed 1x", this@PlayerActivity)
            }
        }

        var locked = false
        val container = playerView.findViewById<View>(R.id.exo_controller_cont)
        val screen = playerView.findViewById<View>(R.id.exo_black_screen)
        val lockButton = playerView.findViewById<ImageButton>(R.id.exo_unlock)
        val timeline =
            playerView.findViewById<ExtendedTimeBar>(androidx.media3.ui.R.id.exo_progress)
        playerView.findViewById<ImageButton>(R.id.exo_lock).setOnClickListener {
            locked = true
            screen.visibility = View.GONE
            container.visibility = View.GONE
            lockButton.visibility = View.VISIBLE
            timeline.setForceDisabled(true)
        }
        lockButton.setOnClickListener {
            locked = false
            screen.visibility = View.VISIBLE
            container.visibility = View.VISIBLE
            it.visibility = View.GONE
            timeline.setForceDisabled(false)
        }

        scaleBtn = playerView.findViewById(R.id.exo_screen)
        prevEpBtn = playerView.findViewById(R.id.exo_prev_ep)
        nextEpBtn = playerView.findViewById(R.id.exo_next_ep)


//        model.player.playbackParameters =


        exoSpeed.setOnClickListener {
            val builder =
                AlertDialog.Builder(this, R.style.DialogTheme)
            builder.setTitle("Speed")


            val speed = arrayOf("0.25", "0.5", "Normal", "1.5", "2")
            val adapter = CustomAdapter(
                this,
                speed
            )
            builder.setAdapter(adapter) { dad, which ->
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
                hideSystemBars()

                when (which) {
                    0 -> {
                        isNormal = false
                        adapter.setSelected(0)
                        changeVideoSpeed(0.25f)
                    }

                    1 -> {
                        isNormal = false
                        adapter.setSelected(1)
                        changeVideoSpeed(0.5f)
                    }

                    2 -> {
                        isNormal = true

                        adapter.setSelected(2)
                        changeVideoSpeed(1f)
                    }

                    3 -> {
                        isNormal = false
                        adapter.setSelected(3)
                        changeVideoSpeed(1.5f)
                    }

                    else -> {
                        isNormal = false
                        adapter.setSelected(4)
                        changeVideoSpeed(2f)

                    }
                }
            }
            hideSystemBars()

            val dialog = builder.create()
            dialog.show()
        }




        exoPip.setOnClickListener {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), packageName
                ) == AppOpsManager.MODE_ALLOWED
            } else false
            // API >= 26 check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (status) {
                    this.enterPictureInPictureMode(
                        PictureInPictureParams.Builder().build()
                    )
                    playerView.useController = false
                    pipStatus = false
                } else {
                    val intent = Intent(
                        "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            } else {
                Toast.makeText(
                    this,
                    "Feature not supported on this device",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        when (isFullscreen) {
            0 -> {
                if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                } else {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                }
            }

            1 -> {
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
        }




        scaleBtn.setOnClickListener {
            if (isFullscreen < 1) isFullscreen += 1 else isFullscreen = 0
            when (isFullscreen) {
                0 -> {
                    if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                    } else {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                }

                1 -> {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL

                }
            }

            Snackbar.make(
                binding.exoPlayerView, (
                        when (isFullscreen) {
                            0 -> "Original"
                            1 -> "Stretch"
                            else -> "Original"
                        }
                        ), 1000
            ).show()
        }


        exoPlay.setOnClickListener {
            if (isInit) {
                (exoPlay.drawable as Animatable?)?.start()
                if (model.player.isPlaying) {
                    Glide.with(this).load(R.drawable.anim_pause_to_play).into(exoPlay)

                    pauseVideo()
                } else {
                    Glide.with(this).load(R.drawable.anim_play_to_pause).into(exoPlay)
                    playVideo()
                }

            }
        }

// Back Button
        playerView.findViewById<ImageButton>(R.id.exo_back).apply {
            setOnClickListener {
                model.player.release()
                finish()
            }
        }


    }

//
//    private fun initPopupQuality(
//        animePlayingDetails: VodMovieResponse,
//        currentEpIndex: Int,
//        exoPlayer: ExoPlayer
//    ): Dialog {
//        val dialog = Dialog(this)
//        val view = LayoutInflater.from(this).inflate(R.layout.dialog_quality_selection, null)
//        dialog.setContentView(view)
//        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewQualities)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        val qualityList = mutableListOf(QualityItem("Auto", ""))
//        qualityList.addAll(animePlayingDetails.urlobj.map { QualityItem(it.hdtv, it.playUrl) })
//
//
//        val wasPlaying = player.isPlaying
//        player.pause()
//
//        dialog.setOnDismissListener {
//            if (wasPlaying) {
//                player.play()
//            }
//        }
//
//        dialog.show()
//        return dialog
//    }


    private fun changePlayerSource(playUrl: String, exoPlayer: ExoPlayer, epind: Int) {
        currentEpIndex = epind
        videoName.isSelected = true
        videoName.text = animePlayingDetails.fileName
        videoInfo.text = "TeraBox"
        serverInfo.text = animePlayingDetails.directLink.substringBefore("https://", ".com")

    }

    override fun onStop() {
        model.player.pause()
        saveData(
            "${animePlayingDetails?.fileName}_${currentEpIndex}",
            model.player.currentPosition,
            this
        )
        super.onStop()

    }


    companion object {
        var sourceType = ""
        var pipStatus: Boolean = false
        var epCount: Int = 0
        var currentEpIndex = 0
        private var isLocked: Boolean = false
        private lateinit var animePlayingDetails: DownloadResponse
        fun newIntent(
            context: Context,
            episodeData: DownloadResponse,
        ): Intent {
            val intent = Intent(context, PlayerActivity::class.java)
            animePlayingDetails = episodeData
            return intent
        }
    }

    private fun playVideo() {
        model.player.play()
    }

    private fun pauseVideo() {
        model.player.pause()
    }


    public override fun onResume() {
        super.onResume()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        hideSystemBars()

        playerView.useController = true
        model.player.prepare()
    }

    public override fun onPause() {
        super.onPause()
        if (pipStatus) pauseVideo()
        if (isInit) {
            playerView.player?.pause()
            saveData(
                "${animePlayingDetails.fileName}_${currentEpIndex}",
                model.player.currentPosition,
                this
            )
        }

    }

    override fun finish() {
        if (mBackstackLost) {
            finishAndRemoveTask()
        } else {
            super.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isInit = false
        pipStatus = false
        model.player.stop()
        model.player.release()
        finishAndRemoveTask()
        saveData(
            "${animePlayingDetails.fileName}_${currentEpIndex}",
            model.player.currentPosition,
            this
        )
    }


    private val keyMap: MutableMap<Int, (() -> Unit)?> = mutableMapOf(
        KeyEvent.KEYCODE_DPAD_RIGHT to null,
        KeyEvent.KEYCODE_DPAD_LEFT to null,
        KeyEvent.KEYCODE_SPACE to { exoPlay.performClick() },
        KeyEvent.KEYCODE_N to { nextEpBtn.performClick() },
        KeyEvent.KEYCODE_B to { prevEpBtn.performClick() }
    )
    private var wasPlaying = false
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!epChanging) {
            if (isInit && !hasFocus) wasPlaying = model.player.isPlaying
            if (hasFocus) {
                if (isInit && wasPlaying) model.player.play()
            } else {
                if (isInit) model.player.pause()
            }
        }
        super.onWindowFocusChanged(hasFocus)
    }

    private var isBuffering = true
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_READY) {
            model.player.play()
            if (episodeLength == 0f) {
                episodeLength = model.player.duration.toFloat()
            }
        }
        isBuffering = playbackState == Player.STATE_BUFFERING

        super.onPlaybackStateChanged(playbackState)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isBuffering) {
            isPlayerPlaying = isPlaying
            playerView.keepScreenOn = isPlaying
            (exoPlay.drawable as Animatable?)?.start()
            if (!this.isDestroyed)
                exoPlay.setImageViewEnabled(!isPlaying)
        }
    }


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (keyMap.containsKey(event.keyCode)) {
            (event.action == KeyEvent.ACTION_UP).also {
                if (isInit && it) keyMap[event.keyCode]?.invoke()
            }
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    private fun ImageView.setImageViewEnabled(enabled: Boolean) = if (enabled) {
        drawable.clearColorFilter()
        isEnabled = true
        isFocusable = true
    } else {
        drawable.colorFilter = PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
        isEnabled = false
        isFocusable = false
    }


    @SuppressLint("ViewConstructor", "UnsafeOptInUsageError")

    class ExtendedTimeBar(
        context: Context,
        attrs: AttributeSet?
    ) : DefaultTimeBar(context, attrs) {

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
}