package com.saikou.teraplay.custom

import android.annotation.SuppressLint
import android.content.Context
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.core.view.GestureDetectorCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView

class PlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : PlayerView(context, attrs, defStyle) {

    lateinit var setActionUpListener: (() -> Unit)

    lateinit var onLongPressListener: (() -> Unit)


    fun setLongPressListenerEvent(itemListenerActionUp: () -> Unit) {
        onLongPressListener = itemListenerActionUp
    }

    fun setActionUpListener(itemListenerActionUp: (() -> Unit)) {
        setActionUpListener = itemListenerActionUp
    }


    companion object {
        const val SEEK_SECONDS = 5
        const val SEEK_MILLISECONDS = SEEK_SECONDS * 1000
    }

    lateinit var doubleTapOverlay: DoubleTapOverlay
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        private var isDoubleTapping = false
        private val handler = Handler(Looper.getMainLooper())
        private val stopDoubleTap = Runnable {
            isDoubleTapping = false
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (isDoubleTapping) {
                handleDoubleTap(e.x, e.y)
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isDoubleTapping)
                performClick()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isDoubleTapping)
                keepDoubleTapping()
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            if (e.actionMasked == MotionEvent.ACTION_UP && isDoubleTapping)
                handleDoubleTap(e.x, e.y)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            println("TUSHDIIIIIIIIIIIIIIIII {}$ % D")
            onLongPressListener.invoke()
        }

        override fun onDown(e: MotionEvent): Boolean {
            return super.onDown(e)
        }

        fun cancelDoubleTap() {
            handler.removeCallbacks(stopDoubleTap)
            isDoubleTapping = false
        }

        fun keepDoubleTapping() {
            handler.removeCallbacks(stopDoubleTap)
            isDoubleTapping = true
            handler.postDelayed(stopDoubleTap, 700)
        }
    }
    private val gestureDetector = GestureDetectorCompat(context, gestureListener)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                setActionUpListener.invoke()

            }
        }
        return true
    }

    fun handleDoubleTap(x: Float, y: Float) {
        player?.let { player ->
            if (player.playbackState == PlaybackState.STATE_ERROR ||
                player.playbackState == PlaybackState.STATE_NONE ||
                player.playbackState == PlaybackState.STATE_STOPPED
            )
                gestureListener.cancelDoubleTap()
            else if (player.currentPosition > 500 && x < doubleTapOverlay.width * 0.35)
                triggerSeek(false, x, y)
            else if (player.currentPosition < player.duration && x > doubleTapOverlay.width * 0.65)
                triggerSeek(true, x, y)
        }
    }

    private fun triggerSeek(forward: Boolean, x: Float, y: Float) {
        doubleTapOverlay.showAnimation(forward, x, y)
        player?.let { player ->
            seekTo(
                if (forward)
                    player.currentPosition + SEEK_MILLISECONDS
                else
                    player.currentPosition - SEEK_MILLISECONDS
            )
        }
    }

    private fun seekTo(position: Long) {
        player?.let { player ->
            when {
                position <= 0 -> player.seekTo(0)
                position >= player.duration -> player.seekTo(player.duration)
                else -> {
                    gestureListener.keepDoubleTapping()
                    player.seekTo(position)
                }
            }
        }
    }
    val controller: PlayerControlView
        @OptIn(UnstableApi::class)
        get() = PlayerView::class.java.getDeclaredField("controller").let {
            it.isAccessible = true
            it.get(this) as PlayerControlView
        }


    @OptIn(UnstableApi::class)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val player = player ?: return super.dispatchKeyEvent(event)

        if (player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM) && player.isPlayingAd) {
            return super.dispatchKeyEvent(event)
        }

        if (controller.isVisible) return super.dispatchKeyEvent(event)

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                player.seekTo(player.currentPosition - 10_000)
                true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                player.seekTo(player.currentPosition + 10_000)
                true
            }

            else -> super.dispatchKeyEvent(event)
        }
    }
}