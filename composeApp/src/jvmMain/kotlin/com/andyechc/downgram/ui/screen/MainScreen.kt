package com.andyechc.downgram.ui.screen

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.DownloadSimple
import com.adamglin.phosphoricons.bold.Gear
import com.adamglin.phosphoricons.bold.Plus
import com.adamglin.phosphoricons.bold.SignOut
import com.adamglin.phosphoricons.regular.CalendarBlank
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.FolderSimple
import com.adamglin.phosphoricons.regular.HardDrive
import com.adamglin.phosphoricons.regular.Megaphone
import com.andyechc.downgram.CredentialManager
import com.andyechc.downgram.data.model.DownloadHistoryItem
import com.andyechc.downgram.data.model.FileDownloadStatus
import com.andyechc.downgram.ui.components.CupertinoNavigationBar
import com.andyechc.downgram.ui.theme.DowngramThemeColors
import com.andyechc.downgram.ui.utils.formatDate
import com.andyechc.downgram.ui.utils.formatFileSize
import com.andyechc.downgram.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onNewDownload: () -> Unit,
    onActiveDownloads: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadHistory by viewModel.downloadHistory.collectAsState()
    val notificationMessage by viewModel.notificationMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadDialogsNonBlocking()
        viewModel.loadDownloadHistory()
    }

    LaunchedEffect(notificationMessage) {
        notificationMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CupertinoNavigationBar(
                title = "Downgram",
                navigationIcon = PhosphorIcons.Bold.SignOut,
                onNavigationClick = {
                    viewModel.logout()
                    CredentialManager.clear()
                    onLogout()
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                PhosphorIcons.Bold.Gear,
                                contentDescription = "Ajustes",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = onActiveDownloads) {
                            val downloadGroups by viewModel.downloadGroups.collectAsState()
                            val activeCount = downloadGroups.sumOf { group ->
                                group.files.count {
                                    it.status == FileDownloadStatus.DOWNLOADING ||
                                    it.status == FileDownloadStatus.PENDING
                                }
                            }
                            Box {
                                Icon(
                                    PhosphorIcons.Bold.DownloadSimple,
                                    contentDescription = "Descargas activas",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (activeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            activeCount.toString(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewDownload,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    PhosphorIcons.Bold.Plus,
                    contentDescription = "Nueva descarga",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (downloadHistory.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "HISTORIAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DowngramThemeColors.textTertiary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                    }
                    items(downloadHistory, key = { it.id }) { historyItem ->
                        DownloadHistoryCard(item = historyItem)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(DowngramThemeColors.cardColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                PhosphorIcons.Bold.DownloadSimple,
                contentDescription = null,
                tint = DowngramThemeColors.textTertiary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Sin descargas aún",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Toca el botón + para empezar a bajar contenido.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = DowngramThemeColors.textTertiary
        )
    }
}

@Composable
fun DownloadHistoryCard(item: DownloadHistoryItem) {
    val cardBg = DowngramThemeColors.cardElevatedColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        PhosphorIcons.Regular.CalendarBlank,
                        contentDescription = null,
                        tint = DowngramThemeColors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatDate(item.downloadDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = DowngramThemeColors.textTertiary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${item.fileCount} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    PhosphorIcons.Regular.FolderSimple,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    item.path,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                color = DowngramThemeColors.separatorColor,
                thickness = 0.5.dp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(PhosphorIcons.Regular.HardDrive, formatFileSize(item.totalSpace))
                    InfoChip(PhosphorIcons.Regular.FileText, item.extensions.take(2).joinToString(", "))
                }

                if (item.channelNames.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            PhosphorIcons.Regular.Megaphone,
                            contentDescription = null,
                            tint = DowngramThemeColors.textTertiary,
                            modifier = Modifier.size(14.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            item.channelNames.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = DowngramThemeColors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = DowngramThemeColors.textTertiary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = DowngramThemeColors.textTertiary
        )
    }
}
