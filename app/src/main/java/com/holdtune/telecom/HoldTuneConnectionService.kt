package com.holdtune.telecom

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

class HoldTuneConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.i(TAG, "onCreateIncomingConnection called")
        val connection = HoldTuneConnection().apply {
            setAddress(request?.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
            setCallerDisplayName(request?.extras?.getString(android.telecom.TelecomManager.EXTRA_INCOMING_CALL_ADDRESS), android.telecom.TelecomManager.PRESENTATION_ALLOWED)
        }
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.i(TAG, "onCreateOutgoingConnection called")
        val connection = HoldTuneConnection().apply {
            setAddress(request?.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
        }
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
        Log.e(TAG, "onCreateIncomingConnectionFailed")
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
        Log.e(TAG, "onCreateOutgoingConnectionFailed")
    }

    companion object {
        private const val TAG = "HoldTuneConnectionService"
    }
}
