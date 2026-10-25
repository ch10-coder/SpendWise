package com.spendwise.app

import android.app.Application
import com.spendwise.app.di.AppContainer
import com.spendwise.app.di.DefaultAppContainer
import com.spendwise.app.server.SpendWiseApiServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SpendWiseApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var apiServer: SpendWiseApiServer? = null

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Start embedded HTTP API server on 127.0.0.1:8080 for on-device AI/automation
        apiServer = SpendWiseApiServer(
            repository = container.repository,
            scope = applicationScope,
            port = 8080,
            appVersion = appVersionName()
        ).also {
            it.start()
        }
    }

    @Suppress("DEPRECATION")
    private fun appVersionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: ""

    override fun onTerminate() {
        super.onTerminate()
        apiServer?.stop()
    }
}
