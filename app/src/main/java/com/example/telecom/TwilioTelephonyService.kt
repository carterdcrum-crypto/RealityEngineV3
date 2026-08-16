package com.example.telecom

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ActiveTwilioCall(
    val callSid: String,
    val from: String,
    val to: String,
    val state: CallState,
    val rawTwilioStatus: String = "",
    val durationSeconds: Int = 0,
    val startTimestamp: Long = System.currentTimeMillis()
)

class TwilioTelephonyService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "TwilioTelephonyService"
        private const val BASE_URL = "https://api.twilio.com/2010-04-01/Accounts"
    }

    private val _currentCall = MutableStateFlow<ActiveTwilioCall?>(null)
    val currentCall: StateFlow<ActiveTwilioCall?> = _currentCall.asStateFlow()

    private var isPolling = false

    suspend fun placeCall(
        accountSid: String,
        authToken: String,
        fromNumber: String,
        toNumber: String,
        twimlOrUrl: String? = null
    ): Result<ActiveTwilioCall> = withContext(Dispatchers.IO) {
        if (accountSid.isBlank() || authToken.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Twilio Account SID and Auth Token must be configured in Settings."))
        }
        if (fromNumber.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Twilio Outgoing Phone Number must be configured in Settings."))
        }
        if (toNumber.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Target phone number cannot be empty."))
        }

        try {
            val url = "$BASE_URL/$accountSid/Calls.json"
            val credential = Credentials.basic(accountSid, authToken)

            val formBuilder = FormBody.Builder()
                .add("To", toNumber)
                .add("From", fromNumber)

            if (!twimlOrUrl.isNullOrBlank()) {
                if (twimlOrUrl.startsWith("http://") || twimlOrUrl.startsWith("https://")) {
                    formBuilder.add("Url", twimlOrUrl)
                } else {
                    formBuilder.add("Twiml", twimlOrUrl)
                }
            } else {
                // Default TwiML: announce call and bridge to audio/voice
                val defaultTwiml = "<Response><Say voice=\"alice\">Connecting Reality Engine active co-pilot call.</Say><Pause length=\"1\"/><Dial>$toNumber</Dial></Response>"
                formBuilder.add("Twiml", defaultTwiml)
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .post(formBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val sid = json.optString("sid", "")
                    val statusStr = json.optString("status", "queued")
                    val state = CallState.fromTwilioStatus(statusStr)

                    val activeCall = ActiveTwilioCall(
                        callSid = sid,
                        from = fromNumber,
                        to = toNumber,
                        state = state,
                        rawTwilioStatus = statusStr,
                        startTimestamp = System.currentTimeMillis()
                    )
                    _currentCall.value = activeCall
                    startStatusPolling(accountSid, authToken, sid)
                    Result.success(activeCall)
                } else {
                    Log.e(TAG, "Twilio place call failed HTTP ${response.code}: $body")
                    val errorMsg = try {
                        JSONObject(body).optString("message", "HTTP ${response.code}")
                    } catch (e: Exception) {
                        "HTTP ${response.code}"
                    }
                    Result.failure(Exception("Twilio call failed: $errorMsg"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception placing Twilio call: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun endCall(
        accountSid: String,
        authToken: String,
        callSid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            isPolling = false
            val url = "$BASE_URL/$accountSid/Calls/$callSid.json"
            val credential = Credentials.basic(accountSid, authToken)

            val formBody = FormBody.Builder()
                .add("Status", "completed")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                _currentCall.value = _currentCall.value?.copy(
                    state = CallState.DISCONNECTED,
                    rawTwilioStatus = "completed"
                )
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to end call: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendDtmf(
        accountSid: String,
        authToken: String,
        callSid: String,
        digit: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/$accountSid/Calls/$callSid.json"
            val credential = Credentials.basic(accountSid, authToken)

            // Update call with DTMF Play instructions
            val dtmfTwiml = "<Response><Play digits=\"$digit\"/></Response>"
            val formBody = FormBody.Builder()
                .add("Twiml", dtmfTwiml)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to send DTMF: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun startStatusPolling(accountSid: String, authToken: String, callSid: String) {
        isPolling = true
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            while (isPolling) {
                delay(2000)
                try {
                    val url = "$BASE_URL/$accountSid/Calls/$callSid.json"
                    val credential = Credentials.basic(accountSid, authToken)
                    val request = Request.Builder()
                        .url(url)
                        .header("Authorization", credential)
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val json = JSONObject(body)
                            val statusStr = json.optString("status", "")
                            val durationSec = json.optInt("duration", 0)
                            val newState = CallState.fromTwilioStatus(statusStr)

                            _currentCall.value = _currentCall.value?.copy(
                                state = newState,
                                rawTwilioStatus = statusStr,
                                durationSeconds = durationSec
                            )

                            if (newState.isDisconnected) {
                                isPolling = false
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Twilio polling error: ${e.message}")
                }
            }
        }
    }
}
