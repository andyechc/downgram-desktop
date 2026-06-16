package com.andyechc.downgram

import java.io.File
import java.net.ServerSocket
import java.util.prefs.Preferences
import java.util.concurrent.TimeUnit
import java.net.URI

object CredentialManager {
    private val prefs = Preferences.userNodeForPackage(CredentialManager::class.java)

    fun saveCredentials(apiId: String, apiHash: String, phone: String) {
        prefs.put("API_ID", apiId)
        prefs.put("API_HASH", apiHash)
        prefs.put("PHONE", phone)
    }

    fun getCredentials(): Map<String, String>? {
        val id = prefs.get("API_ID", null) ?: return null
        val hash = prefs.get("API_HASH", null) ?: return null
        val phone = prefs.get("PHONE", null) ?: return null
        return mapOf("API_ID" to id, "API_HASH" to hash, "PHONE" to phone)
    }

    fun isConfigured(): Boolean = prefs.get("API_ID", null) != null
    
    fun clear() = prefs.clear()
}

class BackendService {
    private var process: Process? = null
    var currentPort: Int = 8000
        private set

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    fun start(onReady: (Int) -> Unit, onError: (String) -> Unit) {
        val creds = CredentialManager.getCredentials()
        if (creds == null) {
            onError("No credentials found")
            return
        }

        currentPort = findFreePort()

        Thread {
            try {
                val isWindows = System.getProperty("os.name").lowercase().contains("win")
                val homeDir = System.getProperty("user.home")
                val persistentDir = File(homeDir, ".downgram").apply { 
                    if (!exists()) mkdirs() 
                }

                val packagedResDir = System.getProperty("compose.application.resources.dir")
                val workingDir: File
                val command: List<String>

                val exeName = if (isWindows) "downgram-backend.exe" else "downgram-backend"
                val resDir = packagedResDir?.let { File(it) }
                val exeCandidates = listOfNotNull(
                    resDir?.let { File(it, exeName) },
                    resDir?.let { File(it, "common/$exeName") }
                ).filter { it.exists() }
                if (exeCandidates.isNotEmpty()) {
                    val exeFile = exeCandidates.first()
                    workingDir = exeFile.parentFile
                    if (!isWindows) exeFile.setExecutable(true)
                    command = listOf(exeFile.absolutePath, "--port", currentPort.toString())
                    println("[BackendService] Production mode: ${exeFile.absolutePath}")
                } else if (resDir != null) {
                    // Packaged app but binary not found — try running main.py with system Python
                    val pyDirs = listOf(resDir, File(resDir, "common"))
                    val pyDir = pyDirs.firstOrNull { File(it, "main.py").exists() }
                    if (pyDir == null) {
                        onError("No se encontró el backend en la app empaquetada ($packagedResDir). Asegúrate de que backend-bin/common/ contenga downgram-backend o los archivos .py necesarios y vuelve a empaquetar la app.")
                        return@Thread
                    }
                    workingDir = pyDir
                    val python = if (isWindows) "python" else "python3"
                    command = listOf(python, "main.py", "--port", currentPort.toString())
                    println("[BackendService] Packaged fallback mode: ${pyDir.absolutePath}")
                } else {
                    // Development mode — find main.py relative to user.dir
                    val userDir = File(System.getProperty("user.dir"))
                    val candidates = listOf(
                        File(userDir, "backend"),
                        File(userDir, "composeApp/backend")
                    ).filter { it.exists() && File(it, "main.py").exists() }

                    if (candidates.isEmpty()) {
                        onError("No se pudo encontrar backend/main.py. Ejecuta la app desde el directorio del proyecto.")
                        return@Thread
                    }

                    workingDir = candidates.first()
                    val venvPython = if (isWindows) {
                        File(workingDir, "venv/Scripts/python.exe")
                    } else {
                        File(workingDir, "venv/bin/python3")
                    }
                    val pythonInterpreter = if (venvPython.exists()) {
                        venvPython.absolutePath
                    } else {
                        if (isWindows) "python" else "python3"
                    }
                    command = listOf(pythonInterpreter, "main.py", "--port", currentPort.toString())
                    println("[BackendService] Development mode: ${workingDir.absolutePath}")
                }
                
                println("[BackendService] Persisting session at: ${persistentDir.absolutePath}")
                
                val pb = ProcessBuilder(command)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .apply {
                        environment()["PORT"] = currentPort.toString()
                        environment()["SESSION_DIR"] = persistentDir.absolutePath
                    }
                
                process = pb.start()
                
                Thread {
                    process?.inputStream?.bufferedReader()?.useLines { lines ->
                        lines.forEach { line ->
                            println("[Backend] $line")
                        }
                    }
                }.start()

                var attempts = 0
                while (attempts < 20) {
                    if (isBackendAlive()) {
                        onReady(currentPort)
                        return@Thread
                    }
                    Thread.sleep(1000)
                    attempts++
                }
                
                onError("Timeout starting backend.")
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            }
        }.start()
    }

    private fun isBackendAlive(): Boolean {
        return try {
            val url = URI("http://127.0.0.1:$currentPort/health").toURL()
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 500
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        process?.destroy()
    }
}
