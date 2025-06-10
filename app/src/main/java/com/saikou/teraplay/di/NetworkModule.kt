// NetworkModule.kt
package com.saikou.teraplay.di

import com.saikou.teraplay.data.remote.TeraBoxApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://teraboxdownloader.online/"

val networkModule = module {
    // 1) OkHttpClient
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // 2) Retrofit
    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 3) TeraBoxApi
    single<TeraBoxApi> {
        get<Retrofit>().create(TeraBoxApi::class.java)
    }
}
