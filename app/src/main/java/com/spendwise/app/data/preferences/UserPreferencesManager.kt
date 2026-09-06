package com.spendwise.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spendwise.app.model.CurrencyOption
import com.spendwise.app.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("spendwise_fast_cache", Context.MODE_PRIVATE)

    private object PreferencesKeys {
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val IS_DYNAMIC_COLOR = booleanPreferencesKey("is_dynamic_color")
        val IS_BALANCE_HIDDEN = booleanPreferencesKey("is_balance_hidden")
        val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        val IS_AMOLED_BLACK = booleanPreferencesKey("is_amoled_black")
        val DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
    }

    private val dataStore = context.dataStore

    /**
     * Instantaneous, zero-delay initial reads from memory-mapped SharedPreferences.
     * Eliminates Main-Thread blocking (runBlocking) on app startup.
     */
    val initialThemeMode: ThemeMode
        get() {
            val raw = sharedPrefs.getString("cached_theme_mode", ThemeMode.SYSTEM.name)
            return try { ThemeMode.valueOf(raw!!) } catch (e: Exception) { ThemeMode.SYSTEM }
        }

    val initialThemePalette: com.spendwise.app.model.ThemePalette
        get() = com.spendwise.app.model.ThemePalette.EMERALD

    val initialIsDynamicColor: Boolean get() = false
    val initialIsAmoledBlack: Boolean get() = false

    val initialCurrencyCode: String
        get() = sharedPrefs.getString("cached_currency_code", CurrencyOption.DEFAULT.code) ?: CurrencyOption.DEFAULT.code

    val initialCurrencySymbol: String
        get() = sharedPrefs.getString("cached_currency_symbol", CurrencyOption.DEFAULT.symbol) ?: CurrencyOption.DEFAULT.symbol

    val currencyCodeFlow: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.CURRENCY_CODE] ?: CurrencyOption.DEFAULT.code
        }

    val currencySymbolFlow: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: CurrencyOption.DEFAULT.symbol
        }

    val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            val rawMode = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(rawMode)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }

    val themePaletteFlow: Flow<com.spendwise.app.model.ThemePalette> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { com.spendwise.app.model.ThemePalette.EMERALD }

    val isDynamicColorFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.IS_DYNAMIC_COLOR] ?: false
        }

    val isBalanceHiddenFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.IS_BALANCE_HIDDEN] ?: false
        }

    val isAppLockEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.IS_APP_LOCK_ENABLED] ?: false
        }

    val isAmoledBlackFlow: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.IS_AMOLED_BLACK] ?: false
        }

    val dailyReminderTimeFlow: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_TIME]
        }

    suspend fun setCurrency(code: String, symbol: String) {
        sharedPrefs.edit()
            .putString("cached_currency_code", code)
            .putString("cached_currency_symbol", symbol)
            .apply()

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_CODE] = code
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        sharedPrefs.edit()
            .putString("cached_theme_mode", mode.name)
            .apply()

        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setThemePalette(palette: com.spendwise.app.model.ThemePalette) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PALETTE] = palette.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setBalanceHidden(hidden: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BALANCE_HIDDEN] = hidden
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AMOLED_BLACK] = enabled
        }
    }

    suspend fun setDailyReminderTime(time: String?) {
        dataStore.edit { preferences ->
            if (time != null) {
                preferences[PreferencesKeys.DAILY_REMINDER_TIME] = time
            } else {
                preferences.remove(PreferencesKeys.DAILY_REMINDER_TIME)
            }
        }
    }
}
