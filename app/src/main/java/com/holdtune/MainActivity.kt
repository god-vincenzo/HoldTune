package com.holdtune

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.holdtune.audio.AudioBridge
import com.holdtune.audio.OngoingCallService
import com.holdtune.telecom.HoldTuneInCallService
import com.holdtune.ui.screens.CallScreen
import com.holdtune.ui.screens.ConsentScreen
import com.holdtune.ui.screens.SettingsScreen
import com.holdtune.ui.theme.HoldTuneTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"
    private var mediaRecorder: MediaRecorder? = null
    
    // UI states reflecting SharedPreferences
    private var serviceEnabled by mutableStateOf(true)
    private var globalInterception by mutableStateOf(true)
    private var autoAnswerDelay by mutableStateOf(2)
    private var selectedTrackPath by mutableStateOf("")
    private val whitelistedNumbers = mutableStateListOf<String>()
    private var consentGranted by mutableStateOf(false)
    private var isRecording by mutableStateOf(false)
    private var defaultDialerRoleGranted by mutableStateOf(false)

    // Activity launchers
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkDefaultDialerRole()
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (!granted) {
            Toast.makeText(this, "Some permissions were denied. Certain features may not function.", Toast.LENGTH_LONG).show()
        }
    }

    private val selectTrackLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Copy file to internal storage so we have a persistent path for the C++ decoder
            val localPath = copyUriToInternalStorage(selectedUri, "hold_track.mp3")
            if (localPath != null) {
                selectedTrackPath = localPath
                val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("hold_track_path", localPath).apply()
                Toast.makeText(this, "Hold music selected!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to copy audio track", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { contactUri ->
            resolveContactPhoneNumber(contactUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSettings()
        checkDefaultDialerRole()
        requestAppPermissions()

        setContent {
            HoldTuneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val activeCallState by OngoingCallService.callState.collectAsState()
                    val isCallOnHold by OngoingCallService.isHoldState.collectAsState()

                    when {
                        !consentGranted -> {
                            ConsentScreen(
                                onConsentGranted = {
                                    consentGranted = true
                                    val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
                                    prefs.edit().putBoolean("consent_granted", true).apply()
                                    // Guide to default dialer request once consent is signed
                                    requestDefaultDialerRole()
                                }
                            )
                        }
                        activeCallState != null -> {
                            val activeCall = activeCallState!!
                            val callerNumber = activeCall.details.handle?.schemeSpecificPart ?: "Unknown Caller"
                            val resolvedName = remember(callerNumber) { getContactName(callerNumber) }
                            
                            CallScreen(
                                call = activeCall,
                                isHoldState = isCallOnHold,
                                onJoinCallClicked = {
                                    val inCallService = HoldTuneInCallService.getInstance()
                                    if (inCallService != null) {
                                        AudioBridge.stopHoldAndUnmute(inCallService)
                                        OngoingCallService.setHoldState(false)
                                    } else {
                                        Toast.makeText(this, "Call handling service not bound", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDisconnectClicked = {
                                    activeCall.disconnect()
                                    AudioBridge.stop()
                                    OngoingCallService.updateCall(null)
                                },
                                callerName = resolvedName
                            )
                        }
                        else -> {
                            SettingsScreen(
                                serviceEnabled = serviceEnabled,
                                onServiceEnabledChanged = { enabled ->
                                    serviceEnabled = enabled
                                    saveSetting("service_enabled", enabled)
                                },
                                globalInterception = globalInterception,
                                onGlobalInterceptionChanged = { global ->
                                    globalInterception = global
                                    saveSetting("global_enabled", global)
                                },
                                autoAnswerDelay = autoAnswerDelay,
                                onAutoAnswerDelayChanged = { delay ->
                                    autoAnswerDelay = delay
                                    saveSetting("auto_answer_delay", delay)
                                },
                                selectedTrackPath = selectedTrackPath,
                                onSelectTrackClicked = { selectTrackLauncher.launch("audio/*") },
                                onPickContactClicked = { pickContactLauncher.launch(null) },
                                whitelistedNumbers = whitelistedNumbers,
                                onAddNumber = { number -> addWhitelistedNumber(number) },
                                onRemoveNumber = { number -> removeWhitelistedNumber(number) },
                                isRecording = isRecording,
                                onStartRecording = { startRecordingAudio() },
                                onStopRecording = { stopRecordingAudio() },
                                onOemBatterySettingsClicked = { openBatteryOptimizationSettings() },
                                isOemQuirks = isOemDevice(),
                                defaultDialerRoleGranted = defaultDialerRoleGranted,
                                onRequestDefaultDialerRole = { requestDefaultDialerRole() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkDefaultDialerRole()
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.ANSWER_PHONE_CALLS,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(android.Manifest.permission.FOREGROUND_SERVICE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(android.Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    private fun checkDefaultDialerRole() {
        defaultDialerRoleGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.defaultDialerPackage == packageName
        }
    }

    private fun requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                requestRoleLauncher.launch(intent)
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
        serviceEnabled = prefs.getBoolean("service_enabled", true)
        globalInterception = prefs.getBoolean("global_enabled", true)
        autoAnswerDelay = prefs.getInt("auto_answer_delay", 2)
        selectedTrackPath = prefs.getString("hold_track_path", "") ?: ""
        consentGranted = prefs.getBoolean("consent_granted", false)

        val whitelistString = prefs.getString("whitelist_contacts", "") ?: ""
        whitelistedNumbers.clear()
        if (whitelistString.isNotEmpty()) {
            whitelistedNumbers.addAll(whitelistString.split(",").map { it.trim() })
        }
    }

    private fun saveSetting(key: String, value: Any) {
        val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is String -> editor.putString(key, value)
        }
        editor.apply()
    }

    private fun addWhitelistedNumber(number: String) {
        if (number.isNotBlank() && !whitelistedNumbers.contains(number)) {
            whitelistedNumbers.add(number)
            saveWhitelist()
        }
    }

    private fun removeWhitelistedNumber(number: String) {
        if (whitelistedNumbers.remove(number)) {
            saveWhitelist()
        }
    }

    private fun saveWhitelist() {
        val prefs = getSharedPreferences("holdtune_prefs", Context.MODE_PRIVATE)
        val whitelistString = whitelistedNumbers.joinToString(",")
        prefs.edit().putString("whitelist_contacts", whitelistString).apply()
    }

    private fun startRecordingAudio() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 102)
            return
        }

        val outputFile = File(filesDir, "recorded_hold_track.3gp")
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                Toast.makeText(this@MainActivity, "Recording started...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(tag, "Failed to start MediaRecorder", e)
                Toast.makeText(this@MainActivity, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecordingAudio() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop MediaRecorder", e)
        }
        mediaRecorder = null
        isRecording = false

        val outputFile = File(filesDir, "recorded_hold_track.3gp")
        selectedTrackPath = outputFile.absolutePath
        saveSetting("hold_track_path", selectedTrackPath)
    }

    private fun copyUriToInternalStorage(uri: Uri, fileName: String): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputFile = File(filesDir, fileName)
            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(tag, "Failed to copy audio URI to files directory", e)
            null
        }
    }

    private fun resolveContactPhoneNumber(contactUri: Uri) {
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(contactUri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                
                if (hasPhoneIndex >= 0 && idIndex >= 0) {
                    val hasPhone = cursor.getString(hasPhoneIndex)
                    val contactId = cursor.getString(idIndex)
                    
                    if (hasPhone == "1") {
                        val phonesCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        phonesCursor?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val numberIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numberIndex >= 0) {
                                    val phoneNumber = pCursor.getString(numberIndex)
                                    addWhitelistedNumber(phoneNumber)
                                    Toast.makeText(this, "Whitelisted contact: $phoneNumber", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve contact phone number", e)
        } finally {
            cursor?.close()
        }
    }

    private fun getContactName(phoneNumber: String): String {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return phoneNumber
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var name = phoneNumber
        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to query contact name", e)
        }
        return name
    }

    private fun isOemDevice(): Boolean {
        val oem = Build.MANUFACTURER.uppercase()
        return oem.contains("SAMSUNG") || oem.contains("XIAOMI") || oem.contains("OPPO") || oem.contains("VIVO") || oem.contains("HUAWEI")
    }

    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open battery settings", Toast.LENGTH_SHORT).show()
        }
    }
}
