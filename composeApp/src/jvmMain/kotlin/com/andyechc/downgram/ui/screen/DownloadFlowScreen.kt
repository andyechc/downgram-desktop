package com.andyechc.downgram.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.ArrowLeft
import com.adamglin.phosphoricons.bold.Check
import com.adamglin.phosphoricons.bold.DownloadSimple
import com.adamglin.phosphoricons.bold.FileAudio
import com.adamglin.phosphoricons.bold.FileVideo
import com.adamglin.phosphoricons.bold.FolderSimple
import com.andyechc.downgram.data.model.MediaFile
import com.andyechc.downgram.ui.components.CardSkeleton
import com.andyechc.downgram.ui.components.CupertinoButton
import com.andyechc.downgram.ui.components.CupertinoCard
import com.andyechc.downgram.ui.components.CupertinoNavigationBar
import com.andyechc.downgram.ui.components.CupertinoSearchBar
import com.andyechc.downgram.ui.components.CupertinoTextField
import com.andyechc.downgram.ui.theme.DowngramThemeColors
import com.andyechc.downgram.ui.viewmodel.DownloadFlowStep
import com.andyechc.downgram.ui.viewmodel.DownloadFlowUiState
import com.andyechc.downgram.ui.viewmodel.DownloadFlowViewModel

@Composable
fun DownloadFlowScreen(
    viewModel: DownloadFlowViewModel,
    onBack: () -> Unit,
    onNavigateToActiveDownloads: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val selectedDialogs by viewModel.selectedDialogs.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPath by viewModel.selectedPath.collectAsState()
    val filteredResults by viewModel.filteredResults.collectAsState()
    val availableExtensions by viewModel.availableExtensions.collectAsState()
    val selectedExtensions by viewModel.selectedExtensions.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CupertinoNavigationBar(
                title = when (currentStep) {
                    DownloadFlowStep.CHANNEL_SELECTION -> "Canales"
                    DownloadFlowStep.MEDIA_SEARCH -> "Búsqueda"
                    DownloadFlowStep.PATH_SELECTION -> "Destino"
                    DownloadFlowStep.DOWNLOADING -> "Descargando"
                },
                navigationIcon = PhosphorIcons.Bold.ArrowLeft,
                onNavigationClick = {
                    if (currentStep == DownloadFlowStep.CHANNEL_SELECTION) {
                        onBack()
                    } else {
                        viewModel.goBack()
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            StepIndicator(currentStep)

            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    DownloadFlowStep.CHANNEL_SELECTION -> {
                        ChannelSelectionView(
                            uiState = uiState,
                            selectedDialogs = selectedDialogs,
                            onToggleSelection = { viewModel.toggleDialogSelection(it) },
                            onNext = { viewModel.proceedToSearch() }
                        )
                    }
                    DownloadFlowStep.MEDIA_SEARCH -> {
                        MediaSearchView(
                            uiState = uiState,
                            results = filteredResults,
                            availableExtensions = availableExtensions,
                            selectedExtensions = selectedExtensions,
                            onToggleExtension = { viewModel.toggleExtensionFilter(it) },
                            selectedFiles = selectedFiles,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onToggleFileSelection = { viewModel.toggleFileSelection(it) },
                            onLoadMore = { viewModel.loadMoreMedia() },
                            onNext = { viewModel.proceedToPathSelection() }
                        )
                    }
                    DownloadFlowStep.PATH_SELECTION -> {
                        PathSelectionView(
                            selectedPath = selectedPath,
                            onPathSelected = { viewModel.selectPath(it) },
                            onStartDownloads = {
                                viewModel.startDownloads()
                                onNavigateToActiveDownloads()
                            }
                        )
                    }
                    DownloadFlowStep.DOWNLOADING -> {
                        LaunchedEffect(Unit) { onNavigateToActiveDownloads() }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: DownloadFlowStep) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DownloadFlowStep.values().forEachIndexed { index, step ->
            val isActive = step == currentStep
            val isCompleted = step.ordinal < currentStep.ordinal

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isActive -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(PhosphorIcons.Bold.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        (index + 1).toString(),
                        color = if (isActive) MaterialTheme.colorScheme.background else DowngramThemeColors.textTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (index < DownloadFlowStep.values().size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
fun ChannelSelectionView(
    uiState: DownloadFlowUiState,
    selectedDialogs: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                is DownloadFlowUiState.Loading -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(6) { CardSkeleton() }
                    }
                }
                is DownloadFlowUiState.ChannelSelectionLoaded -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.dialogs, key = { it.id }) { dialog ->
                            val isSelected = selectedDialogs.contains(dialog.id)
                            CupertinoCard(
                                onClick = { onToggleSelection(dialog.id) },
                                isSelected = isSelected
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            dialog.title,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            dialog.type.uppercase(),
                                            color = DowngramThemeColors.textTertiary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSelection(dialog.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                            uncheckedColor = DowngramThemeColors.textTertiary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                is DownloadFlowUiState.Error -> {
                    Text(
                        uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {}
            }
        }

        Spacer(Modifier.height(24.dp))

        CupertinoButton(
            text = "CONTINUAR",
            onClick = onNext,
            enabled = selectedDialogs.isNotEmpty()
        )
    }
}

@Composable
fun MediaSearchView(
    uiState: DownloadFlowUiState,
    results: List<MediaFile>,
    availableExtensions: Set<String>,
    selectedExtensions: Set<String>,
    onToggleExtension: (String) -> Unit,
    selectedFiles: Set<Long>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleFileSelection: (Long) -> Unit,
    onLoadMore: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))

        CupertinoSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Buscar en los canales seleccionados...",
            autoFocus = true
        )

        if (availableExtensions.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableExtensions.toList().sorted()) { ext ->
                    val isSelected = selectedExtensions.contains(ext)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleExtension(ext) },
                        label = {
                            Text(
                                ext.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            labelColor = DowngramThemeColors.textTertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState is DownloadFlowUiState.Loading && results.isEmpty() -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(8) { CardSkeleton() }
                    }
                }
                results.isNotEmpty() -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results, key = { it.id }) { file ->
                            val isSelected = selectedFiles.contains(file.id)
                            MediaFileCard(file, isSelected) { onToggleFileSelection(file.id) }
                        }

                        if (uiState is DownloadFlowUiState.Loading) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        } else if (uiState is DownloadFlowUiState.SearchResultsLoaded && uiState.hasMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    Button(
                                        onClick = onLoadMore,
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Cargar más", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                uiState is DownloadFlowUiState.SearchResultsLoaded -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No se encontraron resultados",
                            color = DowngramThemeColors.textTertiary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Prueba con otra palabra clave",
                            color = DowngramThemeColors.textTertiary.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
                uiState is DownloadFlowUiState.Error -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error en la búsqueda", color = MaterialTheme.colorScheme.error, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.message, color = DowngramThemeColors.textTertiary, fontSize = 13.sp)
                    }
                }
            }
        }

        CupertinoButton(
            text = "CONTINUAR (${selectedFiles.size})",
            onClick = onNext,
            enabled = selectedFiles.isNotEmpty(),
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
fun MediaFileCard(
    file: MediaFile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        DowngramThemeColors.cardElevatedColor
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .then(
                if (isSelected) Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (file.media_type == "video") PhosphorIcons.Bold.FileVideo else PhosphorIcons.Bold.FileAudio,
                    null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val primaryText = file.message.ifEmpty { file.title ?: file.filename ?: "Sin descripción" }
                Text(
                    primaryText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 14.sp,
                    maxLines = 2
                )
                val subtitle = when {
                    file.filename != null -> "➜ ${file.filename}"
                    file.title != null -> "🎵 $file.title"
                    else -> "${(file.file_size / 1024 / 1024)} MB • ${file.channel_title}"
                }
                Text(
                    subtitle,
                    color = DowngramThemeColors.textTertiary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = DowngramThemeColors.textTertiary
                )
            )
        }
    }
}

