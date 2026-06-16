package com.andyechc.downgram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.andyechc.downgram.data.repository.TelegramRepository
import com.andyechc.downgram.ui.screen.ActiveDownloadsScreen
import com.andyechc.downgram.ui.screen.DownloadFlowScreen
import com.andyechc.downgram.ui.screen.LoginScreen
import com.andyechc.downgram.ui.screen.MainScreen
import com.andyechc.downgram.ui.screen.OtpScreen
import com.andyechc.downgram.ui.screen.SettingsScreen
import com.andyechc.downgram.ui.screen.SplashScreen
import com.andyechc.downgram.ui.theme.DowngramTheme
import com.andyechc.downgram.ui.viewmodel.DownloadFlowViewModel
import com.andyechc.downgram.ui.viewmodel.MainViewModel
import com.andyechc.downgram.ui.viewmodel.SettingsViewModel

enum class AppState {
    SPLASH, CONFIG, OTP, MAIN, DOWNLOAD_FLOW, ACTIVE_DOWNLOADS, SETTINGS
}

fun main() = application {
    val backendService = remember { BackendService() }
    val settingsViewModel = remember { SettingsViewModel() }

    var appState by remember {
        mutableStateOf(if (CredentialManager.isConfigured()) AppState.SPLASH else AppState.CONFIG)
    }
    var backendPort by remember { mutableStateOf(8000) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val windowState = rememberWindowState(width = 960.dp, height = 720.dp)

    Window(
        onCloseRequest = {
            backendService.stop()
            exitApplication()
        },
        title = "Downgram Desktop",
        state = windowState
    ) {
        val themeMode by settingsViewModel.themeMode.collectAsState()

        DowngramTheme(themeMode = themeMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (appState) {
                    AppState.CONFIG -> {
                        LoginScreen(onSuccess = { appState = AppState.SPLASH })
                    }
                    AppState.SPLASH -> {
                        SplashScreen(
                            onReady = { port ->
                                backendPort = port
                                appState = AppState.MAIN
                            },
                            onNeedOtp = { port ->
                                backendPort = port
                                appState = AppState.OTP
                            },
                            onError = { msg -> errorMessage = msg },
                            backendService = backendService
                        )
                    }
                    AppState.OTP -> {
                        key(backendPort) {
                            OtpScreen(
                                backendPort = backendPort,
                                onSuccess = { appState = AppState.MAIN },
                                onBack = { appState = AppState.CONFIG }
                            )
                        }
                    }
                    AppState.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { appState = AppState.MAIN }
                        )
                    }
                    AppState.MAIN, AppState.DOWNLOAD_FLOW, AppState.ACTIVE_DOWNLOADS -> {
                        key(backendPort) {
                            val repository = remember { TelegramRepository(backendPort) }
                            val mainViewModel = remember { MainViewModel(repository) }
                            val downloadFlowViewModel = remember { DownloadFlowViewModel(repository) }

                            when (appState) {
                                AppState.MAIN -> {
                                    MainScreen(
                                        viewModel = mainViewModel,
                                        onLogout = {
                                            CredentialManager.clear()
                                            appState = AppState.CONFIG
                                        },
                                        onNewDownload = {
                                            downloadFlowViewModel.startFlow()
                                            appState = AppState.DOWNLOAD_FLOW
                                        },
                                        onActiveDownloads = { appState = AppState.ACTIVE_DOWNLOADS },
                                        onOpenSettings = { appState = AppState.SETTINGS }
                                    )
                                }
                                AppState.DOWNLOAD_FLOW -> {
                                    DownloadFlowScreen(
                                        viewModel = downloadFlowViewModel,
                                        onBack = {
                                            downloadFlowViewModel.resetFlow()
                                            appState = AppState.MAIN
                                        },
                                        onNavigateToActiveDownloads = { appState = AppState.ACTIVE_DOWNLOADS }
                                    )
                                }
                                AppState.ACTIVE_DOWNLOADS -> {
                                    val downloadGroups by mainViewModel.downloadGroups.collectAsState()
                                    ActiveDownloadsScreen(
                                        downloadGroups = downloadGroups,
                                        onToggleExpand = { mainViewModel.toggleDownloadGroupExpanded(it) },
                                        onBack = { appState = AppState.MAIN }
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }

                errorMessage?.let { msg ->
                    AlertDialog(
                        onDismissRequest = { errorMessage = null },
                        title = {
                            Text(
                                "Error",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                errorMessage = null
                                appState = AppState.CONFIG
                            }) {
                                Text(
                                    "Aceptar",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        textContentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
