package com.example.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.twilio.voice.Call as TwilioVoiceCall
import com.twilio.voice.CallException
import com.twilio.voice.ConnectOptions
import com.twilio.voice.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Authoritative CallService for Android Telecom and Twilio Voice SDK integration.
 * Extends InCallService to act as the primary telephony call handler when set as the Default Dialer,
 * and provides foreground call management, audio routing, and notification controls.
 */
class CallService : InCallService() {

    companion object {
        const val TAG = "RealityEngineCallService"
        const val CHANNEL_ID = "reality_engine_call_channel"
        const val NOTIFICATION_ID = 40401

        const val ACTION_HANGUP = "com.example.telecom.ACTION_HANGUP"
        const val ACTION_ANSWER = "com.example.telecom.ACTION_ANSWER"
        const val ACTION_TOGGLE_MUTE = "com.example.telecom.ACTION_TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.example.telecom.ACTION_TOGGLE_SPEAKER"

        private val _serviceState = MutableStateFlow<CallState>(CallState.IDLE)
        val serviceState: StateFlow<CallState> = _serviceState.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _isSpeakerOn = MutableStateFlow(false)
        val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

        private val _activeTwilioSdkCall = MutableStateFlow<TwilioVoiceCall?>(null)
        val activeTwilioSdkCall: StateFlow<TwilioVoiceCall?> = _activeTwilioSdkCall.asStateFlow()

        private var instance: CallService? = null

        fun getInstance(): CallService? = instance

        fun answerCall() {
            instance?.let { service ->
                CallManager.answerCall()
            }
        }

        fun disconnectCall() {
            // Disconnect Twilio Voice SDK call if present
            _activeTwilioSdkCall.value?.disconnect()
            _activeTwilioSdkCall.value = null

            // Disconnect Telecom in-call service call
            CallManager.endCall()
            _serviceState.value = CallState.DISCONNECTED
        }

        fun sendDtmf(digit: Char) {
            // Send DTMF to Twilio SDK Call
            _activeTwilioSdkCall.value?.sendDigits(digit.toString())
            // Send DTMF to Telecom Call
            CallManager.playDtmf(digit)
        }

        fun toggleMute(): Boolean {
            val service = instance
            val newMute = !_isMuted.value
            _isMuted.value = newMute

            // Apply to Twilio SDK call
            _activeTwilioSdkCall.value?.mute(newMute)

            // Apply to Telecom service
            service?.setMuted(newMute)

            service?.updateNotification()
            return newMute
        }

        fun toggleSpeaker(context: Context): Boolean {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val newSpeaker = !_isSpeakerOn.value
            _isSpeakerOn.value = newSpeaker

            audioManager?.isSpeakerphoneOn = newSpeaker
            instance?.setAudioRoute(if (newSpeaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
            instance?.updateNotification()
            return newSpeaker
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var durationJob: Job? = null
    private var callDurationSeconds = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "CallService created")
        CallManager.bindInCallService(this)
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallService destroyed")
        durationJob?.cancel()
        CallManager.unbindInCallService()
        if (instance == this) {
            instance = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HANGUP -> {
                Log.d(TAG, "Notification ACTION_HANGUP received")
                disconnectCall()
            }
            ACTION_ANSWER -> {
                Log.d(TAG, "Notification ACTION_ANSWER received")
                answerCall()
            }
            ACTION_TOGGLE_MUTE -> {
                Log.d(TAG, "Notification ACTION_TOGGLE_MUTE received")
                toggleMute()
            }
            ACTION_TOGGLE_SPEAKER -> {
                Log.d(TAG, "Notification ACTION_TOGGLE_SPEAKER received")
                toggleSpeaker(this)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "onCallAdded: $call (state=${call.state})")
        CallManager.onCallAdded(call, applicationContext)
        _serviceState.value = CallState.fromTelecomState(call.state)
        startCallForeground(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved: $call")
        CallManager.onCallRemoved(call)
        _serviceState.value = CallState.DISCONNECTED
        durationJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            _isMuted.value = it.isMuted
            _isSpeakerOn.value = (it.route == CallAudioState.ROUTE_SPEAKER)
            updateNotification()
        }
    }

    /**
     * Connects an outgoing call via the official Twilio Voice SDK using a registration Access Token.
     */
    fun connectTwilioSdkCall(
        accessToken: String,
        params: Map<String, String>,
        listener: TwilioVoiceCall.Listener? = null
    ): TwilioVoiceCall? {
        if (accessToken.isBlank()) {
            Log.w(TAG, "Twilio Access Token is blank; cannot connect Voice SDK call.")
            return null
        }

        try {
            val connectOptions = ConnectOptions.Builder(accessToken)
                .params(params)
                .build()

            val callListener = object : TwilioVoiceCall.Listener {
                override fun onConnectFailure(call: TwilioVoiceCall, callException: CallException) {
                    Log.e(TAG, "Twilio Voice SDK Connect Failure: ${callException.message}", callException)
                    _serviceState.value = CallState.DISCONNECTED
                    _activeTwilioSdkCall.value = null
                    listener?.onConnectFailure(call, callException)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }

                override fun onRinging(call: TwilioVoiceCall) {
                    Log.d(TAG, "Twilio Voice SDK Ringing: ${call.sid}")
                    _serviceState.value = CallState.RINGING
                    listener?.onRinging(call)
                    updateNotification()
                }

                override fun onConnected(call: TwilioVoiceCall) {
                    Log.d(TAG, "Twilio Voice SDK Connected: ${call.sid}")
                    _serviceState.value = CallState.ACTIVE
                    _activeTwilioSdkCall.value = call
                    startDurationTimer()
                    listener?.onConnected(call)
                    updateNotification()
                }

                override fun onReconnecting(call: TwilioVoiceCall, callException: CallException) {
                    Log.w(TAG, "Twilio Voice SDK Reconnecting: ${callException.message}")
                    listener?.onReconnecting(call, callException)
                }

                override fun onReconnected(call: TwilioVoiceCall) {
                    Log.d(TAG, "Twilio Voice SDK Reconnected: ${call.sid}")
                    listener?.onReconnected(call)
                }

                override fun onDisconnected(call: TwilioVoiceCall, callException: CallException?) {
                    Log.d(TAG, "Twilio Voice SDK Disconnected: ${call.sid}")
                    _serviceState.value = CallState.DISCONNECTED
                    _activeTwilioSdkCall.value = null
                    durationJob?.cancel()
                    listener?.onDisconnected(call, callException)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }

            val call = Voice.connect(this, connectOptions, callListener)
            _activeTwilioSdkCall.value = call
            _serviceState.value = CallState.CONNECTING
            startForeground(NOTIFICATION_ID, buildNotification("Connecting Voice Call...", "Twilio Voice Engine"))
            return call
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting Twilio Voice call: ${e.message}", e)
            _serviceState.value = CallState.DISCONNECTED
            return null
        }
    }

    private fun startCallForeground(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: "Phone Call"
        val state = CallState.fromTelecomState(call.state)
        val title = when (state) {
            CallState.RINGING -> "Incoming Call from $number"
            CallState.CONNECTING -> "Dialing $number..."
            CallState.ACTIVE -> "Active Call with $number"
            else -> "Call with $number"
        }

        val notification = buildNotification(title, state.displayName)
        startForeground(NOTIFICATION_ID, notification)

        if (state == CallState.ACTIVE) {
            startDurationTimer()
        }
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        callDurationSeconds = 0
        durationJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                callDurationSeconds++
                if (callDurationSeconds % 2 == 0) {
                    updateNotification()
                }
            }
        }
    }

    fun updateNotification() {
        val info = CallManager.callInfo.value
        val number = info?.phoneNumber?.ifBlank { "Live Audio Session" } ?: "Live Audio Session"
        val durationFormatted = String.format("%02d:%02d", callDurationSeconds / 60, callDurationSeconds % 60)
        val state = _serviceState.value

        val title = when (state) {
            CallState.RINGING -> "Incoming Call: $number"
            CallState.CONNECTING -> "Connecting: $number"
            CallState.ACTIVE -> "Ongoing Call: $number ($durationFormatted)"
            CallState.HOLDING -> "Call on Hold: $number"
            else -> "Reality Engine Call"
        }

        val text = "AI Co-Pilot & Transcription Active"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangupIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_HANGUP
        }
        val hangupPendingIntent = PendingIntent.getService(
            this,
            1,
            hangupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            2,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                hangupPendingIntent
            )
            .addAction(
                if (_isMuted.value) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_lock_silent_mode_off,
                if (_isMuted.value) "Unmute" else "Mute",
                mutePendingIntent
            )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reality Engine Voice Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active Call & Real-time AI Co-Pilot status"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
