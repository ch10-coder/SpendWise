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

        // Start embedded HTTP API server on port 8080 for local AI/automation
        apiServer = SpendWiseApiServer(
            repository = container.repository,
            scope = applicationScope,
            port = 8080
        ).also {
            it.start()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        apiServer?.stop()
    }
}
