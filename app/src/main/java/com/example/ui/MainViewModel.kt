package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.clock.SoundSynthesizer
import com.example.ui.notes.VoiceRecorder
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(
        val temperature: Double,
        val windspeed: Double,
        val weatherCode: Int,
        val cityName: String,
        val isGPS: Boolean
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

class MainViewModel(
    private val repository: AppRepository,
    context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private val voiceRecorder = VoiceRecorder(appContext)
    private var mediaPlayer: MediaPlayer? = null

    // --- NOTES STATE ---
    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordedPath = MutableStateFlow<String?>(null)
    val recordedPath = _recordedPath.asStateFlow()

    private val _playingNoteId = MutableStateFlow<Int?>(null)
    val playingNoteId = _playingNoteId.asStateFlow()

    // --- CLOCK STATE: ALARMS ---
    val allAlarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _triggeredAlarm = MutableStateFlow<Alarm?>(null)
    val triggeredAlarm = _triggeredAlarm.asStateFlow()

    private var alarmCheckJob: Job? = null
    private var lastTriggeredMinCode = ""

    // --- CLOCK STATE: STOPWATCH ---
    private val _stopwatchTime = MutableStateFlow(0L) // in milliseconds
    val stopwatchTime = _stopwatchTime.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning = _isStopwatchRunning.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<String>>(emptyList())
    val stopwatchLaps = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null

    // --- CLOCK STATE: TIMER ---
    private val _timerDuration = MutableStateFlow(0L) // Total duration in seconds
    val timerDuration = _timerDuration.asStateFlow()

    private val _timerRemaining = MutableStateFlow(0L) // Remaining seconds
    val timerRemaining = _timerRemaining.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    // --- WEATHER STATE ---
    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState = _weatherState.asStateFlow()

    private val _selectedCity = MutableStateFlow<LocalCity>(IndianCities[1]) // Default Delhi
    val selectedCity = _selectedCity.asStateFlow()

    // --- GAME SCORES STATE ---
    private val _highScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val highScores = _highScores.asStateFlow()

    init {
        startAlarmScheduler()
        fetchDefaultWeather()
        fetchHighScores()
    }

    // --- NOTES ACTIONS ---
    fun saveNote(title: String, content: String, type: String, imageUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalPath = if (type == "voice") _recordedPath.value else null
            val note = Note(
                title = title.ifBlank { "Untitled Note" },
                content = content,
                type = type,
                voicePath = finalPath,
                imageUri = imageUri
            )
            repository.insertNote(note)
            _recordedPath.value = null
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
            // Delete voice file if exists
            note.voicePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    fun startVoiceRecording() {
        val path = voiceRecorder.startRecording()
        if (path != null) {
            _recordedPath.value = path
            _isRecording.value = true
        }
    }

    fun stopVoiceRecording() {
        voiceRecorder.stopAudio()
        _isRecording.value = false
    }

    fun playVoiceNote(note: Note) {
        val path = note.voicePath ?: return
        if (_playingNoteId.value == note.id) {
            stopVoiceNotePlayback()
            return
        }
        
        stopVoiceNotePlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                _playingNoteId.value = note.id
                setOnCompletionListener {
                    _playingNoteId.value = null
                    it.release()
                    mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playingNoteId.value = null
        }
    }

    fun stopVoiceNotePlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _playingNoteId.value = null
    }

    // --- ALARM ACTIONS ---
    fun saveAlarm(hour: Int, minute: Int, soundOption: Int, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAlarm(Alarm(hour = hour, minute = minute, soundOption = soundOption, label = label))
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlarm(alarm)
        }
    }

    fun testAlarmSound(soundOption: Int) {
        SoundSynthesizer.playSound(soundOption, viewModelScope)
    }

    fun stopTestAlarmSound() {
        SoundSynthesizer.stopSound()
    }

    fun dismissAlarm() {
        SoundSynthesizer.stopSound()
        _triggeredAlarm.value = null
    }

    private fun startAlarmScheduler() {
        alarmCheckJob?.cancel()
        alarmCheckJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val minCode = "$currentHour:$currentMinute"

                if (minCode != lastTriggeredMinCode) {
                    val alarms = allAlarms.value
                    val matchingAlarm = alarms.find {
                        it.isEnabled && it.hour == currentHour && it.minute == currentMinute
                    }
                    if (matchingAlarm != null) {
                        lastTriggeredMinCode = minCode
                        _triggeredAlarm.value = matchingAlarm
                        SoundSynthesizer.playSound(matchingAlarm.soundOption, viewModelScope)
                    }
                }
                delay(10000) // check every 10 seconds
            }
        }
    }

    // --- STOPWATCH ACTIONS ---
    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        val startTime = System.currentTimeMillis() - _stopwatchTime.value
        stopwatchJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isStopwatchRunning.value) {
                _stopwatchTime.value = System.currentTimeMillis() - startTime
                delay(30)
            }
        }
    }

    fun pauseStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
    }

    fun resetStopwatch() {
        pauseStopwatch()
        _stopwatchTime.value = 0
        _stopwatchLaps.value = emptyList()
    }

    fun lapStopwatch() {
        val count = _stopwatchLaps.value.size + 1
        val formatted = formatStopwatchTime(_stopwatchTime.value)
        _stopwatchLaps.value = listOf("Lap $count: $formatted") + _stopwatchLaps.value
    }

    fun formatStopwatchTime(ms: Long): String {
        val minutes = (ms / 60000) % 60
        val seconds = (ms / 1000) % 60
        val hundredths = (ms / 10) % 100
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    // --- TIMER ACTIONS ---
    fun startTimer(seconds: Long) {
        timerJob?.cancel()
        _timerDuration.value = seconds
        _timerRemaining.value = seconds
        _isTimerRunning.value = true

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_timerRemaining.value > 0 && _isTimerRunning.value) {
                delay(1000)
                if (_isTimerRunning.value) {
                    _timerRemaining.value -= 1
                }
            }
            if (_timerRemaining.value == 0L && _isTimerRunning.value) {
                _isTimerRunning.value = false
                // Trigger digital sound to notify timer is complete
                SoundSynthesizer.playSound(1, viewModelScope)
                delay(5000)
                SoundSynthesizer.stopSound()
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
    }

    fun resumeTimer() {
        if (_timerRemaining.value > 0) {
            _isTimerRunning.value = true
            startTimer(_timerRemaining.value)
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerRemaining.value = 0
        _timerDuration.value = 0
    }

    // --- WEATHER ACTIONS ---
    fun fetchDefaultWeather() {
        getWeatherForCity(IndianCities[1]) // New Delhi by default
    }

    fun getWeatherForCity(city: LocalCity) {
        _selectedCity.value = city
        _weatherState.value = WeatherUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = WeatherClient.api.getWeather(city.latitude, city.longitude)
                _weatherState.value = WeatherUiState.Success(
                    temperature = response.currentWeather.temperature,
                    windspeed = response.currentWeather.windspeed,
                    weatherCode = response.currentWeather.weathercode,
                    cityName = city.name,
                    isGPS = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _weatherState.value = WeatherUiState.Error("Network Error: ${e.localizedMessage ?: "Check connection"}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestGPSLocationAndWeather(context: Context) {
        _weatherState.value = WeatherUiState.Loading
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val response = WeatherClient.api.getWeather(location.latitude, location.longitude)
                                _weatherState.value = WeatherUiState.Success(
                                    temperature = response.currentWeather.temperature,
                                    windspeed = response.currentWeather.windspeed,
                                    weatherCode = response.currentWeather.weathercode,
                                    cityName = "Current GPS Location",
                                    isGPS = true
                                )
                            } catch (e: Exception) {
                                _weatherState.value = WeatherUiState.Error("Weather API error, showing default city.")
                                delay(2000)
                                fetchDefaultWeather()
                            }
                        }
                    } else {
                        // Location was null, load default Delhi
                        _weatherState.value = WeatherUiState.Error("GPS Location unavailable. Try setting a city manually.")
                        viewModelScope.launch {
                            delay(2000)
                            fetchDefaultWeather()
                        }
                    }
                }
                .addOnFailureListener {
                    _weatherState.value = WeatherUiState.Error("Failed to access GPS: ${it.localizedMessage}")
                    viewModelScope.launch {
                        delay(2000)
                        fetchDefaultWeather()
                    }
                }
        } catch (e: Exception) {
            _weatherState.value = WeatherUiState.Error("Location Permission is required.")
            viewModelScope.launch {
                delay(2000)
                fetchDefaultWeather()
            }
        }
    }

    // --- GAMES HIGH SCORES ---
    fun fetchHighScores() {
        viewModelScope.launch(Dispatchers.IO) {
            val scores = mutableMapOf<String, Int>()
            val games = listOf("racing", "fruit", "archery", "cricket")
            games.forEach { id ->
                scores[id] = repository.getHighScore(id)
            }
            _highScores.value = scores
        }
    }

    fun updateHighScore(gameId: String, score: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateHighScore(gameId, score)
            fetchHighScores() // reload
        }
    }

    override fun onCleared() {
        super.onCleared()
        SoundSynthesizer.stopSound()
        stopVoiceRecording()
        stopVoiceNotePlayback()
        alarmCheckJob?.cancel()
        stopwatchJob?.cancel()
        timerJob?.cancel()
    }
}

class MainViewModelFactory(
    private val repository: AppRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
