package com.andyechc.downgram.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.andyechc.downgram.data.repository.SettingsRepository
import com.andyechc.downgram.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    private val _themeMode = MutableStateFlow(SettingsRepository.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _maxConcurrentDownloads = MutableStateFlow(SettingsRepository.getMaxConcurrentDownloads())
    val maxConcurrentDownloads: StateFlow<Int> = _maxConcurrentDownloads.asStateFlow()

    private val _useDefaultPath = MutableStateFlow(SettingsRepository.getUseDefaultDownloadPath())
    val useDefaultPath: StateFlow<Boolean> = _useDefaultPath.asStateFlow()

    private val _defaultDownloadPath = MutableStateFlow(SettingsRepository.getDefaultDownloadPath())
    val defaultDownloadPath: StateFlow<String> = _defaultDownloadPath.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        SettingsRepository.setThemeMode(mode)
    }

    fun setMaxConcurrentDownloads(value: Int) {
        val clamped = value.coerceIn(1, 16)
        _maxConcurrentDownloads.value = clamped
        SettingsRepository.setMaxConcurrentDownloads(clamped)
    }

    fun setUseDefaultPath(value: Boolean) {
        _useDefaultPath.value = value
        SettingsRepository.setUseDefaultDownloadPath(value)
    }

    fun setDefaultDownloadPath(value: String) {
        _defaultDownloadPath.value = value
        SettingsRepository.setDefaultDownloadPath(value)
    }
}
