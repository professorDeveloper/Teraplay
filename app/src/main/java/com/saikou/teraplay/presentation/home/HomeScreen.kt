package com.saikou.teraplay.presentation.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.saikou.teraplay.R
import com.saikou.teraplay.broadcast.DownloadReceiver
import com.saikou.teraplay.data.models.DownloadItem
import com.saikou.teraplay.data.models.DownloadResponse
import com.saikou.teraplay.data.models.DownloadStatus
import com.saikou.teraplay.databinding.DialogBackgroundBinding
import com.saikou.teraplay.databinding.HomeScreenBinding
import com.saikou.teraplay.presentation.play.PlayerActivity
import com.saikou.teraplay.utils.UiState
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class HomeScreen : Fragment(R.layout.home_screen) {
    private var _binding: HomeScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModel()
    private var dialogInstance: AlertDialog? = null
    private var currentDownloadItem: DownloadItem? = null
    private lateinit var cancelReceiver: BroadcastReceiver
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) {
                viewModel.loadDownloads(requireContext())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }
            } else {
                Snackbar.make(
                    binding.root,
                    "Some permissions denied. Notifications or downloads may not work.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.home_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = HomeScreenBinding.bind(view)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        setupUI()
        requestPermissions()
        cancelReceiver = DownloadReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().registerReceiver(
                cancelReceiver,
                IntentFilter("ACTION_CANCEL_DOWNLOAD"),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }

    @SuppressLint("NewApi")
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsLauncher.launch(permissions)
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = android.app.NotificationChannel(
            "download_channel",
            "Downloads",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(true)
            enableLights(true)
            enableVibration(true)
        }
        (requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun setupUI() {
        binding.copyButton.visibility = View.GONE
        binding.previewContainer.root.visibility = View.GONE
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.isVisible(WindowInsetsCompat.Type.ime())
            binding.copyButton.apply {
                visibility = if (ime) View.VISIBLE else View.GONE
                translationY =
                    if (ime) -insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.toFloat() else 0f
            }
            insets
        }
        binding.urlInput.doOnTextChanged { _, _, _, _ -> clearUrlError() }
        binding.urlInput.setOnEditorActionListener { _, id, _ ->
            if (id == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                attemptSearch(binding.urlInput.text.toString().trim())
                hideKeyboard()
                true
            } else false
        }
        binding.copyButton.setOnClickListener { pasteClipboard() }
        binding.trySearchButton.setOnClickListener {
            attemptSearch(binding.urlInput.text.toString().trim())
            hideKeyboard()
        }
        observeViewModel()
    }

    private fun clearUrlError() {
        binding.urlInputLayout.error = null
        binding.urlInput.setTextColor(ContextCompat.getColor(requireContext(), R.color.button_text))
    }

    private fun observeViewModel() {
        viewModel.searchResponse.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.dataProgress.visibility = View.VISIBLE
                    binding.previewContainer.root.visibility = View.GONE
                }

                is UiState.Success -> showPreview(state.data)
                is UiState.Error -> showError(state.message ?: "Unknown error")
                else -> {}
            }
        }

        viewModel.downloads.observe(viewLifecycleOwner) { list ->
            val item = list.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
            if (item != null && item != currentDownloadItem) {
                showProgressDialog(item)
                currentDownloadItem = item
            } else if (item == null) {
                dismissDialog()
            }
        }
    }

    private fun showPreview(data: DownloadResponse) {
        binding.dataProgress.visibility = View.GONE
        binding.previewContainer.root.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(300).start()
        }
        with(binding.previewContainer) {
            Glide.with(thumbnail).load(data.thumb).into(thumbnail)
            videoTitle.text = data.fileName
            sizeBadge.text = data.size
            playButton.setOnClickListener { openPlayer(data) }
            downloadButton.setOnClickListener {
                showStartDialog(data.fileName)
                viewModel.startDownload(requireContext(), data)
            }
        }
    }

    private fun showError(msg: String) {
        binding.dataProgress.visibility = View.GONE
        binding.previewContainer.root.visibility = View.GONE
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showStartDialog(name: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Download started")
            .setMessage("$name is downloading...")
            .setPositiveButton("OK", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showProgressDialog(item: DownloadItem) {
        val b = DialogBackgroundBinding.inflate(LayoutInflater.from(requireContext()))
        b.tvFileName.text = item.response.fileName
        b.tvStatus.text = item.status.name
        b.progressBar.progress = item.progress
        b.tvProgress.text = "${item.progress}%"
        b.btnCancel.setOnClickListener {
            lifecycleScope.launch {
                item.downloadId?.let { viewModel.cancelDownload(requireContext(), it) }
                dismissDialog()
            }
        }
        b.btnHide.setOnClickListener { dismissDialog() }
        dialogInstance = AlertDialog.Builder(requireContext())
            .setView(b.root)
            .create().apply { show() }
        viewModel.downloadProgress.observe(viewLifecycleOwner) { (id, prog, stat) ->
            if (id == item.downloadId) {
                b.progressBar.progress = prog
                b.tvProgress.text = "$prog%"
                b.tvStatus.text = stat.name
                if (stat != DownloadStatus.DOWNLOADING) dismissDialog()
            }
        }
    }

    private fun dismissDialog() {
        dialogInstance?.dismiss()
        dialogInstance = null
        currentDownloadItem = null
    }

    private fun openPlayer(data: DownloadResponse) {
        startActivity(PlayerActivity.newIntent(requireContext(), data))
    }

    private fun attemptSearch(url: String) {
        if (url.isNotBlank() && android.util.Patterns.WEB_URL.matcher(url).matches()) {
            viewModel.trySearch(url)
        } else {
            binding.urlInputLayout.error = "Invalid URL"
        }
    }

    private fun pasteClipboard() {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.primaryClip?.getItemAt(0)?.text?.toString()?.takeIf {
            android.util.Patterns.WEB_URL.matcher(it).matches()
        }?.let {
            binding.urlInput.setText(it)
            viewModel.trySearch(it)
        } ?: Snackbar.make(binding.root, "Invalid clipboard URL", Snackbar.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.urlInput.windowToken, 0)
    }

    override fun onDestroyView() {
        requireContext().unregisterReceiver(cancelReceiver)
        dismissDialog()
        super.onDestroyView()
        _binding = null
    }
}