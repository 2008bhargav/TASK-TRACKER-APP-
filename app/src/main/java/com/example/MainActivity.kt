package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.animatedColorsBg
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.clock.ClockScreen
import com.example.ui.clock.SoundSynthesizer
import com.example.ui.games.GameScreen
import com.example.ui.notes.NotesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.weather.WeatherScreen
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup systems
        val database = AppRepository.getDatabase(this)
        val repository = AppRepository(database)
        val factory = MainViewModelFactory(repository, this)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainLayout(viewModel)
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: MainViewModel) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) } // 0: Notes, 1: Clock, 2: Calc, 3: Weather, 4: Games
    
    val triggeredAlarm by viewModel.triggeredAlarm.collectAsState()

    val screens = listOf(
        NavigationItem("Notes", Icons.Filled.Description),
        NavigationItem("Clock", Icons.Filled.AccessTime),
        NavigationItem("3D Calc", Icons.Filled.Calculate),
        NavigationItem("Weather", Icons.Filled.Cloud),
        NavigationItem("Arcade", Icons.Filled.SportsEsports)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FF),
        bottomBar = {
            Column {
                HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f), thickness = 1.dp)
                NavigationBar(
                    containerColor = Color(0xFFFFFFFF),
                    contentColor = Color(0xFF1A1C1E)
                ) {
                    screens.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedScreenIndex == index,
                            onClick = { selectedScreenIndex = index },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF001D35),
                                selectedTextColor = Color(0xFF001D35),
                                indicatorColor = Color(0xFFD3E4FF),
                                unselectedIconColor = Color(0xFF44474E).copy(alpha = 0.7f),
                                unselectedTextColor = Color(0xFF44474E).copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .animatedColorsBg()
        ) {
            AnimatedContent(
                targetState = selectedScreenIndex,
                transitionSpec = {
                    (slideInHorizontally { width -> width / 12 } + fadeIn(animationSpec = tween(250))) togetherWith
                    (slideOutHorizontally { width -> -width / 12 } + fadeOut(animationSpec = tween(250)))
                },
                label = "ScreenSwitchAnimation"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> NotesScreen(viewModel)
                    1 -> ClockScreen(viewModel)
                    2 -> CalculatorScreen()
                    3 -> WeatherScreen(viewModel)
                    4 -> GameScreen(viewModel)
                }
            }

            // Realtime Alarms Overlay dialog popup
            triggeredAlarm?.let { alarm ->
                val alarmTimeStr = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
                val soundName = when (alarm.soundOption) {
                    0 -> "Cyber Buzz"
                    1 -> "Digital Beat"
                    else -> "Space Chime"
                }

                Dialog(
                    onDismissRequest = {}, // Force player to explicitly interact to stop rings
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF261922)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = null,
                                    tint = Color(0xFFF43F5E),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = alarmTimeStr,
                                    color = Color.White,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = alarm.label,
                                    color = Color.LightGray,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ringing Tone: $soundName",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.dismissAlarm() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Icon(Icons.Filled.AlarmOff, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dismiss Alarm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    // Snooze adds 5 mins to Alarm instance
                                    var newMin = alarm.minute + 5
                                    var newHr = alarm.hour
                                    if (newMin >= 60) {
                                        newMin -= 60
                                        newHr = (newHr + 1) % 24
                                    }
                                    viewModel.dismissAlarm()
                                    viewModel.saveAlarm(newHr, newMin, alarm.soundOption, "${alarm.label} (Snoozed)")
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Icon(Icons.Filled.Snooze, contentDescription = null, tint = Color.LightGray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Snooze (+5 Mins)", color = Color.LightGray, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
