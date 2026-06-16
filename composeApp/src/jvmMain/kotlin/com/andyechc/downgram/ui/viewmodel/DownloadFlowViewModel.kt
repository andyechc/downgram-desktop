package com.andyechc.downgram.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andyechc.downgram.data.model.TelegramDialog
import com.andyechc.downgram.data.model.MediaFile
import com.andyechc.downgram.data.repository.SettingsRepository
import com.andyechc.downgram.data.repository.TelegramRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private fun log(msg: String) {
    println("[DownloadFlowViewModel] $msg")
}

enum class DownloadFlowStep {
    CHANNEL_SELECTION,
    MEDIA_SEARCH,
    PATH_SELECTION,
    DOWNLOADING
}

sealed class DownloadFlowUiState {
    object Idle : DownloadFlowUiState()
    object Loading : DownloadFlowUiState()
    data class ChannelSelectionLoaded(val dialogs: List<TelegramDialog>) : DownloadFlowUiState()
    data class SearchResultsLoaded(val results: List<MediaFile>, val hasMore: Boolean) : DownloadFlowUiState()
    data class Error(val message: String) : DownloadFlowUiState()
}

class DownloadFlowViewModel(private val repository: TelegramRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<DownloadFlowUiState>(DownloadFlowUiState.Idle)
    val uiState: StateFlow<DownloadFlowUiState> = _uiState.asStateFlow()

    private val _currentStep = MutableStateFlow(DownloadFlowStep.CHANNEL_SELECTION)
    val currentStep: StateFlow<DownloadFlowStep> = _currentStep.asStateFlow()

    private val _selectedDialogs = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDialogs: StateFlow<Set<Long>> = _selectedDialogs.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFiles: StateFlow<Set<Long>> = _selectedFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPath = MutableStateFlow("")
    val selectedPath: StateFlow<String> = _selectedPath.asStateFlow()

    private val _selectedExtensions = MutableStateFlow<Set<String>>(emptySet())
    val selectedExtensions: StateFlow<Set<String>> = _selectedExtensions.asStateFlow()

    private val _downloadGroups = MutableStateFlow<List<com.andyechc.downgram.data.model.ActiveDownloadGroup>>(emptyList())
    val downloadGroups: StateFlow<List<com.andyechc.downgram.data.model.ActiveDownloadGroup>> = _downloadGroups.asStateFlow()

    private var searchJob: Job? = null
    private var isSearching = false

    // Derived state for available extensions from current search results
    val availableExtensions: StateFlow<Set<String>> = _uiState.map { state ->
        if (state is DownloadFlowUiState.SearchResultsLoaded) {
            state.results.map { it.media_type }.toSet()
        } else {
            emptySet()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Derived state for filtered results
    val filteredResults: StateFlow<List<MediaFile>> = combine(_uiState, _selectedExtensions) { state, selectedExts ->
        if (state is DownloadFlowUiState.SearchResultsLoaded) {
            if (selectedExts.isEmpty()) {
                state.results
            } else {
                state.results.filter { selectedExts.contains(it.media_type) }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleExtensionFilter(extension: String) {
        val current = _selectedExtensions.value
        _selectedExtensions.value = if (current.contains(extension)) {
            current - extension
        } else {
            current + extension
        }
    }

    fun startFlow() {
        viewModelScope.launch {
            _currentStep.value = DownloadFlowStep.CHANNEL_SELECTION
            loadChannels()
        }
    }

    fun resetFlow() {
        _currentStep.value = DownloadFlowStep.CHANNEL_SELECTION
        _uiState.value = DownloadFlowUiState.Idle
        _selectedDialogs.value = emptySet()
        _selectedFiles.value = emptySet()
        _searchQuery.value = ""
        _selectedPath.value = ""
        _selectedExtensions.value = emptySet()
        log("Flow reset")
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.value = DownloadFlowUiState.Loading
            log("Loading channels...")
            
            repository.getDialogs()
                .onSuccess { dialogs ->
                    log("Channels loaded: ${dialogs.size} dialogs")
                    _uiState.value = DownloadFlowUiState.ChannelSelectionLoaded(dialogs)
                }
                .onFailure { error ->
                    log("Failed to load channels: ${error.message}")
                    _uiState.value = DownloadFlowUiState.Error(error.message ?: "Error al cargar canales")
                }
        }
    }

    fun toggleDialogSelection(dialogId: Long) {
        val current = _selectedDialogs.value
        _selectedDialogs.value = if (current.contains(dialogId)) {
            current - dialogId
        } else {
            current + dialogId
        }
        log("Dialog selection toggled: $dialogId, selected: ${_selectedDialogs.value}")
    }

    fun proceedToSearch() {
        if (_selectedDialogs.value.isNotEmpty()) {
            _currentStep.value = DownloadFlowStep.MEDIA_SEARCH
            _searchQuery.value = ""
            _selectedFiles.value = emptySet()
            _selectedExtensions.value = emptySet()
            log("Proceeded to search step with ${_selectedDialogs.value.size} channels")
            searchMedia("")
        }
    }

    fun searchMedia(keyword: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is DownloadFlowUiState.SearchResultsLoaded) {
                _uiState.value = DownloadFlowUiState.Loading
            }
            isSearching = true
            
            repository.search(
                entitiesIds = _selectedDialogs.value.toList(),
                keyword = keyword,
                offset = 0
            )
                .onSuccess { response ->
                    _uiState.value = DownloadFlowUiState.SearchResultsLoaded(response.media, response.has_more)
                }
                .onFailure { error ->
                    if (currentState !is DownloadFlowUiState.SearchResultsLoaded) {
                        _uiState.value = DownloadFlowUiState.Error(error.message ?: "Error en búsqueda")
                    }
                }
                .also {
                    isSearching = false
                }
        }
    }

    fun loadMoreMedia() {
        val currentState = _uiState.value
        if (currentState is DownloadFlowUiState.SearchResultsLoaded && currentState.hasMore) {
            viewModelScope.launch {
                val currentResults = currentState.results
                repository.search(
                    entitiesIds = _selectedDialogs.value.toList(),
                    keyword = _searchQuery.value,
                    offset = currentResults.size
                )
                    .onSuccess { response ->
                        _uiState.value = DownloadFlowUiState.SearchResultsLoaded(
                            results = currentResults + response.media,
                            hasMore = response.has_more
                        )
                    }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400)
                searchMedia(query)
            }
        } else if (query.isEmpty()) {
            if (_uiState.value is DownloadFlowUiState.SearchResultsLoaded) {
                _uiState.value = DownloadFlowUiState.Idle
            }
        }
    }

    fun toggleFileSelection(fileId: Long) {
        val current = _selectedFiles.value
        _selectedFiles.value = if (current.contains(fileId)) {
            current - fileId
        } else {
            current + fileId
        }
    }

    fun proceedToPathSelection() {
        if (_selectedFiles.value.isNotEmpty()) {
            _currentStep.value = DownloadFlowStep.PATH_SELECTION
            val defaultPath = SettingsRepository.getDefaultDownloadPath()
            _selectedPath.value = if (SettingsRepository.getUseDefaultDownloadPath() && defaultPath.isNotBlank()) {
                defaultPath
            } else ""
        }
    }

    fun selectPath(path: String) {
        _selectedPath.value = path
    }

    fun startDownloads() {
        if (_selectedFiles.value.isNotEmpty() && _selectedPath.value.isNotEmpty()) {
            _currentStep.value = DownloadFlowStep.DOWNLOADING
            
            viewModelScope.launch {
                val maxConcurrent = SettingsRepository.getMaxConcurrentDownloads()
                repository.setMaxConcurrent(maxConcurrent)
                _selectedFiles.value.forEach { fileId ->
                    repository.download(fileId, _selectedPath.value)
                }
            }
        }
    }

    fun goBack() {
        when (_currentStep.value) {
            DownloadFlowStep.MEDIA_SEARCH -> {
                _currentStep.value = DownloadFlowStep.CHANNEL_SELECTION
                _selectedFiles.value = emptySet()
                _searchQuery.value = ""
            }
            DownloadFlowStep.PATH_SELECTION -> {
                _currentStep.value = DownloadFlowStep.MEDIA_SEARCH
            }
            else -> {}
        }
    }

    fun toggleDownloadGroupExpanded(groupId: String) {
        val current = _downloadGroups.value
        _downloadGroups.value = current.map { group ->
            if (group.id == groupId) {
                group.copy(isExpanded = !group.isExpanded)
            } else {
                group
            }
        }
    }
}
