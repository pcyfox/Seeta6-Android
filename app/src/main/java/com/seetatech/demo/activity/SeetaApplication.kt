package com.seetatech.demo.activity

import android.app.Application

class SeetaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        var instance: SeetaApplication? = null
            private set
    }
}