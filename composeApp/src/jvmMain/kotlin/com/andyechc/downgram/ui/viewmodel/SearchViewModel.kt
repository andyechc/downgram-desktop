package com.andyechc.downgram.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andyechc.downgram.data.model.MediaFile
import com.andyechc.downgram.data.repository.TelegramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MediaTypeFilter { ALL, VIDEO, AUDIO }

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val media: List<MediaFile>,
        val totalFound: Int,
        val hasMore: Boolean,
        val currentPage: Int
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val repository: TelegramRepository,
    private val channelId: Long,
    private var downloadPath: String = System.getProperty("user.home") + "/Downloads/Downgram"
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _selectedFiles = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFiles: StateFlow<Set<Long>> = _selectedFiles.asStateFlow()

    private val _currentFilter = MutableStateFlow(MediaTypeFilter.ALL)
    val currentFilter: StateFlow<MediaTypeFilter> = _currentFilter.asStateFlow()

    private val _useOriginalFilename = MutableStateFlow(true)
    val useOriginalFilename: StateFlow<Boolean> = _useOriginalFilename.asStateFlow()

    // Flow for filtered media results
    val filteredMedia: StateFlow<List<MediaFile>> = combine(_uiState, _currentFilter) { state, filter ->
        if (state is SearchUiState.Success) {
            when (filter) {
                MediaTypeFilter.ALL -> state.media
                MediaTypeFilter.VIDEO -> state.media.filter { it.media_type == "video" }
                MediaTypeFilter.AUDIO -> state.media.filter { it.media_type == "audio" }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentKeyword = ""
    private var currentPage = 0
    private var allMedia = mutableListOf<MediaFile>()

    fun setDownloadPath(path: String) {
        downloadPath = path
    }

    fun setFilter(filter: MediaTypeFilter) {
        _currentFilter.value = filter
    }

    fun setUseOriginalFilename(use: Boolean) {
        _useOriginalFilename.value = use
    }

    fun search(keyword: String, page: Int = 0) {
        currentKeyword = keyword
        currentPage = page
        if (page == 0) allMedia.clear()
        
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            repository.search(listOf(channelId), keyword, page)
                .onSuccess { response ->
                    if (page == 0) {
                        allMedia = response.media.toMutableList()
                    } else {
                        allMedia.addAll(response.media)
                    }
                    
                    _uiState.value = SearchUiState.Success(
                        media = allMedia.toList(),
                        totalFound = response.total_found,
                        hasMore = response.has_more,
                        currentPage = page
                    )
                }
                .onFailure { error ->
                    _uiState.value = SearchUiState.Error(error.message ?: "Error en la búsqueda")
                }
        }
    }

    fun loadNextPage() {
        if (uiState.value is SearchUiState.Success) {
            search(currentKeyword, currentPage + 1)
        }
    }

    fun toggleSelection(mediaId: Long) {
        val current = _selectedFiles.value
        if (current.contains(mediaId)) {
            _selectedFiles.value = current - mediaId
        } else {
            _selectedFiles.value = current + mediaId
        }
    }

    fun downloadSelected() {
        val selected = _selectedFiles.value
        val useOriginal = _useOriginalFilename.value
        val currentMedia = (uiState.value as? SearchUiState.Success)?.media ?: emptyList()

        viewModelScope.launch {
            selected.forEach { id ->
                val mediaFile = currentMedia.find { it.id == id }
                val customName = if (useOriginal && mediaFile?.filename != null) {
                    mediaFile.filename
                } else null
                
                repository.download(id, downloadPath, customName)
            }
            _selectedFiles.value = emptySet()
        }
    }
}
