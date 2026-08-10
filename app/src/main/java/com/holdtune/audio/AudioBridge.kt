package com.holdtune.audio

import android.telecom.InCallService
import android.util.Log

object AudioBridge {
    private const val TAG = "AudioBridge"

    init {
        try {
            System.loadLibrary("holdtune")
            Log.i(TAG, "Native library 'holdtune' loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library 'holdtune'", e)
        }
    }

    /**
     * Start playing the hold track from the given file path.
     * Decodes and streams the audio to the device earpiece/speaker (VoiceCommunication).
     */
    fun start(filePath: String): Boolean {
        Log.i(TAG, "start() called with path: $filePath")
        return try {
            nativeStart(filePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error in nativeStart", e)
            false
        }
    }

    /**
     * Stop playing the hold track.
     */
    fun stop() {
        Log.i(TAG, "stop() called")
        try {
            nativeStop()
        } catch (e: Exception) {
            Log.e(TAG, "Error in nativeStop", e)
        }
    }

    /**
     * Stops the local hold music playback and unmutes the user's microphone.
     */
    fun stopHoldAndUnmute(inCallService: InCallService) {
        Log.i(TAG, "stopHoldAndUnmute() called. Stopping stream and unmuting mic.")
        stop()
        try {
            inCallService.setMuted(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unmute mic on InCallService", e)
        }
    }

    private external fun nativeStart(filePath: String): Boolean
    private external fun nativeStop()
}
