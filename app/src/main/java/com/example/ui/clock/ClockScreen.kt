package com.example.ui.clock

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Alarm
import com.example.ui.MainViewModel
import com.example.ui.tilt3D
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ClockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Alarms, 1: Stopwatch, 2: Timer, 3: World Clock
    val tabs = listOf("Alarms", "Stopwatch", "Timer", "World Clock")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White.copy(alpha = 0.7f),
            contentColor = Color(0xFF001D35),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF0061A4)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    selectedContentColor = Color(0xFF0061A4),
                    unselectedContentColor = Color(0xFF44474E).copy(alpha = 0.7f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> AlarmsTab(viewModel)
                1 -> StopwatchTab(viewModel)
                2 -> TimerTab(viewModel)
                3 -> WorldClockTab()
            }
        }
    }
}

@Composable
fun AlarmsTab(viewModel: MainViewModel) {
    val alarms by viewModel.allAlarms.collectAsState()
    var showAddAlarmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Saved Alarms", color = Color(0xFF001D35), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                
                Button(
                    onClick = { showAddAlarmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Filled.AddAlarm, contentDescription = "Add Alarm", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Alarm", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No alarms configured", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onDelete = { viewModel.deleteAlarm(alarm) }
                        )
                    }
                }
            }
        }

        if (showAddAlarmDialog) {
            var hour by remember { mutableIntStateOf(7) }
            var minute by remember { mutableIntStateOf(0) }
            var soundOption by remember { mutableIntStateOf(0) } // 0, 1, 2
            var label by remember { mutableStateOf("Morning Alarm") }
            var isTestingSound by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = {
                showAddAlarmDialog = false
                viewModel.stopTestAlarmSound()
            }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Add New Alarm", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Hour / Minute adjustments
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HOUR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { hour = if (hour == 0) 23 else hour - 1 }) {
                                        Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "-", tint = Color(0xFF38BDF8))
                                    }
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d", hour),
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { hour = if (hour == 23) 0 else hour + 1 }) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "+", tint = Color(0xFF38BDF8))
                                    }
                                }
                            }

                            Text(":", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MINUTE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { minute = if (minute == 0) 59 else minute - 1 }) {
                                        Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "-", tint = Color(0xFF38BDF8))
                                    }
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d", minute),
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { minute = if (minute == 59) 0 else minute + 1 }) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "+", tint = Color(0xFF38BDF8))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Alarm Label") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Sound Choice Sector
                        Text("Select Alert Sound", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val alertSounds = listOf("Cyber Buzz", "Digital", "Space Chime")
                            alertSounds.forEachIndexed { index, name ->
                                val selected = soundOption == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFF6366F1) else Color(0xFF131520))
                                        .clickable {
                                            soundOption = index
                                            if (isTestingSound) {
                                                viewModel.testAlarmSound(soundOption)
                                            }
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name, color = if (selected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Test sound utility
                        Button(
                            onClick = {
                                if (isTestingSound) {
                                    viewModel.stopTestAlarmSound()
                                    isTestingSound = false
                                } else {
                                    viewModel.testAlarmSound(soundOption)
                                    isTestingSound = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isTestingSound) Color.Red else Color.DarkGray)
                        ) {
                            Icon(if (isTestingSound) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isTestingSound) "Stop Sound Trial" else "Preview Sound Tune")
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // Action rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showAddAlarmDialog = false
                                    viewModel.stopTestAlarmSound()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss", color = Color.LightGray)
                            }
                            Button(
                                onClick = {
                                    viewModel.saveAlarm(hour, minute, soundOption, label)
                                    showAddAlarmDialog = false
                                    viewModel.stopTestAlarmSound()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Alarm")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmRow(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val soundName = when (alarm.soundOption) {
        0 -> "Cyber Buzz"
        1 -> "Digital Beat"
        else -> "Space Chime"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, if (alarm.isEnabled) Color(0xFF0061A4).copy(alpha = 0.25f) else Color(0xFFCAC4D0).copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .tilt3D(maxRotationX = 10f, maxRotationY = 10f)
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = if (alarm.isEnabled) listOf(Color(0xFFE8F0FF), Color(0xFFFFFFFF))
                    else listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    color = Color(0xFF001D35),
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(alarm.label, color = Color(0xFF1A1C1E), fontSize = 13.sp)
                    Text("•", color = Color(0xFFCAC4D0), fontSize = 13.sp)
                    Text(soundName, color = Color(0xFF0061A4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0061A4)
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete alarm", tint = Color(0xFFBA1A1A))
                }
            }
        }
    }
}

