package com.andyechc.downgram.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.ArrowLeft
import com.adamglin.phosphoricons.bold.CaretDown
import com.adamglin.phosphoricons.bold.CaretUp
import com.adamglin.phosphoricons.bold.CheckCircle
import com.adamglin.phosphoricons.bold.DownloadSimple
import com.adamglin.phosphoricons.bold.Pause
import com.adamglin.phosphoricons.bold.Play
import com.andyechc.downgram.data.model.ActiveDownloadGroup
import com.andyechc.downgram.data.model.FileDownloadStatus
import com.andyechc.downgram.ui.components.CupertinoNavigationBar
import com.andyechc.downgram.ui.components.CupertinoProgressBar
import com.andyechc.downgram.ui.theme.DowngramThemeColors
import com.andyechc.downgram.ui.utils.formatDuration
import com.andyechc.downgram.ui.utils.formatFileSize

@Composable
fun ActiveDownloadsScreen(
    downloadGroups: List<ActiveDownloadGroup>,
    onToggleExpand: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        CupertinoNavigationBar(
            title = "Descargas Activas",
            navigationIcon = PhosphorIcons.Bold.ArrowLeft,
            onNavigationClick = onBack
        )

        if (downloadGroups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(DowngramThemeColors.cardColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            PhosphorIcons.Bold.DownloadSimple,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = DowngramThemeColors.textTertiary
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "No hay descargas activas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DowngramThemeColors.textTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(downloadGroups, key = { it.id }) { group ->
                    DownloadGroupCard(
                        group = group,
                        onToggleExpand = { onToggleExpand(group.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadGroupCard(group: ActiveDownloadGroup, onToggleExpand: () -> Unit) {
    val cardBg = DowngramThemeColors.cardElevatedColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Grupo de Descarga",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "${group.completedFiles} de ${group.totalFiles} archivos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${formatFileSize(group.downloadedSize)} / ${formatFileSize(group.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DowngramThemeColors.textTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (group.isExpanded) PhosphorIcons.Bold.CaretUp else PhosphorIcons.Bold.CaretDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            CupertinoProgressBar(
                progress = group.overallProgress / 100f,
                height = 8.dp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${group.overallProgress}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                if (group.downloadSpeed > 0) {
                    Text(
                        "${formatFileSize(group.downloadSpeed)}/s • ${formatDuration(group.remainingTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DowngramThemeColors.textTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(visible = group.isExpanded) {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    HorizontalDivider(
                        color = DowngramThemeColors.separatorColor,
                        thickness = 0.5.dp
                    )
                    group.files.forEach { file ->
                        FileDownloadItem(file = file)
                    }
                }
            }
        }
    }
}

@Composable
fun FileDownloadItem(file: com.andyechc.downgram.data.model.ActiveDownloadFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (file.status == FileDownloadStatus.COMPLETED)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (file.status) {
                    FileDownloadStatus.COMPLETED -> PhosphorIcons.Bold.CheckCircle
                    FileDownloadStatus.PAUSED -> PhosphorIcons.Bold.Play
                    else -> if (file.status == FileDownloadStatus.DOWNLOADING) PhosphorIcons.Bold.Pause
                    else PhosphorIcons.Bold.DownloadSimple
                },
                contentDescription = null,
                tint = if (file.status == FileDownloadStatus.COMPLETED) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                file.filename,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                file.description,
                style = MaterialTheme.typography.labelSmall,
                color = DowngramThemeColors.textTertiary
            )

            if (file.status == FileDownloadStatus.DOWNLOADING) {
                Spacer(Modifier.height(8.dp))
                CupertinoProgressBar(
                    progress = file.progress / 100f,
                    height = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (file.status == FileDownloadStatus.DOWNLOADING) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${file.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatFileSize(file.downloadSpeed) + "/s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
