package com.andyechc.downgram.data.repository

import com.andyechc.downgram.ui.theme.ThemeMode
import java.util.prefs.Preferences

object SettingsRepository {
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_MAX_CONCURRENT = "max_concurrent_downloads"
    private const val KEY_USE_DEFAULT_PATH = "use_default_download_path"
    private const val KEY_DEFAULT_PATH = "default_download_path"
    private val prefs = Preferences.userNodeForPackage(SettingsRepository::class.java)

    fun getThemeMode(): ThemeMode {
        return try {
            val ordinal = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
            ThemeMode.entries.firstOrNull { it.ordinal == ordinal } ?: ThemeMode.SYSTEM
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.putInt(KEY_THEME_MODE, mode.ordinal)
    }

    fun getMaxConcurrentDownloads(): Int {
        return prefs.getInt(KEY_MAX_CONCURRENT, 4)
    }

    fun setMaxConcurrentDownloads(value: Int) {
        prefs.putInt(KEY_MAX_CONCURRENT, value.coerceIn(1, 16))
    }

    fun getUseDefaultDownloadPath(): Boolean {
        return prefs.getBoolean(KEY_USE_DEFAULT_PATH, false)
    }

    fun setUseDefaultDownloadPath(value: Boolean) {
        prefs.putBoolean(KEY_USE_DEFAULT_PATH, value)
    }

    fun getDefaultDownloadPath(): String {
        return prefs.get(KEY_DEFAULT_PATH, "")
    }

    fun setDefaultDownloadPath(value: String) {
        prefs.put(KEY_DEFAULT_PATH, value)
    }
}
