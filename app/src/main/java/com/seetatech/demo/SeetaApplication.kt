package com.seetatech.demo

import android.app.Application
import com.seetatech.demo.SeetaApplication

class SeetaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    companion object {
        var instance: SeetaApplication? = null
            private set
    }
}