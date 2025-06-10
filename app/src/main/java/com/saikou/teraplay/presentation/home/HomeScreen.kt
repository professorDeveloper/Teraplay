package com.saikou.teraplay.presentation.home

import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.saikou.teraplay.R
import com.saikou.teraplay.databinding.HomeScreenBinding
import com.saikou.teraplay.utils.UiState
import com.saikou.teraplay.utils.snackString
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeScreen : Fragment() {
    private var _binding: HomeScreenBinding? = null
    private val binding get() = _binding!!

    private val model: HomeViewModel by viewModel()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = HomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("StringFormatInvalid")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.copyButton.setOnClickListener {
            pasteClipboardToInput()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            model.searchResponse.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.dataProgress.visibility = View.VISIBLE
                        binding.previewContainer.root.visibility = View.GONE
                    }

                    is UiState.Success -> {
                        binding.dataProgress.visibility = View.GONE
                        binding.previewContainer.root.visibility = View.VISIBLE

                        with(binding.previewContainer) {
                            Glide.with(thumbnail)
                                .load(state.data.thumb)
                                .into(thumbnail)

                            videoTitle.text = state.data.fileName
                            sizeBadge.text = state.data.size

                            playButton.setOnClickListener {
//                                openVideoPlayer(state.data.directLink)
                            }
                            downloadButton.setOnClickListener {
//                                downloadFile(state.data.directLink, state.data.fileName)
                            }
                        }
                    }

                    is UiState.Error -> {
                        binding.dataProgress.visibility = View.GONE
                        binding.previewContainer.root.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            state.message ?: "Unknown error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    UiState.Idle -> {
                    }
                }
            }
        }

        binding.urlInput.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO
            ) {
                val url = textView.text.toString().trim()
                if (Patterns.WEB_URL.matcher(url)
                        .matches() && URLUtil.isValidUrl(url) && url.isNotEmpty()
                ) {
                    model.trySearch(url)
                } else {
                    binding.urlInput.error =
                        requireActivity().getString(R.string.invalid_url_error, url)
                }
            }
            true
        }
        binding.trySearchButton.setOnClickListener {
            val url = binding.urlInput.text.toString().trim()
            if (Patterns.WEB_URL.matcher(url)
                    .matches() && URLUtil.isValidUrl(url) && url.isNotEmpty()
            ) {
                model.trySearch(url)
            } else {
                binding.urlInput.error =
                    requireActivity().getString(R.string.invalid_url_error, url)
            }
        }
    }

    private fun pasteClipboardToInput() {
        val clipboard = requireContext().getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip

        if (clip != null && clip.itemCount > 0 && clipboard.primaryClipDescription?.hasMimeType(
                ClipDescription.MIMETYPE_TEXT_PLAIN
            ) == true
        ) {
            val pastedText = clip.getItemAt(0).coerceToText(requireContext()).toString().trim()

            val isUrl =
                Patterns.WEB_URL.matcher(pastedText).matches() && URLUtil.isValidUrl(pastedText)

            if (isUrl) {
                binding.urlInput.setText(pastedText)
                model.trySearch(pastedText)
            } else {
                Toast.makeText(
                    requireContext(), "Clipboard does not contain a valid URL", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}