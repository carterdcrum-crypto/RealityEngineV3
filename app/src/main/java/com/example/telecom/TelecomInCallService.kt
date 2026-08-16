package com.example.telecom

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class TelecomInCallService : InCallService() {

    companion object {
        private const val TAG = "TelecomInCallService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TelecomInCallService created")
        CallManager.bindInCallService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TelecomInCallService destroyed")
        CallManager.unbindInCallService()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "onCallAdded: $call")
        CallManager.onCallAdded(call, applicationContext)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved: $call")
        CallManager.onCallRemoved(call)
    }
}
