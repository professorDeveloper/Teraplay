package com.saikou.teraplay.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class MyApp : Application() {
    @SuppressLint("StaticFieldLeak")
    companion object {
        lateinit var context: Context

    }

    override fun onCreate() {
        super.onCreate()
        context = this@MyApp
//        Bugsnag.start(this)
//        AndroidThreeTen.init(this) // Initialize ThreeTenABP
//        BugsnagPerformance.start(this)
        startKoin {
            androidContext(this@MyApp)
            androidLogger()
//            modules(NetworkModule, koinModule, firebaseModule)

        }

    }
}