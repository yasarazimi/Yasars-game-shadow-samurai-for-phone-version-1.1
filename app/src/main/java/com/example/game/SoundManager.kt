package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

object SoundManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isMusicPlaying = false
    private var musicJob = scope.launch { } // Empty job to initialize

    // Externally adjustable volumes (range 0.0f to 1.0f)
    var soundVolume: Float = 0.8f
    var musicVolume: Float = 0.5f
    var keyMovementVolume: Float = 0.5f

    // Generates a sweep tone with customized frequency drop or rise
    private fun generateSweepWave(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float = 0.5f,
        waveType: String = "sine"
    ): ShortArray {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * t
            val angle = 2.0 * Math.PI * freq * (i.toDouble() / sampleRate)

            val amplitude = when (waveType) {
                "triangle" -> {
                    val x = (angle / (2.0 * Math.PI)) % 1.0
                    if (x < 0.5) 4.0 * x - 1.0 else 3.0 - 4.0 * x
                }
                "square" -> {
                    if (sin(angle) >= 0) 1.0 else -1.0
                }
                else -> sin(angle)
            }

            // Quick fade out envelope to avoid click noise
            val envelope = if (i > numSamples * 0.85) {
                ((numSamples - i).toFloat() / (numSamples * 0.15f))
            } else 1.0f

            val sample = (amplitude * Short.MAX_VALUE * volume * envelope).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    fun playJump() {
        scope.launch {
            val volume = 0.3f * keyMovementVolume * soundVolume
            val buffer = generateSweepWave(280.0, 580.0, 140, volume, "sine")
            playBuffer(buffer)
        }
    }

    fun playSlash() {
        scope.launch {
            val volume = 0.4f * soundVolume
            val buffer = generateSweepWave(1300.0, 320.0, 150, volume, "triangle")
            playBuffer(buffer)
        }
    }

    fun playHit() {
        scope.launch {
            // Heavy crunch/slashing impact: square sweep + deep sine bass thump
            val baseThump = generateSweepWave(160.0, 40.0, 220, 0.6f * soundVolume, "sine")
            val clashSweep = generateSweepWave(900.0, 150.0, 130, 0.45f * soundVolume, "square")

            val mixLen = maxOf(baseThump.size, clashSweep.size)
            val mixed = ShortArray(mixLen)
            for (i in 0 until mixLen) {
                var sum = 0f
                if (i < baseThump.size) sum += baseThump[i]
                if (i < clashSweep.size) sum += clashSweep[i] * 0.5f
                mixed[i] = sum.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
            }
            playBuffer(mixed)
        }
    }

    fun playMove() {
        scope.launch {
            val volume = 0.15f * keyMovementVolume * soundVolume
            val buffer = generateSweepWave(85.0, 55.0, 50, volume, "sine")
            playBuffer(buffer)
        }
    }

    fun playIntroClash() {
        scope.launch {
            val volume = 0.4f * soundVolume
            val sweep = generateSweepWave(90.0, 1100.0, 600, volume, "sine")
            playBuffer(sweep)
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        if (buffer.isEmpty()) return
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(22050)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            
            // Release track static reference after audio duration
            scope.launch {
                delay(1000)
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing buffer: ${e.message}")
        }
    }

    fun startAmbientMusic() {
        if (isMusicPlaying) return
        isMusicPlaying = true
        musicJob = scope.launch {
            // Samurai traditional theme layout using authentic synth sounds
            // Pentatonic steps for rustic oriental flavor
            val scale = doubleArrayOf(220.0, 246.94, 293.66, 329.63, 392.00, 440.0, 493.88)
            val kickFreq = 50.0
            var beat = 0
            
            while (isMusicPlaying) {
                val duration = 400
                // Procedural melodies
                val index = when (beat) {
                    0, 8 -> 0
                    2, 10 -> 2
                    4, 12 -> 4
                    6 -> 5
                    14 -> 3
                    else -> (0..5).random()
                }
                
                val melodyFreq = scale[index]
                val melody = generateSweepWave(melodyFreq, melodyFreq * 0.99, duration, 0.15f * musicVolume, "triangle")
                val bass = if (beat % 4 == 0 || beat == 6 || beat == 14) {
                    generateSweepWave(kickFreq, kickFreq * 0.5, 110, 0.35f * musicVolume, "sine")
                } else ShortArray(0)

                val mixed = ShortArray(melody.size)
                for (i in mixed.indices) {
                    var sum = melody[i].toFloat()
                    if (i < bass.size) {
                        sum += bass[i] * 0.7f
                    }
                    mixed[i] = sum.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
                }

                playBuffer(mixed)
                delay(duration.toLong() + 15L)
                beat = (beat + 1) % 16
            }
        }
    }

    fun stopAmbientMusic() {
        isMusicPlaying = false
        try {
            musicJob.cancel()
        } catch (e: Exception) {}
    }
}
