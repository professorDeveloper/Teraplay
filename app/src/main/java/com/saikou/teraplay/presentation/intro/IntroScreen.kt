package com.saikou.teraplay.presentation.intro

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.saikou.teraplay.R
import com.saikou.teraplay.databinding.IntroScreenBinding
import com.saikou.teraplay.utils.animationTransactionClearStack

class IntroScreen : Fragment() {

    private var _binding: IntroScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Shared‐element container transform for your logo+title:
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.navHost           // your NavHost container
            duration = 600L                                  // fast but snappy
            scrimColor = Color.TRANSPARENT                   // no grey overlay
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            addTarget(R.id.introBanner)                        // the ImageView in intro
            setAllContainerColors(requireContext().getColor(R.color.background_dashboard))
            setPathMotion(MaterialArcMotion())                 // gentle arc
        }

        // 2) Fade‐through for non‐shared content:
        enterTransition = MaterialFadeThrough().apply {
            duration = 1000L
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Disable default return/pop transitions
        returnTransition = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = IntroScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure the shared‐element only starts once the destination is laid out:
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }

        val stagger = listOf(
            binding.introBanner,
            binding.titleText,
            binding.descText,
            binding.buttonContainer,
            binding.getStartButton
        )

        stagger.forEachIndexed { idx, v ->
            v.alpha = 0f
            v.translationY = 30f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((idx * 100L) + 400L)
                .setDuration(400L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // 4) Button nav:
        binding.getStartButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_introScreen_to_homeScreen,
                null,
                animationTransactionClearStack(R.id.introScreen).build()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
