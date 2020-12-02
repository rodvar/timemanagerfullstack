package com.rodvar.timemanager.base

import android.app.Application
import com.rodvar.timemanager.di.networkingModule
import com.rodvar.timemanager.di.repositoryModule
import com.rodvar.timemanager.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TimeManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // start Koin!
        startKoin {
            androidLogger()
            // declare used Android context
            androidContext(this@TimeManagerApplication)
            // declare modules
            modules(
                networkingModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}