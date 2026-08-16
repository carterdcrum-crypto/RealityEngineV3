package com.example.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Deepgram SDK Client providing:
 * 1. Live Streaming Transcription (Nova-2 STT with diarization) via [DeepgramLiveTranscriber]
 * 2. Pre-recorded Audio REST Transcription
 * 3. Token & Project Health Validation
 */
class DeepgramSdkClient(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "DeepgramSdkClient"
        private const val BASE_REST_URL = "https://api.deepgram.com/v1"
        const val DEFAULT_MODEL = "nova-2"
    }

    /**
     * Creates a new live stream transcriber instance configured for Nova-2.
     */
    fun createLiveTranscriber(): DeepgramLiveTranscriber {
        return DeepgramLiveTranscriber()
    }

    /**
     * Validates that the provided Deepgram API key is valid and has active project access.
     */
    suspend fun validateApiKey(): Result<DeepgramProjectInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Deepgram API Key cannot be empty"))
        }

        try {
            val request = Request.Builder()
                .url("$BASE_REST_URL/projects")
                .header("Authorization", "Token $apiKey")
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val projects = json.optJSONArray("projects")
                    val firstProject = if (projects != null && projects.length() > 0) {
                        val p = projects.getJSONObject(0)
                        DeepgramProjectInfo(
                            projectId = p.optString("project_id", "active"),
                            name = p.optString("name", "Deepgram Project"),
                            isValid = true
                        )
                    } else {
                        DeepgramProjectInfo(projectId = "valid", name = "Deepgram Account", isValid = true)
                    }
                    Result.success(firstProject)
                } else {
                    Result.failure(Exception("Deepgram validation failed (HTTP ${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating Deepgram API key: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Transcribes pre-recorded audio bytes (e.g. WAV or PCM) using Deepgram Nova-2 REST API.
     */
    suspend fun transcribeAudioBytes(
        audioBytes: ByteArray,
        mimeType: String = "audio/wav",
        model: String = DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Deepgram API Key is missing"))
        }

        try {
            val url = "$BASE_REST_URL/listen?model=$model&smart_format=true&diarize=true&punctuate=true"
            val mediaType = mimeType.toMediaType()
            val requestBody = audioBytes.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Token $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val results = json.optJSONObject("results")
                    val channels = results?.optJSONArray("channels")
                    val firstChannel = channels?.optJSONObject(0)
                    val alternatives = firstChannel?.optJSONArray("alternatives")
                    val transcript = alternatives?.optJSONObject(0)?.optString("transcript", "").orEmpty()
                    Result.success(transcript)
                } else {
                    Result.failure(Exception("Deepgram REST transcription failed (HTTP ${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Deepgram REST transcription error: ${e.message}", e)
            Result.failure(e)
        }
    }
}

data class DeepgramProjectInfo(
    val projectId: String,
    val name: String,
    val isValid: Boolean
)
