package com.andyechc.downgram.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0s"
    if (seconds < 60) return "${seconds}s"

    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return when {
        hours > 0 -> {
            if (minutes > 0) "${hours}h ${minutes}m"
            else "${hours}h"
        }
        minutes > 0 -> {
            if (remainingSeconds > 0) "${minutes}m ${remainingSeconds}s"
            else "${minutes}m"
        }
        else -> "${remainingSeconds}s"
    }
}
