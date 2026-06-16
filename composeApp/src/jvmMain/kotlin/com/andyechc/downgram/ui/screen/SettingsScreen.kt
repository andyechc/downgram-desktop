package com.andyechc.downgram.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.bold.ArrowLeft
import com.adamglin.phosphoricons.bold.DownloadSimple
import com.adamglin.phosphoricons.bold.FolderSimple
import com.adamglin.phosphoricons.bold.Moon
import com.adamglin.phosphoricons.bold.Palette
import com.adamglin.phosphoricons.bold.Sun
import com.andyechc.downgram.ui.components.CupertinoButton
import com.andyechc.downgram.ui.components.CupertinoGroupedCard
import com.andyechc.downgram.ui.components.CupertinoNavigationBar
import com.andyechc.downgram.ui.components.CupertinoSegmentedControl
import com.andyechc.downgram.ui.components.CupertinoTextField
import com.andyechc.downgram.ui.components.CupertinoToggle
import com.andyechc.downgram.ui.theme.DowngramThemeColors
import com.andyechc.downgram.ui.theme.ThemeMode
import com.andyechc.downgram.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
    val useDefaultPath by viewModel.useDefaultPath.collectAsState()
    val defaultDownloadPath by viewModel.defaultDownloadPath.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CupertinoNavigationBar(
            title = "Ajustes",
            navigationIcon = PhosphorIcons.Bold.ArrowLeft,
            onNavigationClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ─── Appearance Section ───
            Text(
                "APARIENCIA",
                style = MaterialTheme.typography.labelSmall,
                color = DowngramThemeColors.textTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            CupertinoGroupedCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                PhosphorIcons.Bold.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Tema",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(
                        color = DowngramThemeColors.separatorColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    CupertinoSegmentedControl(
                        selectedValue = themeMode,
                        options = listOf(
                            ThemeMode.SYSTEM to "Automático",
                            ThemeMode.LIGHT to "Claro",
                            ThemeMode.DARK to "Oscuro"
                        ),
                        onValueChange = { viewModel.setThemeMode(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Spacer(Modifier.height(12.dp))
                }
            }

            // ─── Downloads Section ───
            Text(
                "DESCARGAS",
                style = MaterialTheme.typography.labelSmall,
                color = DowngramThemeColors.textTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            CupertinoGroupedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            PhosphorIcons.Bold.DownloadSimple,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Descargas simultáneas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Máximo de archivos descargándose a la vez",
                            style = MaterialTheme.typography.bodySmall,
                            color = DowngramThemeColors.textTertiary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = maxConcurrentDownloads > 1) {
                                    viewModel.setMaxConcurrentDownloads(maxConcurrentDownloads - 1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${maxConcurrentDownloads}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = maxConcurrentDownloads < 16) {
                                    viewModel.setMaxConcurrentDownloads(maxConcurrentDownloads + 1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            CupertinoGroupedCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Usar carpeta predeterminada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Omitir la selección de carpeta al descargar",
                                style = MaterialTheme.typography.bodySmall,
                                color = DowngramThemeColors.textTertiary
                            )
                        }
                        CupertinoToggle(
                            checked = useDefaultPath,
                            onCheckedChange = { viewModel.setUseDefaultPath(it) }
                        )
                    }

                    if (useDefaultPath) {
                        HorizontalDivider(
                            color = DowngramThemeColors.separatorColor,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        CupertinoTextField(
                            value = defaultDownloadPath,
                            onValueChange = { viewModel.setDefaultDownloadPath(it) },
                            placeholder = "~/Downloads/Downgram",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        CupertinoButton(
                            text = "Seleccionar carpeta",
                            onClick = {
                                val chooser = javax.swing.JFileChooser()
                                chooser.dialogTitle = "Carpeta predeterminada"
                                chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                                if (defaultDownloadPath.isNotBlank()) {
                                    chooser.selectedFile = java.io.File(defaultDownloadPath)
                                }
                                if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                                    viewModel.setDefaultDownloadPath(chooser.selectedFile.absolutePath)
                                }
                            },
                            icon = PhosphorIcons.Bold.FolderSimple,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // ─── About Section ───
            Text(
                "ACERCA DE",
                style = MaterialTheme.typography.labelSmall,
                color = DowngramThemeColors.textTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )

            CupertinoGroupedCard {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Downgram Desktop",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Versión 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = DowngramThemeColors.textTertiary
                            )
                        }
                    }

                    HorizontalDivider(
                        color = DowngramThemeColors.separatorColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Basado en Downgram CLI",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DowngramThemeColors.textTertiary
                        )
                    }

                    HorizontalDivider(
                        color = DowngramThemeColors.separatorColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Hecho con ❤️ por andyechc",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DowngramThemeColors.textTertiary
                        )
                    }
                }
            }
        }
    }
}
