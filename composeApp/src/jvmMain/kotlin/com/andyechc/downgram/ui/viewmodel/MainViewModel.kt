package com.andyechc.downgram.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andyechc.downgram.data.model.*
import com.andyechc.downgram.data.repository.TelegramRepository
import com.andyechc.downgram.ui.utils.formatFileSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private fun log(msg: String) {
    println("[MainViewModel] $msg")
}

sealed class MainUiState {
    object Idle : MainUiState()
    object Loading : MainUiState()
    data class Success(val dialogs: List<TelegramDialog>) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

class MainViewModel(private val repository: TelegramRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadStatus>>(emptyList())
    val downloads: StateFlow<List<DownloadStatus>> = _downloads.asStateFlow()

    private val _downloadGroups = MutableStateFlow<List<ActiveDownloadGroup>>(emptyList())
    val downloadGroups: StateFlow<List<ActiveDownloadGroup>> = _downloadGroups.asStateFlow()

    private val _downloadHistory = MutableStateFlow<List<DownloadHistoryItem>>(emptyList())
    val downloadHistory: StateFlow<List<DownloadHistoryItem>> = _downloadHistory.asStateFlow()

    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    init {
        startObservingProgress()
    }

    private fun startObservingProgress() {
        viewModelScope.launch {
            // Keep observing indefinitely with restart on failure
            while (isActive) {
                try {
                    // Fetch initial state immediately via REST
                    repository.getDownloads().onSuccess { statuses ->
                        _downloads.value = statuses
                        updateDownloadGroups(statuses)
                    }
                    
                    // Then listen for real-time updates
                    repository.observeDownloadProgress()
                        .catch { e ->
                            log("Download progress flow error: ${e.message}")
                        }
                        .collect { statuses ->
                            if (statuses.isNotEmpty()) {
                                log("WebSocket: ${statuses.size} downloads updated")
                            }
                            _downloads.value = statuses
                            updateDownloadGroups(statuses)
                        }
                } catch (e: Exception) {
                    log("WebSocket observer crashed: ${e.message}")
                    delay(3000) // Wait before restarting
                }
            }
        }
    }

    fun showNotification(message: String) {
        _notificationMessage.value = message
    }

    fun clearNotification() {
        _notificationMessage.value = null
    }

    fun loadDialogs() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            log("loadDialogs started")
            fetchDialogs()
        }
    }

    fun loadDialogsNonBlocking() {
        viewModelScope.launch {
            fetchDialogs()
        }
    }

    private suspend fun fetchDialogs() {
        log("Fetching dialogs from repository...")
        repository.getDialogs()
            .onSuccess { 
                log("Dialogs fetched successfully: ${it.size} dialogs")
                _uiState.value = MainUiState.Success(it) 
            }
            .onFailure { 
                log("Failed to fetch dialogs: ${it.message}")
                _uiState.value = MainUiState.Error(it.message ?: "Error") 
            }
    }

    private fun startDownloadPolling() {
        viewModelScope.launch {
            while (true) {
                repository.getDownloads()
                    .onSuccess { statuses ->
                        if (statuses.isNotEmpty()) {
                            log("Polling: ${statuses.size} downloads active")
                        }
                        _downloads.value = statuses
                        updateDownloadGroups(statuses)
                    }
                    .onFailure { error ->
                        log("Error polling downloads: ${error.message}")
                    }
                delay(1000)
            }
        }
    }

    private fun updateDownloadGroups(statuses: List<DownloadStatus>) {
        if (statuses.isEmpty()) {
            _downloadGroups.value = emptyList()
            return
        }

        val currentGroups = _downloadGroups.value
        val isExpanded = if (currentGroups.isNotEmpty()) currentGroups[0].isExpanded else true

        val activeFiles = statuses.map { ds ->
            ActiveDownloadFile(
                id = ds.id,
                filename = ds.filename,
                description = "Tamaño: ${formatFileSize(ds.size)}",
                progress = ds.progress,
                fileSize = ds.size,
                downloadSpeed = ds.download_speed ?: 0,
                remainingTime = ds.eta ?: 0,
                status = when (ds.status.lowercase()) {
                    "downloading" -> FileDownloadStatus.DOWNLOADING
                    "completed" -> FileDownloadStatus.COMPLETED
                    "paused" -> FileDownloadStatus.PAUSED
                    "failed" -> FileDownloadStatus.FAILED
                    "queued", "pending" -> FileDownloadStatus.PENDING
                    else -> FileDownloadStatus.PENDING
                }
            )
        }

        val overallProgress = if (activeFiles.isNotEmpty()) {
            activeFiles.sumOf { it.progress } / activeFiles.size
        } else 0

        val totalSize = activeFiles.sumOf { it.fileSize }
        val downloadedSize = activeFiles.sumOf { it.fileSize * it.progress / 100 }
        val totalSpeed = activeFiles.sumOf { it.downloadSpeed }
        val maxEta = activeFiles.filter { it.status == FileDownloadStatus.DOWNLOADING }.maxOfOrNull { it.remainingTime } ?: 0

        _downloadGroups.value = listOf(
            ActiveDownloadGroup(
                id = "current",
                totalFiles = activeFiles.size,
                completedFiles = activeFiles.count { it.status == FileDownloadStatus.COMPLETED },
                totalSize = totalSize,
                downloadedSize = downloadedSize,
                downloadSpeed = totalSpeed,
                remainingTime = maxEta,
                overallProgress = overallProgress,
                files = activeFiles,
                isExpanded = isExpanded
            )
        )
    }

    fun toggleDownloadGroupExpanded(groupId: String) {
        _downloadGroups.value = _downloadGroups.value.map {
            if (it.id == groupId) it.copy(isExpanded = !it.isExpanded) else it
        }
    }

    fun loadDownloadHistory() {
        viewModelScope.launch {
            repository.getDownloadHistory()
                .onSuccess { history ->
                    _downloadHistory.value = history
                    log("Download history loaded: ${history.size} items")
                }
                .onFailure { error ->
                    log("Failed to load download history: ${error.message}")
                    _downloadHistory.value = emptyList()
                }
        }
    }

    fun logout() {
        _uiState.value = MainUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
