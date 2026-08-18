package com.spendwise.app

import android.app.Application
import com.spendwise.app.di.AppContainer
import com.spendwise.app.di.DefaultAppContainer

class SpendWiseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
