package com.example.telecom

/**
 * Authoritative CallState enum representing real-world telephony and voice call status
 * tracked from the Twilio Voice SDK / REST API and Android Telecom subsystem.
 */
enum class CallState(val displayName: String) {
    IDLE("Idle"),
    RINGING("Ringing"),
    CONNECTING("Connecting"),
    ACTIVE("Active"),
    HOLDING("On Hold"),
    DISCONNECTED("Disconnected");

    val isConnecting: Boolean
        get() = this == CONNECTING || this == RINGING

    val isActive: Boolean
        get() = this == ACTIVE || this == HOLDING

    val isDisconnected: Boolean
        get() = this == DISCONNECTED || this == IDLE

    val isLive: Boolean
        get() = this == CONNECTING || this == RINGING || this == ACTIVE || this == HOLDING

    companion object {
        /**
         * Maps raw Twilio call statuses (queued, ringing, in-progress, completed, busy, failed, no-answer, canceled)
         * to the authoritative CallState enum.
         */
        fun fromTwilioStatus(status: String): CallState {
            return when (status.lowercase().trim()) {
                "queued", "initiating", "ringing" -> CONNECTING
                "in-progress", "in_progress", "active", "answered" -> ACTIVE
                "completed", "busy", "failed", "no-answer", "no_answer", "canceled" -> DISCONNECTED
                else -> if (status.isNotBlank()) CONNECTING else IDLE
            }
        }

        /**
         * Maps Android Telecom Call.state integer codes to the authoritative CallState enum.
         */
        fun fromTelecomState(state: Int): CallState {
            return when (state) {
                android.telecom.Call.STATE_RINGING -> RINGING
                android.telecom.Call.STATE_DIALING,
                android.telecom.Call.STATE_CONNECTING,
                android.telecom.Call.STATE_SELECT_PHONE_ACCOUNT -> CONNECTING
                android.telecom.Call.STATE_ACTIVE -> ACTIVE
                android.telecom.Call.STATE_HOLDING -> HOLDING
                android.telecom.Call.STATE_DISCONNECTED,
                android.telecom.Call.STATE_DISCONNECTING -> DISCONNECTED
                else -> IDLE
            }
        }
    }
}
