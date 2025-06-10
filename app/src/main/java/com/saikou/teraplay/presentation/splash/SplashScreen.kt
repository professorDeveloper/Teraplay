package com.saikou.teraplay.presentation.splash

import android.graphics.Color
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.transition.MaterialContainerTransform
import com.saikou.teraplay.R
import com.saikou.teraplay.databinding.SplashScreenBinding
import com.saikou.teraplay.utils.animationTransactionClearStack

class SplashScreen : Fragment() {

    private var _binding: SplashScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navHost            // your NavHost ID
            duration = 1000L
            scrimColor = Color.TRANSPARENT                    // no fade-over
            addTarget(R.id.appLogo)                           // your logo view
            setAllContainerColors(requireContext().getColor(R.color.background))
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SplashScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val drawable = binding.appLogo.drawable

        val extras = FragmentNavigatorExtras(
            binding.appLogo to "logoTransition",
        )

        fun goNext() {
            findNavController().navigate(
                R.id.action_splashScreen_to_introScreen,
                null,
                animationTransactionClearStack(R.id.splashScreen).build(),
                extras
            )
        }

        when (drawable) {
            is AnimatedVectorDrawableCompat -> {
                drawable.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        goNext()
                    }
                })
                drawable.start()
            }
            is AnimatedVectorDrawable -> {
                drawable.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        goNext()
                    }
                })
                drawable.start()
            }
            else -> {
                binding.appLogo.postDelayed({ goNext() }, 2000L)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
