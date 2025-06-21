package com.saikou.teraplay.presentation.dialog

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.saikou.teraplay.R
import com.saikou.teraplay.data.models.DownloadResponse
import com.saikou.teraplay.databinding.PlayerBottomSheetBinding
import com.saikou.teraplay.utils.gone
import com.saikou.teraplay.utils.visible

class PlayerBottomSheetDialog(private var media: DownloadResponse) : BottomSheetDialogFragment() {
    private var _binding: PlayerBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var isSelected = false

    private lateinit var btnClickListener: (DownloadResponse) -> Unit

    fun setOnBtnClickListener(listener: (DownloadResponse) -> Unit) {
        btnClickListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.player1Bg.setOnClickListener {
            binding.player1Radio.isChecked = true
            isSelected = true
            binding.player1Bg.setBackgroundResource(R.drawable.selected_player_bg)
            binding.errorTxt.gone()
        }
        binding.player1Radio.setOnClickListener {
            isSelected = true
            binding.errorTxt.gone()
            binding.player1Bg.setBackgroundResource(R.drawable.selected_player_bg)
        }
        binding.okBtn.setOnClickListener {
            if (isSelected) {
                dismiss()
                btnClickListener(media)
            } else {
                binding.errorTxt.visible()
                binding.errorTxt.vibrate(100)
                binding.errorTxt.vibrateAnimation()
            }
        }
    }

    companion object {
        fun newInstance(media: DownloadResponse): PlayerBottomSheetDialog {
            return PlayerBottomSheetDialog(media)
        }
    }
}

private fun View.vibrateAnimation() {
    val animator = ObjectAnimator.ofFloat(this, "translationX", 0f, 2f, -2f, 0f)
    animator.duration = 100
    animator.repeatCount = 1
    animator.start()
}

@SuppressLint("NewApi")
private fun View.vibrate(i: Int) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (vibrator.hasVibrator()) {
        val vibrationEffect =
            VibrationEffect.createOneShot(i.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    }
}
