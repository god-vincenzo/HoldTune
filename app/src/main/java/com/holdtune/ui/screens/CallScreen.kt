package com.holdtune.ui.screens

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdtune.audio.AudioBridge
import com.holdtune.telecom.HoldTuneInCallService
import com.holdtune.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    call: Call,
    isHoldState: Boolean,
    onJoinCallClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
    callerName: String
) {
    val number = call.details.handle?.schemeSpecificPart ?: "Unknown Caller"
    var durationSeconds by remember { mutableStateOf(0) }
    
    // Track audio settings
    var isMuted by remember { mutableStateOf(true) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    // Call duration timer
    LaunchedEffect(isHoldState) {
        if (!isHoldState) {
            durationSeconds = 0
            while (true) {
                delay(1000L)
                durationSeconds++
            }
        }
    }

    // Sync initial states from InCallService audio state if available
    val inCallService = HoldTuneInCallService.getInstance()
    LaunchedEffect(Unit) {
        inCallService?.let { service ->
            service.callAudioState?.let { audioState ->
                isMuted = audioState.isMuted
                isSpeakerOn = (audioState.route == CallAudioState.ROUTE_SPEAKER)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isHoldState) {
                        listOf(DarkGray, Color(0xFF1E351E)) // Dark green tint for hold mode
                    } else {
                        listOf(DarkGray, Color(0xFF1A1A2E)) // Dark blue/gray tint for active mode
                    }
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile Initials / Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (callerName.isNotEmpty()) callerName.take(1).uppercase() else "?",
                    color = PrimaryPurple,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Caller Identifier
            Text(
                text = callerName,
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (callerName != number) {
                Text(
                    text = number,
                    color = TextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Call status and Timer
            if (isHoldState) {
                Text(
                    text = "HOLDTUNE INTERCEPTED\nPlaying Private Hold Music...",
                    color = WarningOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            } else {
                val minutes = durationSeconds / 60
                val seconds = durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = AccentGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Action UI based on hold/active state
            Crossfade(targetState = isHoldState, label = "call_screen_actions") { holdActive ->
                if (holdActive) {
                    // Hold Mode layout - answer or decline options
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onJoinCallClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = "Answer & Join Call",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onDisconnectClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text(
                                text = "Decline Call",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Active Call controls
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Grid of mute/speaker buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Mute button
                            IconButtonWithLabel(
                                onClick = {
                                    val targetMute = !isMuted
                                    inCallService?.setMuted(targetMute)
                                    isMuted = targetMute
                                },
                                label = if (isMuted) "Unmute" else "Mute",
                                isToggled = isMuted,
                                activeIcon = android.R.drawable.stat_notify_call_mute,
                                inactiveIcon = android.R.drawable.stat_notify_call_mute
                            )

                            // Speaker button
                            IconButtonWithLabel(
                                onClick = {
                                    val targetRoute = if (isSpeakerOn) {
                                        CallAudioState.ROUTE_EARPIECE
                                    } else {
                                        CallAudioState.ROUTE_SPEAKER
                                    }
                                    inCallService?.setAudioRoute(targetRoute)
                                    isSpeakerOn = !isSpeakerOn
                                },
                                label = "Speaker",
                                isToggled = isSpeakerOn,
                                activeIcon = android.R.drawable.stat_sys_speakerphone,
                                inactiveIcon = android.R.drawable.stat_sys_speakerphone
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // Large red hangup button
                        Button(
                            onClick = onDisconnectClicked,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier
                                .size(72.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "✖",
                                color = TextWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconButtonWithLabel(
    onClick: () -> Unit,
    label: String,
    isToggled: Boolean,
    activeIcon: Int,
    inactiveIcon: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isToggled) PrimaryPurple else Color(0xFF262626))
        ) {
            Icon(
                // Safely load standard system icons to prevent compilation errors
                imageVector = androidx.compose.material.icons.Icons.Default.Call,
                contentDescription = label,
                tint = if (isToggled) DarkGray else TextWhite
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Simple fallback icon import to avoid Compose icons dependency issues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
