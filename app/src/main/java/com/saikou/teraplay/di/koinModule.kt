// AppModule.kt (or KoinModule.kt)
package com.saikou.teraplay.di

import com.saikou.teraplay.data.repository.DownloadRepositoryImpl
import com.saikou.teraplay.domain.repository.DownloadRepository
import com.saikou.teraplay.presentation.home.HomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // repository interface → implementation
    single<DownloadRepository> {
        DownloadRepositoryImpl(apiService = get())
    }

    // ViewModel depends only on the interface
    viewModel {
        HomeViewModel(repo = get())
    }
}