@Composable
fun PathSelectionView(
    selectedPath: String,
    onPathSelected: (String) -> Unit,
    onStartDownloads: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
                PhosphorIcons.Bold.FolderSimple,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "Ruta de destino",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Selecciona dónde guardar tus archivos",
            style = MaterialTheme.typography.bodyMedium,
            color = DowngramThemeColors.textTertiary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        CupertinoTextField(
            value = selectedPath,
            onValueChange = onPathSelected,
            placeholder = "Ejem: /Downloads/Telegram"
        )

        Spacer(Modifier.height(16.dp))

        CupertinoButton(
            text = "Seleccionar carpeta",
            onClick = {
                val chooser = javax.swing.JFileChooser()
                chooser.dialogTitle = "Seleccionar carpeta de descarga"
                chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                if (selectedPath.isNotBlank()) {
                    chooser.selectedFile = java.io.File(selectedPath)
                }
                if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                    onPathSelected(chooser.selectedFile.absolutePath)
                }
            },
            icon = PhosphorIcons.Bold.FolderSimple
        )

        Spacer(Modifier.height(48.dp))

        CupertinoButton(
            text = "INICIAR DESCARGAS",
            onClick = onStartDownloads,
            enabled = selectedPath.isNotBlank(),
            icon = PhosphorIcons.Bold.DownloadSimple
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Los archivos se descargarán en segundo plano",
            style = MaterialTheme.typography.bodySmall,
            color = DowngramThemeColors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}
