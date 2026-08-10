package com.holdtune.ui.screens

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.holdtune.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    serviceEnabled: Boolean,
    onServiceEnabledChanged: (Boolean) -> Unit,
    globalInterception: Boolean,
    onGlobalInterceptionChanged: (Boolean) -> Unit,
    autoAnswerDelay: Int,
    onAutoAnswerDelayChanged: (Int) -> Unit,
    selectedTrackPath: String,
    onSelectTrackClicked: () -> Unit,
    onPickContactClicked: () -> Unit,
    whitelistedNumbers: List<String>,
    onAddNumber: (String) -> Unit,
    onRemoveNumber: (String) -> Unit,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onOemBatterySettingsClicked: () -> Unit,
    isOemQuirks: Boolean,
    defaultDialerRoleGranted: Boolean,
    onRequestDefaultDialerRole: () -> Unit
) {
    var showAddNumberDialog by remember { mutableStateOf(false) }
    var inputNumber by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HoldTune Settings", color = TextWhite, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGray)
            )
        },
        containerColor = DarkGray
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dialer Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "System Status",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (defaultDialerRoleGranted) AccentGreen else AccentRed,
                                        shape = RoundedCornerShape(5.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (defaultDialerRoleGranted) "Default Phone App (Granted)" else "Not Set as Default Phone App",
                                color = if (defaultDialerRoleGranted) TextWhite else TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        
                        if (!defaultDialerRoleGranted) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestDefaultDialerRole,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Make Default Phone App", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Main Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardGray)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Answer Service",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable or disable incoming call interception globally.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = serviceEnabled,
                            onCheckedChange = onServiceEnabledChanged,
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                        )
                    }
                }
            }

            // Settings Configurations Section
            if (serviceEnabled) {
                // Delay Config
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Auto-Answer Delay",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Time elapsed before answering a ringing call automatically.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Slider(
                                    value = autoAnswerDelay.toFloat(),
                                    onValueChange = { onAutoAnswerDelayChanged(it.toInt()) },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryPurple,
                                        activeTrackColor = PrimaryPurple
                                    )
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "$autoAnswerDelay sec",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                // Audio Track Selection
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Hold Track Configuration",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Private music played on earpiece/speaker during auto-answering.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val trackName = if (selectedTrackPath.isNotEmpty()) {
                                selectedTrackPath.substringAfterLast("/")
                            } else {
                                "Recorded Hold Track (Fallback)"
                            }
                            
                            Text(
                                text = "Active Track: $trackName",
                                color = PrimaryPurple,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSelectTrackClicked,
                                    colors = ButtonDefaults.buttonColors(containerColor = CardGray),
                                    border = ButtonDefaults.outlinedButtonBorder,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pick Local File", color = TextWhite)
                                }

                                Button(
                                    onClick = { if (isRecording) onStopRecording() else onStartRecording() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRecording) AccentRed else CardGray
                                    ),
                                    border = if (isRecording) null else ButtonDefaults.outlinedButtonBorder,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isRecording) "Stop Rec" else "Record Mic", color = TextWhite)
                                }
                            }
                        }
                    }
                }

                // Interception Mode and Whitelist
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Interception Filtering",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGlobalInterceptionChanged(false) }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = !globalInterception,
                                    onClick = { onGlobalInterceptionChanged(false) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Whitelist Only", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("Only answer configured whitelisted numbers.", color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGlobalInterceptionChanged(true) }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = globalInterception,
                                    onClick = { onGlobalInterceptionChanged(true) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Global (All Incoming Calls)", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("Intercept all calls except emergency services.", color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            // Global Mode Warning
                            AnimatedVisibility(
                                visible = globalInterception,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x33FF9800)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚠️ WARNING: Global mode answers ALL incoming calls. Ensure this compliance aligns with your local laws. Whitelist mode is recommended.",
                                            color = WarningOrange,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Whitelisted Contacts Management (Only visible when Whitelist is selected)
                if (!globalInterception) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardGray)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Whitelisted Contacts",
                                        color = TextWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { showAddNumberDialog = true }) {
                                        Text("+ Add", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                
                                Button(
                                    onClick = onPickContactClicked,
                                    colors = ButtonDefaults.buttonColors(containerColor = CardGray),
                                    border = ButtonDefaults.outlinedButtonBorder,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text("Pick from System Contacts", color = TextWhite)
                                }

                                if (whitelistedNumbers.isEmpty()) {
                                    Text(
                                        text = "No contacts whitelisted. Add numbers to begin intercepting calls.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        whitelistedNumbers.forEach { number ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Color(0xFF262626),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(number, color = TextWhite, fontSize = 14.sp)
                                                Text(
                                                    text = "Delete",
                                                    color = AccentRed,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable { onRemoveNumber(number) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // OEM Optimization Whitelist Helper
            if (isOemQuirks) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "OEM Background Optimization Warning",
                                color = WarningOrange,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your device manufacturer (${Build.MANUFACTURER}) aggressively kills background apps. For HoldTune to answer incoming calls reliably, please whitelist this app from battery optimization.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOemBatterySettingsClicked,
                                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Disable Battery Optimization", color = DarkGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Number Dialog
    if (showAddNumberDialog) {
        AlertDialog(
            onDismissRequest = { showAddNumberDialog = false },
            title = { Text("Add Phone Number") },
            text = {
                Column {
                    Text("Enter custom phone number to add to whitelist:", color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = inputNumber,
                        onValueChange = { inputNumber = it },
                        singleLine = true,
                        placeholder = { Text("+15550199") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputNumber.isNotBlank()) {
                            onAddNumber(inputNumber.trim())
                            inputNumber = ""
                            showAddNumberDialog = false
                        }
                    }
                ) {
                    Text("Add", color = PrimaryPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNumberDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
