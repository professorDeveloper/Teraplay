package com.saikou.teraplay.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikou.teraplay.data.models.DownloadResponse
import com.saikou.teraplay.data.repository.DownloadRepositoryImpl
import com.saikou.teraplay.domain.repository.DownloadRepository
import com.saikou.teraplay.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: DownloadRepository) : ViewModel() {
    private val _searchResponse = MutableStateFlow<UiState<DownloadResponse>>(UiState.Idle)
    val searchResponse: StateFlow<UiState<DownloadResponse>> get() = _searchResponse

    fun trySearch(query: String) {
        viewModelScope.launch {
            _searchResponse.value = UiState.Loading
            val result = repo.fetchDownloadInfo(query)
            _searchResponse.value = when {
                result.isSuccess -> UiState.Success(result.getOrNull()!!)
                result.isFailure -> UiState.Error(result.exceptionOrNull()!!.message!!)
                else -> UiState.Idle
            }
        }
    }

}