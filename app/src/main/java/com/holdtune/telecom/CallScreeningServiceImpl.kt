package com.holdtune.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val number = handle?.schemeSpecificPart ?: "Unknown"
        Log.i(TAG, "onScreenCall: screening incoming call from: $number")

        // HoldTune does not block cellular calls itself (it only intercepts them in InCallService)
        // So we build a response that allows the call to ring normally
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }

    companion object {
        private const val TAG = "CallScreeningServiceImpl"
    }
}
