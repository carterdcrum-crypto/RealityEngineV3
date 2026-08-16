package com.example.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class TranscriberState {
    IDLE,
    CONNECTING,
    LISTENING,
    ERROR,
    STOPPED
}

class DeepgramLiveTranscriber(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
) {
    companion object {
        private const val TAG = "DeepgramLiveTranscriber"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val _state = MutableStateFlow(TranscriberState.IDLE)
    val state: StateFlow<TranscriberState> = _state.asStateFlow()

    private val _transcriptFlow = MutableSharedFlow<TranscriptSegment>(extraBufferCapacity = 64)
    val transcriptFlow: SharedFlow<TranscriptSegment> = _transcriptFlow.asSharedFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startStreaming(deepgramApiKey: String) {
        if (deepgramApiKey.isBlank()) {
            Log.w(TAG, "Deepgram API key is blank; transcription cannot start.")
            _state.value = TranscriberState.ERROR
            return
        }

        stopStreaming()

        _state.value = TranscriberState.CONNECTING

        val wsUrl = "wss://api.deepgram.com/v1/listen?model=nova-2&smart_format=true&diarize=true&interim_results=true&encoding=linear16&sample_rate=$SAMPLE_RATE"
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Token $deepgramApiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Deepgram WebSocket Connected")
                _state.value = TranscriberState.LISTENING
                startAudioCapture(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                parseDeepgramResponse(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Deepgram WebSocket Failure: ${t.message}", t)
                _state.value = TranscriberState.ERROR
                stopAudioCapture()
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Deepgram WebSocket Closing: $code / $reason")
                _state.value = TranscriberState.STOPPED
                stopAudioCapture()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _state.value = TranscriberState.STOPPED
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture(ws: WebSocket) {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

                var record: AudioRecord? = null
                val audioSources = listOf(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.DEFAULT
                )

                for (source in audioSources) {
                    try {
                        val testRecord = AudioRecord(source, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
                        if (testRecord.state == AudioRecord.STATE_INITIALIZED) {
                            record = testRecord
                            break
                        } else {
                            testRecord.release()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed source $source: ${e.message}")
                    }
                }

                if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "Unable to initialize AudioRecord")
                    _state.value = TranscriberState.ERROR
                    return@launch
                }

                audioRecord = record
                record.startRecording()

                val audioBuffer = ByteArray(bufferSize)

                while (isActive && _state.value == TranscriberState.LISTENING) {
                    val bytesRead = record.read(audioBuffer, 0, audioBuffer.size)
                    if (bytesRead > 0) {
                        // Calculate RMS amplitude for visualizer
                        var sum = 0.0
                        for (i in 0 until bytesRead step 2) {
                            val sample = (audioBuffer[i].toInt() and 0xFF) or (audioBuffer[i + 1].toInt() shl 8)
                            val sampleShort = sample.toShort()
                            sum += sampleShort * sampleShort
                        }
                        val rms = Math.sqrt(sum / (bytesRead / 2.0))
                        val normalizedAmp = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        _audioAmplitude.value = normalizedAmp

                        // Stream bytes to Deepgram
                        val byteString = audioBuffer.toByteString(0, bytesRead)
                        ws.send(byteString)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio capture loop: ${e.message}", e)
                _state.value = TranscriberState.ERROR
            } finally {
                stopAudioCapture()
            }
        }
    }

    private fun parseDeepgramResponse(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val channel = root.optJSONObject("channel") ?: return
            val alternatives = channel.optJSONArray("alternatives") ?: return
            if (alternatives.length() == 0) return

            val alt = alternatives.getJSONObject(0)
            val transcript = alt.optString("transcript", "").trim()
            val isFinal = root.optBoolean("is_final", false)
            val speechFinal = root.optBoolean("speech_final", false)

            if (transcript.isNotBlank()) {
                // Check words array for speaker diarization
                var speaker = 0
                val words = alt.optJSONArray("words")
                if (words != null && words.length() > 0) {
                    speaker = words.getJSONObject(0).optInt("speaker", 0)
                }

                val isYou = speaker == 0
                val speakerEnum = if (isYou) Speaker.YOU else Speaker.OTHER
                val speakerLabel = if (isYou) "You" else "Contact"

                val segment = TranscriptSegment(
                    id = UUID.randomUUID().toString(),
                    speaker = speakerEnum,
                    speakerName = speakerLabel,
                    text = transcript,
                    timestamp = java.text.SimpleDateFormat("mm:ss", java.util.Locale.US).format(java.util.Date()),
                    isFinal = isFinal || speechFinal,
                    linguisticDistance = 0.2f,
                    stressLevel = 0.2f
                )

                scope.launch {
                    _transcriptFlow.emit(segment)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Deepgram response: ${e.message}")
        }
    }

    private fun stopAudioCapture() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
            _audioAmplitude.value = 0f
        }
    }

    fun stopStreaming() {
        recordingJob?.cancel()
        recordingJob = null
        stopAudioCapture()

        try {
            // Send CloseStream message to Deepgram
            webSocket?.send(ByteArray(0).toByteString())
            webSocket?.close(1000, "Normal Closure")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing WebSocket: ${e.message}")
        } finally {
            webSocket = null
            _state.value = TranscriberState.STOPPED
        }
    }
}
