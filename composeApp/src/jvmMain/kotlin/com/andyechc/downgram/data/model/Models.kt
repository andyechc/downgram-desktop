package com.andyechc.downgram.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TelegramDialog(
    val id: Long,
    val title: String,
    val type: String
)

@Serializable
data class LoginRequest(
    val api_id: Int,
    val api_hash: String,
    val phone: String
)

@Serializable
data class SendCodeRequest(
    val api_id: Int,
    val api_hash: String,
    val phone: String
)

@Serializable
data class VerifyCodeRequest(
    val api_id: Int,
    val api_hash: String,
    val phone: String,
    val code: String,
    val password: String? = null
)

@Serializable
data class SendCodeResponse(
    val status: String,
    val need_code: Boolean,
    val phone: String? = null,
    val error: String? = null
)

@Serializable
data class VerifyCodeResponse(
    val status: String,
    val success: Boolean,
    val need_password: Boolean? = null,
    val error: String? = null
)

@Serializable
data class SearchRequest(
    val entities_ids: List<Long>,
    val keyword: String,
    val offset: Int = 0,
    val limit: Int = 50
)

@Serializable
data class DownloadRequest(
    val media_id: Long,
    val path: String,
    val filename: String? = null
)

@Serializable
data class MediaFile(
    val id: Long,
    val date: String,
    val channel_title: String,
    val message: String,
    val file_size: Long,
    val media_type: String,
    val filename: String? = null,
    val title: String? = null
)

@Serializable
data class SearchResponse(
    val media: List<MediaFile>,
    val total_found: Int,
    val has_more: Boolean
)

@Serializable
data class DownloadStatus(
    val id: Long,
    val filename: String,
    val progress: Int,
    val status: String,
    val size: Long,
    val download_speed: Long? = 0,
    val eta: Long? = 0
)

// New models for download history and active downloads

@Serializable
data class DownloadHistoryItem(
    val id: String,
    val downloadDate: String,
    val path: String,
    val totalSpace: Long,
    val fileCount: Int,
    val extensions: List<String>,
    val channelNames: List<String>
)

data class ActiveDownloadGroup(
    val id: String,
    val totalFiles: Int,
    val completedFiles: Int,
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val downloadSpeed: Long,
    val remainingTime: Long,
    val overallProgress: Int,
    val files: List<ActiveDownloadFile>,
    val isExpanded: Boolean = false
)

data class ActiveDownloadFile(
    val id: Long,
    val filename: String,
    val description: String,
    val progress: Int,
    val fileSize: Long = 0,
    val downloadSpeed: Long = 0,
    val remainingTime: Long = 0,
    val status: FileDownloadStatus
)

enum class FileDownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

@Serializable
data class DownloadHistoryResponse(
    val history: List<DownloadHistoryItem>
)
