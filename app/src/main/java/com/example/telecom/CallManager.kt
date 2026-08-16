package com.example.telecom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TelecomCallState {
    IDLE,
    RINGING,
    DIALING,
    CONNECTING,
    ACTIVE,
    HOLDING,
    DISCONNECTED
}

data class TelecomCallInfo(
    val phoneNumber: String = "",
    val displayName: String = "",
    val state: TelecomCallState = TelecomCallState.IDLE,
    val isIncoming: Boolean = false
)

object CallManager {
    private const val TAG = "RealityEngineCallManager"

    private val _activeCall = MutableStateFlow<Call?>(null)
    val activeCall: StateFlow<Call?> = _activeCall.asStateFlow()

    private val _callInfo = MutableStateFlow<TelecomCallInfo?>(null)
    val callInfo: StateFlow<TelecomCallInfo?> = _callInfo.asStateFlow()

    private var inCallServiceInstance: TelecomInCallService? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            updateCallState(call, state)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            extractCallInfo(call)
        }
    }

    fun bindInCallService(service: TelecomInCallService) {
        inCallServiceInstance = service
    }

    fun unbindInCallService() {
        inCallServiceInstance = null
    }

    fun onCallAdded(call: Call, context: Context) {
        Log.d(TAG, "Telecom onCallAdded: state=${call.state}")
        _activeCall.value = call
        call.registerCallback(callback)
        extractCallInfo(call)
        updateCallState(call, call.state)

        // Launch MainActivity automatically on incoming or outgoing call
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("EXTRA_FROM_TELECOM", true)
        }
        context.startActivity(intent)
    }

    fun onCallRemoved(call: Call) {
        Log.d(TAG, "Telecom onCallRemoved")
        call.unregisterCallback(callback)
        if (_activeCall.value == call) {
            _activeCall.value = null
            _callInfo.value = _callInfo.value?.copy(state = TelecomCallState.DISCONNECTED)
        }
    }

    private fun extractCallInfo(call: Call) {
        val handleUri: Uri? = call.details?.handle
        val phoneNumber = handleUri?.schemeSpecificPart ?: ""
        val displayName = call.details?.callerDisplayName ?: ""
        val isIncoming = call.state == Call.STATE_RINGING

        _callInfo.value = TelecomCallInfo(
            phoneNumber = phoneNumber,
            displayName = displayName,
            state = mapTelecomState(call.state),
            isIncoming = isIncoming
        )
    }

    private fun updateCallState(call: Call, state: Int) {
        val mappedState = mapTelecomState(state)
        Log.d(TAG, "Call state changed to: $mappedState (code $state)")
        _callInfo.value = _callInfo.value?.copy(state = mappedState) ?: TelecomCallInfo(state = mappedState)
    }

    private fun mapTelecomState(state: Int): TelecomCallState {
        return when (state) {
            Call.STATE_RINGING -> TelecomCallState.RINGING
            Call.STATE_DIALING -> TelecomCallState.DIALING
            Call.STATE_CONNECTING -> TelecomCallState.CONNECTING
            Call.STATE_ACTIVE -> TelecomCallState.ACTIVE
            Call.STATE_HOLDING -> TelecomCallState.HOLDING
            Call.STATE_DISCONNECTED -> TelecomCallState.DISCONNECTED
            else -> TelecomCallState.IDLE
        }
    }

    // Call Actions
    fun answerCall() {
        val call = _activeCall.value ?: return
        try {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call", e)
        }
    }

    fun declineCall() {
        val call = _activeCall.value ?: return
        try {
            if (call.state == Call.STATE_RINGING) {
                call.reject(false, null)
            } else {
                call.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error declining call", e)
        }
    }

    fun endCall() {
        val call = _activeCall.value ?: return
        try {
            call.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
        }
    }

    fun holdCall() {
        val call = _activeCall.value ?: return
        try {
            call.hold()
        } catch (e: Exception) {
            Log.e(TAG, "Error holding call", e)
        }
    }

    fun unholdCall() {
        val call = _activeCall.value ?: return
        try {
            call.unhold()
        } catch (e: Exception) {
            Log.e(TAG, "Error unholding call", e)
        }
    }

    fun setMuted(muted: Boolean) {
        inCallServiceInstance?.setMuted(muted)
    }

    fun setSpeakerphone(on: Boolean) {
        val route = if (on) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        inCallServiceInstance?.setAudioRoute(route)
    }

    fun playDtmf(digit: Char) {
        val call = _activeCall.value ?: return
        try {
            call.playDtmfTone(digit)
            call.stopDtmfTone()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending DTMF", e)
        }
    }

    /**
     * Places a real phone call via Android TelecomManager when default phone app.
     */
    fun placeOutgoingCall(context: Context, phoneNumber: String): Boolean {
        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val uri = Uri.fromParts("tel", phoneNumber, null)
            val extras = Bundle()
            telecomManager?.placeCall(uri, extras)
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException placing call via TelecomManager", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception placing call", e)
            false
        }
    }
}
