package com.holdtune.telecom

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.holdtune.audio.AudioBridge
import com.holdtune.audio.OngoingCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class HoldTuneInCallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var answerJob: Job? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.i(TAG, "Call state changed: ${stateToString(state)}")
            handleCallStateChange(call, state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "HoldTuneInCallService created")
        instance = this
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val number = call.details.handle?.schemeSpecificPart ?: "Unknown"
        Log.i(TAG, "onCallAdded: Call received from number: $number")

        // 1. EMERGENCY BYPASS (Strict Check)
        if (isEmergencyCall(call)) {
            Log.i(TAG, "Bypassing HoldTune: Call is identified as an emergency call. Falling through to default system call handling.")
            return
        }

        // 2. CHECK IF INTERCEPTION IS ENABLED & WHITELIST PASSED
        if (!shouldInterceptCall(number)) {
            Log.i(TAG, "Bypassing HoldTune: Number $number not whitelisted or service disabled.")
            return
        }

        // Register callback to track call state changes
        call.registerCallback(callCallback)
        
        // Update global call references
        OngoingCallService.updateCall(call)
        OngoingCallService.setHoldState(true)

        // 3. START AUTO-ANSWER TIMER (IF RINGING)
        if (call.state == Call.STATE_RINGING) {
            val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
            val delaySeconds = prefs.getInt("auto_answer_delay", 2)
            Log.i(TAG, "Scheduling auto-answer in $delaySeconds seconds")

            answerJob = serviceScope.launch {
                delay(delaySeconds * 1000L)
                if (call.state == Call.STATE_RINGING) {
                    Log.i(TAG, "Auto-answering call now.")
                    try {
                        call.answer(VideoProfile.STATE_AUDIO_ONLY)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to auto-answer call", e)
                    }
                }
            }
        } else if (call.state == Call.STATE_ACTIVE) {
            // If already answered elsewhere or self-answered
            handleCallActive(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i(TAG, "onCallRemoved")
        call.unregisterCallback(callCallback)
        
        // Clean up audio and service
        AudioBridge.stop()
        answerJob?.cancel()
        
        // Update state to hide call UI
        OngoingCallService.updateCall(null)
        
        // Stop foreground service
        val stopServiceIntent = Intent(this, OngoingCallService::class.java).apply {
            action = OngoingCallService.ACTION_STOP
        }
        startService(stopServiceIntent)
        
        if (activeCall == call) {
            activeCall = null
        }
    }

    private fun handleCallStateChange(call: Call, state: Int) {
        when (state) {
            Call.STATE_ACTIVE -> {
                handleCallActive(call)
            }
            Call.STATE_DISCONNECTED -> {
                onCallRemoved(call)
            }
        }
    }

    private fun handleCallActive(call: Call) {
        activeCall = call
        Log.i(TAG, "Call is active. Starting private hold music and muting microphone.")

        // Start Ongoing Call Foreground Service to prevent OS kill
        val startServiceIntent = Intent(this, OngoingCallService::class.java).apply {
            action = OngoingCallService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startServiceIntent)
        } else {
            startService(startServiceIntent)
        }

        // Mute user's mic immediately
        try {
            setMuted(true)
            Log.i(TAG, "Microphone mute state set to TRUE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mute microphone", e)
        }

        // Play the hold audio track locally via VoiceCommunication usage
        val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
        val selectedPath = prefs.getString("hold_track_path", "") ?: ""
        
        val audioFile = if (selectedPath.isNotEmpty()) {
            File(selectedPath)
        } else {
            // Default built-in file location fallback
            File(filesDir, "recorded_hold_track.3gp")
        }

        if (audioFile.exists()) {
            Log.i(TAG, "Playing local hold music file: ${audioFile.absolutePath}")
            val success = AudioBridge.start(audioFile.absolutePath)
            if (!success) {
                Log.e(TAG, "Failed to start AudioBridge playback")
            }
        } else {
            Log.w(TAG, "No hold track file exists at path: ${audioFile.absolutePath}. Silence will play.")
        }
    }

    /**
     * Checks if the incoming call is an emergency number or is in emergency callback mode.
     */
    fun isEmergencyCall(call: Call): Boolean {
        // Distinct named function, easy to test
        val handle = call.details.handle
        val number = handle?.schemeSpecificPart

        if (number != null) {
            // Standard Android API for emergency numbers lookup
            if (PhoneNumberUtils.isEmergencyNumber(number)) {
                Log.i(TAG, "isEmergencyCall: Number $number is classified as emergency.")
                return true
            }
        }

        // Check if the call is in emergency callback mode property
        if (call.details.hasProperty(Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE)) {
            Log.i(TAG, "isEmergencyCall: Call has PROPERTY_EMERGENCY_CALLBACK_MODE set.")
            return true
        }

        return false
    }

    private fun shouldInterceptCall(number: String): Boolean {
        val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
        val isServiceEnabled = prefs.getBoolean("service_enabled", true)
        if (!isServiceEnabled) return false

        val isGlobalEnabled = prefs.getBoolean("global_enabled", true)
        if (isGlobalEnabled) return true

        // Whitelist mode check
        val whitelistString = prefs.getString("whitelist_contacts", "") ?: ""
        if (whitelistString.isEmpty()) return false

        val whitelistedNumbers = whitelistString.split(",").map { it.trim() }
        for (whiteNumber in whitelistedNumbers) {
            if (PhoneNumberUtils.compare(number, whiteNumber)) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "HoldTuneInCallService destroyed")
        if (instance == this) {
            instance = null
        }
    }

    companion object {
        private const val TAG = "HoldTuneInCallService"
        
        private var instance: HoldTuneInCallService? = null
        fun getInstance(): HoldTuneInCallService? = instance

        var activeCall: Call? = null
            private set

        private fun stateToString(state: Int): String {
            return when (state) {
                Call.STATE_NEW -> "NEW"
                Call.STATE_DIALING -> "DIALING"
                Call.STATE_RINGING -> "RINGING"
                Call.STATE_HOLDING -> "HOLDING"
                Call.STATE_ACTIVE -> "ACTIVE"
                Call.STATE_DISCONNECTED -> "DISCONNECTED"
                Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
                else -> "UNKNOWN ($state)"
            }
        }
    }
}
