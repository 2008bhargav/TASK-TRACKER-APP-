package com.example.ui.weather

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IndianCities
import com.example.data.LocalCity
import com.example.ui.MainViewModel
import com.example.ui.WeatherUiState
import com.example.ui.tilt3D
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    
    val context = LocalContext.current
    
    // Manage location permissions using Accompanist
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.requestGPSLocationAndWeather(context)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Atmosphere",
                        color = Color(0xFF001D35),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Realtime environment details",
                        color = Color(0xFF44474E),
                        fontSize = 14.sp
                    )
                }

                // GPS Request action icon
                IconButton(
                    onClick = {
                        if (permissionsState.allPermissionsGranted) {
                            viewModel.requestGPSLocationAndWeather(context)
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF0061A4)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Access Current GPS Weather",
                        tint = Color.White
                    )
                }
            }

            // Quick Indian City Filter pills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Location Stations", color = Color(0xFF001D35), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(IndianCities) { city ->
                        val active = selectedCity.name == city.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) Color(0xFFD3E4FF) else Color(0xFFFFFFFF))
                                .clickable { viewModel.getWeatherForCity(city) }
                                .border(
                                    if (active) androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
                                    else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = city.name,
                                color = if (active) Color(0xFF001D35) else Color(0xFF44474E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Core Weather UI States
            when (val state = weatherState) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF0061A4))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Retrieving wind coordinates...", color = Color(0xFF44474E))
                        }
                    }
                }
                is WeatherUiState.Success -> {
                    WeatherDashboardCard(state = state)
                }
                is WeatherUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFFFF1F1))
                            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f)), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(54.dp))
                            Text(
                                text = state.message,
                                color = Color(0xFF1F1B1B),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.fetchDefaultWeather() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Load Secondary Offline City", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDashboardCard(state: WeatherUiState.Success) {
    val temp = state.temperature
    val code = state.weatherCode
    
    // Style configurations based on weather code mapping to Sleek pastel look
    val (weatherTitle, weatherIcon, cardBrush, brandTint) = remember(code) {
        when (code) {
            0 -> Quadruple(
                "Bright Sunny Sky",
                Icons.Filled.WbSunny,
                Brush.verticalGradient(listOf(Color(0xFFFFECB3), Color(0xFFFFD54F))),
                Color(0xFFE28900)
            )
            1, 2, 3 -> Quadruple(
                "Partly Cloudy Air",
                Icons.Filled.Cloud,
                Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC))),
                Color(0xFF455A64)
            )
            45, 48 -> Quadruple(
                "Dense Foggy Vistas",
                Icons.Filled.BlurOn,
                Brush.verticalGradient(listOf(Color(0xFFF5F5F5), Color(0xFFE0E0E0))),
                Color(0xFF616161)
            )
            51, 53, 55, 61, 63, 65 -> Quadruple(
                "Drizzling Showers",
                Icons.Filled.WaterDrop,
                Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFF90CAF9))),
                Color(0xFF0D47A1)
            )
            71, 73, 75 -> Quadruple(
                "Snowy Mountains Vibe",
                Icons.Filled.AcUnit,
                Brush.verticalGradient(listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA))),
                Color(0xFF006064)
            )
            else -> Quadruple(
                "Tropical Rainstorms",
                Icons.Filled.Thunderstorm,
                Brush.verticalGradient(listOf(Color(0xFFEDE7F6), Color(0xFFB39DDB))),
                Color(0xFF4A148C)
            )
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .tilt3D(maxRotationX = 14f, maxRotationY = 14f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBrush)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top City / Sub
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isGPS) Icons.Filled.MyLocation else Icons.Filled.LocationCity,
                            contentDescription = null,
                            tint = Color(0xFF001D35),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = state.cityName,
                            color = Color(0xFF001D35),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "REALTIME DATA ACCREDITED",
                        color = Color(0xFF44474E).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Core Temp details
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = null,
                        tint = brandTint,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "$temp°C",
                        color = Color(0xFF001D35),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = weatherTitle,
                        color = Color(0xFF001D35),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Accessory details board
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.6f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Air, contentDescription = null, tint = Color(0xFF0061A4))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Wind velocity", color = Color(0xFF44474E), fontSize = 11.sp)
                        Text("${state.windspeed} km/h", color = Color(0xFF001D35), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFFCAC4D0).copy(alpha = 0.5f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DeviceThermostat, contentDescription = null, tint = Color(0xFF0A6B3A))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("System Mode", color = Color(0xFF44474E), fontSize = 11.sp)
                        Text(if (temp > 28) "Warm" else "Cozy", color = Color(0xFF001D35), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
