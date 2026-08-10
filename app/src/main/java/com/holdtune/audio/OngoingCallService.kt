package com.holdtune.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.util.Log
import androidx.core.app.NotificationCompat
import com.holdtune.HoldTuneApp
import com.holdtune.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OngoingCallService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "OngoingCallService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand action: $action")
        
        when (action) {
            ACTION_START -> {
                startForegroundNotification()
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        // Using standard system icon to avoid missing resource compile issues
        val notification: Notification = NotificationCompat.Builder(this, HoldTuneApp.CHANNEL_ID)
            .setContentTitle("HoldTune Active")
            .setContentText("A call is currently intercepted on hold.")
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "OngoingCallService destroyed")
    }

    companion object {
        private const val TAG = "OngoingCallService"
        private const val NOTIFICATION_ID = 101
        
        const val ACTION_START = "com.holdtune.action.START_CALL"
        const val ACTION_STOP = "com.holdtune.action.STOP_CALL"

        // Global state flow of the active call to recompose Compose UI
        private val _callState = MutableStateFlow<Call?>(null)
        val callState: StateFlow<Call?> = _callState

        // Stores whether we are currently in hold state
        private val _isHoldState = MutableStateFlow(true)
        val isHoldState: StateFlow<Boolean> = _isHoldState

        fun updateCall(call: Call?) {
            _callState.value = call
            if (call == null) {
                _isHoldState.value = true // Reset
            }
        }

        fun setHoldState(isHold: Boolean) {
            _isHoldState.value = isHold
        }
    }
}
