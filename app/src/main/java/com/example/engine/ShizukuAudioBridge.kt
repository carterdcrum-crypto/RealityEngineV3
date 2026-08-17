package com.example.engine

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Privileged-audio integration point for Reality Engine.
 *
 * Shizuku provides a privileged execution channel, but it does not itself
 * guarantee access to the remote side of a cellular call. Android's audio
 * privacy rules still apply, so the call-audio backend remains isolated here
 * until it is validated on the target device.
 */
class ShizukuAudioBridge(private val context: Context) {
    companion object {
        private const val TAG = "RealityEngineShizuku"
        private const val REQUEST_CODE = 2401
    }

    enum class Status {
        UNAVAILABLE, NOT_AUTHORIZED, READY, CAPTURE_RESTRICTED, RUNNING, STOPPED, ERROR
    }

    @Volatile
    var status: Status = Status.UNAVAILABLE
        private set

    private var recorder: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var running = false

    fun refreshStatus(): Status {
        status = when {
            !Shizuku.pingBinder() -> Status.UNAVAILABLE
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> Status.NOT_AUTHORIZED
            else -> Status.READY
        }
        return status
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder()) {
            status = Status.UNAVAILABLE
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            status = Status.READY
            return
        }
        Shizuku.requestPermission(REQUEST_CODE)
    }

    /**
     * Starts a microphone capture probe. This intentionally does not claim to
     * capture the remote cellular-call leg; that requires a separately verified
     * privileged audio backend on the target Android/Samsung build.
     */
    fun startMicrophoneProbe(onPcm: (ByteArray, Int) -> Unit): Boolean {
        if (refreshStatus() != Status.READY) return false
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status = Status.CAPTURE_RESTRICTED
            return false
        }

        return try {
            val minBuffer = AudioRecord.getMinBufferSize(
                16_000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) {
                status = Status.ERROR
                return false
            }

            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16_000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
            recorder?.startRecording()
            running = true
            status = Status.RUNNING

            captureThread = Thread {
                val buffer = ByteArray(minBuffer)
                try {
                    while (running) {
                        val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) onPcm(buffer.copyOf(read), 16_000)
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Audio probe failed", t)
                    status = Status.ERROR
                } finally {
                    runCatching { recorder?.stop() }
                    runCatching { recorder?.release() }
                    recorder = null
                }
            }.apply {
                name = "RealityEngine-ShizukuAudio"
                start()
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Unable to start audio probe", t)
            status = Status.ERROR
            stop()
            false
        }
    }

    fun stop() {
        running = false
        captureThread?.interrupt()
        captureThread = null
        recorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        recorder = null
        if (status == Status.RUNNING) status = Status.STOPPED
    }
}
