package com.saikou.teraplay.presentation.splash

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.saikou.teraplay.R
import com.saikou.teraplay.databinding.SplashScreenBinding
import com.saikou.teraplay.utils.alphaAnim
import com.saikou.teraplay.utils.animationTransactionClearStack

class SplashScreen : Fragment() {
    private var _binding: SplashScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SplashScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenCreated {
            binding.appLogo.alphaAnim()
            binding.appLogo.postDelayed({
                findNavController().navigate(
                    R.id.action_splashScreen_to_homeScreen,
                    null,
                    animationTransactionClearStack(R.id.splashScreen).build()
                )
            }, 2000)
        }
    }
}