package com.example.ui.clock

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var playJob: Job? = null

    fun playSound(option: Int, scope: CoroutineScope) {
        stopSound()
        
        playJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 8000
            val numSamples = sampleRate * 2 // 2 seconds loops
            val sample = ShortArray(numSamples)
            
            when (option) {
                0 -> { // Cyber Buzz (Low freq modulated square wave)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val pulse = (t * 4).toInt() % 2 // pulse every quarter second
                        if (pulse == 0) {
                            val f = 180.0
                            val s = sin(2.0 * Math.PI * f * t)
                            sample[i] = if (s > 0) 9000 else -9000
                        } else {
                            sample[i] = 0
                        }
                    }
                }
                1 -> { // Digital Beat (Pulsing piezo frequency beep)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val pulse = (t * 8).toInt() % 2
                        if (pulse == 0) {
                            val f = 1000.0
                            sample[i] = (sin(2.0 * Math.PI * f * t) * 12000).toInt().toShort()
                        } else {
                            sample[i] = 0
                        }
                    }
                }
                2 -> { // Space Chime (Modulated wave)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val sweepFreq = 440.0 + sin(2.0 * Math.PI * 3.0 * t) * 150.0
                        sample[i] = (sin(2.0 * Math.PI * sweepFreq * t) * 10000).toInt().toShort()
                    }
                }
            }
            
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            try {
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBufSize, numSamples * 2),
                    AudioTrack.MODE_STREAM
                )
                
                audioTrack?.play()
                
                while (playJob?.isActive == true) {
                    audioTrack?.write(sample, 0, sample.size)
                    delay(10) // slight buffer pacing
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopSound() {
        playJob?.cancel()
        playJob = null
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }
}
