package com.andyechc.downgram.data.repository

import com.andyechc.downgram.data.model.*
import io.ktor.client.statement.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// Logging simple
private fun log(msg: String) {
    println("[TelegramRepository] $msg")
}

class TelegramRepository(private val port: Int) {
    init {
        log("TelegramRepository created with port: $port")
    }
    
    private val client = HttpClient {
        install(WebSockets)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }

    private val baseUrl = "http://127.0.0.1:$port"

    suspend fun sendCode(apiId: Int, apiHash: String, phone: String): Result<SendCodeResponse> = runCatching {
        val response = client.post("$baseUrl/send_code") {
            contentType(ContentType.Application.Json)
            setBody(SendCodeRequest(apiId, apiHash, phone))
        }
        response.body<SendCodeResponse>()
    }

    suspend fun verifyCode(apiId: Int, apiHash: String, phone: String, code: String, password: String? = null): Result<VerifyCodeResponse> = runCatching {
        log("Sending verify_code request to $baseUrl/verify_code")
        log("Request body: apiId=$apiId, phone=$phone, code=$code")
        
        val response = client.post("$baseUrl/verify_code") {
            contentType(ContentType.Application.Json)
            setBody(VerifyCodeRequest(apiId, apiHash, phone, code, password))
        }
        
        val rawBody = response.bodyAsText()
        log("Response status: ${response.status}")
        log("Response body: $rawBody")
        
        Json.decodeFromString(VerifyCodeResponse.serializer(), rawBody)
    }

    suspend fun getDialogs(): Result<List<TelegramDialog>> = runCatching {
        log("Fetching dialogs from $baseUrl/dialogs")
        val response = client.get("$baseUrl/dialogs")
        log("Get dialogs response status: ${response.status}")
        val body = response.bodyAsText()
        log("Get dialogs response body: $body")
        Json.decodeFromString<List<TelegramDialog>>(body)
    }

    suspend fun search(entitiesIds: List<Long>, keyword: String, offset: Int = 0, limit: Int = 50): Result<SearchResponse> = runCatching {
        client.post("$baseUrl/search") {
            contentType(ContentType.Application.Json)
            setBody(SearchRequest(entitiesIds, keyword, offset, limit))
        }.body()
    }

    suspend fun download(mediaId: Long, path: String, filename: String? = null): Result<Unit> = runCatching {
        client.post("$baseUrl/download") {
            contentType(ContentType.Application.Json)
            setBody(DownloadRequest(mediaId, path, filename))
        }
    }

    fun observeDownloadProgress(): Flow<List<DownloadStatus>> = callbackFlow {
        val wsUrl = "ws://127.0.0.1:$port/ws/downloads"
        log("Starting WebSocket observer: $wsUrl")
        
        var attempt = 0
        val maxDelay = 30000L // Max 30 seconds between retries
        
        while (isActive) {
            try {
                log("WebSocket connecting (attempt ${attempt + 1})...")
                client.webSocket(wsUrl) {
                    log("WebSocket connection established")
                    attempt = 0 // Reset counter on successful connection
                    
                    // Send ping periodically to keep connection alive
                    launch {
                        while (isActive) {
                            delay(25000) // Send ping every 25 seconds
                            try {
                                send(Frame.Text("ping"))
                            } catch (e: Exception) {
                                log("Ping failed: ${e.message}")
                                break
                            }
                        }
                    }
                    
                    // Receive messages
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val text = frame.readText()
                                if (text == "pong") {
                                    // Heartbeat response, ignore
                                    continue
                                }
                                try {
                                    val statuses = Json { ignoreUnknownKeys = true }
                                        .decodeFromString<List<DownloadStatus>>(text)
                                    trySend(statuses)
                                } catch (e: Exception) {
                                    log("Failed to parse download status: ${e.message}")
                                }
                            }
                            is Frame.Close -> {
                                log("WebSocket closed by server")
                                break
                            }
                            else -> { /* Ignore other frames */ }
                        }
                    }
                }
            } catch (e: Exception) {
                log("WebSocket error: ${e.message}")
            }
            
            // Exponential backoff before reconnecting
            attempt++
            val delayMs = minOf(1000L * attempt, maxDelay)
            log("WebSocket reconnecting in ${delayMs}ms...")
            delay(delayMs)
        }
        
        awaitClose { 
            log("WebSocket observer closing")
        }
    }

    suspend fun getDownloads(): Result<List<DownloadStatus>> = runCatching {
        client.get("$baseUrl/downloads").body()
    }

    suspend fun getDownloadHistory(): Result<List<DownloadHistoryItem>> = runCatching {
        log("Fetching download history from $baseUrl/download_history")
        val response = client.get("$baseUrl/download_history")
        val body = response.body<DownloadHistoryResponse>()
        body.history
    }

    suspend fun setMaxConcurrent(max: Int): Result<Unit> = runCatching {
        client.post("$baseUrl/set_concurrent") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("max_concurrent" to max))
        }
    }

    fun close() {
        client.close()
    }
}
