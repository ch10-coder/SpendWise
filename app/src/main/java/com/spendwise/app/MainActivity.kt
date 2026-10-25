package com.spendwise.app

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.spendwise.app.ui.lock.AppLock
import com.spendwise.app.ui.lock.AppLockSession
import com.spendwise.app.ui.lock.LockScreen
import com.spendwise.app.ui.screens.main.MainScreen
import com.spendwise.app.ui.theme.SpendWiseTheme
import kotlinx.coroutines.launch

// FragmentActivity (a ComponentActivity) is required by androidx BiometricPrompt
class MainActivity : FragmentActivity() {

    private lateinit var biometricPrompt: BiometricPrompt

    // Android 8.x: confirm the PIN / pattern / password directly
    private val confirmDeviceCredential =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) AppLockSession.isUnlocked = true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SpendWiseApplication
        val repository = app.container.repository

        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLockSession.isUnlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Nothing to verify against on this phone: never lock the user out of their own data
                    if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS
                    ) {
                        AppLockSession.isUnlocked = true
                    }
                }
            }
        )

        // Generate due recurring transactions every time the app comes to the foreground,
        // not just on a cold start
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.processRecurringTransactions()
            }
        }

        setContent {
            val themeMode by repository.themeMode.collectAsStateWithLifecycle(initialValue = repository.initialThemeMode)
            val isAppLockEnabled by repository.isAppLockEnabled.collectAsStateWithLifecycle(initialValue = repository.initialIsAppLockEnabled)

            SpendWiseTheme(
                themeMode = themeMode
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainScreen(repository = repository)
                    }
                    if (isAppLockEnabled && !AppLockSession.isUnlocked) {
                        LockScreen(
                            onUnlockRequest = ::requestUnlock,
                            onExit = ::finish
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val backgroundedAt = AppLockSession.backgroundedAtElapsed
        if (backgroundedAt != 0L && SystemClock.elapsedRealtime() - backgroundedAt > AppLockSession.RELOCK_AFTER_MS) {
            AppLockSession.isUnlocked = false
        }
        AppLockSession.backgroundedAtElapsed = 0L
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            AppLockSession.backgroundedAtElapsed = SystemClock.elapsedRealtime()
        }
    }

    private fun requestUnlock() {
        if (!AppLock.isAvailable(this)) {
            // The screen lock was removed after App Lock was turned on; there is nothing to check against
            AppLockSession.isUnlocked = true
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val keyguard = getSystemService(KeyguardManager::class.java)
            @Suppress("DEPRECATION")
            val intent = keyguard?.createConfirmDeviceCredentialIntent("Unlock SpendWise", "Confirm it's you to see your finances")
            if (intent == null) {
                AppLockSession.isUnlocked = true
            } else {
                confirmDeviceCredential.launch(intent)
            }
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock SpendWise")
            .setSubtitle("Confirm it's you to see your finances")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}
