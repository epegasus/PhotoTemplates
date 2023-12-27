package dev.pegasus.phototemplates

import android.app.Application
import dev.pegasus.phototemplates.helpers.di.modules.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin()
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@MainApplication)
            modules(viewModelModule)
        }
    }
}