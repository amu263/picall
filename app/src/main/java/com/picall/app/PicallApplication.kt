package com.picall.app

import android.app.Application

class PicallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PicallApplication
            private set
    }
}
