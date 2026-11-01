package com.essential.services

import android.app.Application
import android.content.Context

class EssentialServices : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}