@Composable
fun StopwatchTab(viewModel: MainViewModel) {
    val stopwatchTime by viewModel.stopwatchTime.collectAsState()
    val isRunning by viewModel.isStopwatchRunning.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Space holder
        Spacer(modifier = Modifier.height(20.dp))

        // Large time display 3D Digital LCD Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .tilt3D(maxRotationX = 8f, maxRotationY = 8f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),
                            Color(0xFFBBDEFB)
                        )
                    )
                )
                .border(androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1976D2).copy(alpha = 0.25f)), RoundedCornerShape(24.dp))
                .padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = viewModel.formatStopwatchTime(stopwatchTime),
                color = Color(0xFF0D47A1),
                fontSize = 44.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Actions rows
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            if (isRunning) {
                Button(
                    onClick = { viewModel.pauseStopwatch() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE28900)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause", color = Color.White)
                }
                Button(
                    onClick = { viewModel.lapStopwatch() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A6B3A)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.OutlinedFlag, contentDescription = "Lap Split", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lap Split", color = Color.White)
                }
            } else {
                Button(
                    onClick = { viewModel.startStopwatch() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start", color = Color.White)
                }
                Button(
                    onClick = { viewModel.resetStopwatch() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    enabled = stopwatchTime > 0
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset", color = Color.White)
                }
            }
        }

        // Laps lists
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .tilt3D(maxRotationX = 6f, maxRotationY = 6f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFFFFFF))
                .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            if (laps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No laps recorded yet", color = Color(0xFF44474E), fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(laps) { lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(lap.substringBefore(":"), color = Color(0xFF1A1C1E), fontSize = 15.sp)
                            Text(lap.substringAfter(":"), color = Color(0xFF0061A4), fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun TimerTab(viewModel: MainViewModel) {
    val totalSeconds by viewModel.timerDuration.collectAsState()
    val remainingSeconds by viewModel.timerRemaining.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()

    var inputHours by remember { mutableFloatStateOf(0f) }
    var inputMinutes by remember { mutableFloatStateOf(5f) }
    var inputSeconds by remember { mutableFloatStateOf(0f) }

    val formattedSecondsStr = remember(remainingSeconds) {
        val h = remainingSeconds / 3600
        val m = (remainingSeconds / 60) % 60
        val s = remainingSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (totalSeconds == 0L) {
            // Configuration mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Setup CountDown Timer", color = Color(0xFF001D35), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                // Hour
                Text("Hours: ${inputHours.toInt()} hr", color = Color(0xFF1A1C1E), fontSize = 14.sp)
                Slider(
                    value = inputHours,
                    onValueChange = { inputHours = it },
                    valueRange = 0f..23f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF0061A4), activeTrackColor = Color(0xFF0061A4))
                )

                // Minutes
                Text("Minutes: ${inputMinutes.toInt()} min", color = Color(0xFF1A1C1E), fontSize = 14.sp)
                Slider(
                    value = inputMinutes,
                    onValueChange = { inputMinutes = it },
                    valueRange = 0f..59f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF0288D1), activeTrackColor = Color(0xFF0288D1))
                )

                // Seconds
                Text("Seconds: ${inputSeconds.toInt()} sec", color = Color(0xFF1A1C1E), fontSize = 14.sp)
                Slider(
                    value = inputSeconds,
                    onValueChange = { inputSeconds = it },
                    valueRange = 0f..59f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00897B), activeTrackColor = Color(0xFF00897B))
                )
                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = {
                        val secs = (inputHours.toInt() * 3600) + (inputMinutes.toInt() * 60) + inputSeconds.toInt()
                        if (secs > 0) {
                            viewModel.startTimer(secs.toLong())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.width(220.dp)
                ) {
                    Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Timer", color = Color.White)
                }
            }
        } else {
            // Running / Countdown mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .tilt3D(maxRotationX = 10f, maxRotationY = 10f),
                contentAlignment = Alignment.Center
            ) {
                // Circular progress arc
                val fraction = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
                Canvas(modifier = Modifier.size(240.dp)) {
                    // background circle
                    drawCircle(
                        color = Color(0xFFD3E4FF),
                        radius = size.width / 2,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    // active progress circle
                    drawArc(
                        color = Color(0xFF0061A4),
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedSecondsStr,
                        color = Color(0xFF001D35),
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
						text = "REMAINING",
                        color = Color(0xFF44474E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRunning) {
                    Button(
                        onClick = { viewModel.pauseTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE28900)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.resumeTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A6B3A)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume", color = Color.White)
                    }
                }

                Button(
                    onClick = { viewModel.stopTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop & Reset", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun WorldClockTab() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val displayCities = listOf(
        WorldCity("Local Time", "Asia/Kolkata", "Your current location"),
        WorldCity("London (UTC)", "Europe/London", "United Kingdom"),
        WorldCity("New York (EST/EDT)", "America/New_York", "United States"),
        WorldCity("Tokyo (JST)", "Asia/Tokyo", "Japan"),
        WorldCity("Sydney (AEST)", "Australia/Sydney", "Australia")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Global Time Stations", color = Color(0xFF001D35), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }

        items(displayCities) { city ->
            val sdf = SimpleDateFormat("hh:mm:ss a • EEE dd MMM", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone(city.zoneId)
            }
            val formattedTime = sdf.format(Date(currentTime))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(city.name, color = Color(0xFF001D35), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(city.region, color = Color(0xFF44474E), fontSize = 12.sp)
                        }
                        
                        Text(
                            text = formattedTime,
                            color = Color(0xFF0061A4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
            }
        }
    }
}

data class WorldCity(val name: String, val zoneId: String, val region: String)
