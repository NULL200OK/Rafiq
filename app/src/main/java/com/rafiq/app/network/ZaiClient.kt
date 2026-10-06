package com.rafiq.app.network

import com.rafiq.app.config.LlmConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthExpiredException(message: String = "API key expired or invalid") : Exception(message)

sealed class StreamEvent {
    object Started : StreamEvent()
    data class Chunk(val text: String) : StreamEvent()
    object Done : StreamEvent()
    object AuthExpired : StreamEvent()
    data class Failed(val error: Exception) : StreamEvent()
}

object ZaiClient {

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun test(config: LlmConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("model", config.model)
                .put("max_tokens", 5)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "ping")))

            val request = Request.Builder()
                .url("${config.baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonType))
                .build()

            http.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> Unit
                    response.code == 401 || response.code == 403 -> throw AuthExpiredException()
                    else -> throw IOException("HTTP ${response.code}")
                }
            }
        }
    }

    fun streamChat(config: LlmConfig, messages: List<Pair<String, String>>): Flow<StreamEvent> =
        callbackFlow {
            val body = JSONObject()
                .put("model", config.model)
                .put("stream", true)
                .put("messages", JSONArray().apply {
                    messages.forEach { (role, content) ->
                        put(JSONObject().put("role", role).put("content", content))
                    }
                })

            val request = Request.Builder()
                .url("${config.baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody(jsonType))
                .build()

            val call: Call = http.newCall(request)

            Thread {
                try {
                    call.execute().use { response ->
                        when {
                            response.code == 401 || response.code == 403 -> {
                                trySend(StreamEvent.AuthExpired); close(); return@Thread
                            }
                            !response.isSuccessful -> {
                                trySend(StreamEvent.Failed(IOException("HTTP ${response.code}")))
                                close(); return@Thread
                            }
                        }

                        val source = response.body?.source()
                        if (source == null) {
                            trySend(StreamEvent.Failed(IOException("Empty response body")))
                            close(); return@Thread
                        }

                        trySend(StreamEvent.Started)
                        while (true) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data:")) continue
                            val payload = line.removePrefix("data:").trim()
                            if (payload == "[DONE]") break
                            val delta = runCatching {
                                JSONObject(payload)
                                    .optJSONArray("choices")?.optJSONObject(0)
                                    ?.optJSONObject("delta")?.optString("content")
                            }.getOrNull()
                            if (!delta.isNullOrEmpty()) trySend(StreamEvent.Chunk(delta))
                        }
                        trySend(StreamEvent.Done)
                        close()
                    }
                } catch (e: Exception) {
                    if (!call.isCanceled()) trySend(StreamEvent.Failed(e))
                    close()
                }
            }.start()

            awaitClose { call.cancel() }
        }
}