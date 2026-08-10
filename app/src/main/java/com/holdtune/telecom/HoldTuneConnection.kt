package com.holdtune.telecom

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log

class HoldTuneConnection : Connection() {

    init {
        Log.i(TAG, "Connection initialized")
        // Set standard capabilities
        connectionCapabilities = CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD
        audioModeIsVoip = false
    }

    override fun onAnswer() {
        super.onAnswer()
        Log.i(TAG, "onAnswer called")
        setActive()
    }

    override fun onReject() {
        super.onReject()
        Log.i(TAG, "onReject called")
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    override fun onDisconnect() {
        super.onDisconnect()
        Log.i(TAG, "onDisconnect called")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        super.onMuteStateChanged(isMuted)
        Log.i(TAG, "onMuteStateChanged: $isMuted")
    }

    companion object {
        private const val TAG = "HoldTuneConnection"
    }
}
