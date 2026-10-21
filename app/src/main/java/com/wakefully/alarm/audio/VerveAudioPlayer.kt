package com.wakefully.alarm.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * High-fidelity acoustic tone generator & soundtrack player for Verve.
 * Supports synthesizing bespoke harmonic chimes and playing user-assigned custom audio files.
 */
object VerveAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var previewJob: Job? = null
    private var volumeEnforcerJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview.asStateFlow()

    private val _currentPlayingTone = MutableStateFlow<String?>(null)
    val currentPlayingTone: StateFlow<String?> = _currentPlayingTone.asStateFlow()

    /**
     * Preview an acoustic tone or custom audio URI for [durationSeconds].
     */
    fun previewTone(
        context: Context,
        toneName: String,
        toneUri: String? = null,
        durationSeconds: Int = 5,
        onFinished: () -> Unit = {}
    ) {
        stopAll()

        _currentPlayingTone.value = toneName
        _isPlayingPreview.value = true

        previewJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                if (!toneUri.isNullOrBlank()) {
                    // Custom user soundtrack via URI
                    playUriSoundtrack(context, Uri.parse(toneUri), loop = false)
                } else {
                    // Built-in acoustic tone synthesis
                    playBuiltInTone(toneName, durationSeconds)
                }
            } catch (e: Exception) {
                // Fallback to system default ringtone if synthesis/media fails
                try {
                    playSystemFallback(context)
                } catch (_: Exception) {}
            } finally {
                withContext(Dispatchers.Main) {
                    _isPlayingPreview.value = false
                    _currentPlayingTone.value = null
                    onFinished()
                }
            }
        }
    }

    /**
     * Continuous alarm playback (used when alarm triggers and phone is ringing).
     * Locks STREAM_ALARM to maximum volume and continuously enforces it against tampering.
     */
    fun startAlarmRinging(context: Context, toneName: String, toneUri: String? = null) {
        stopAll()

        _currentPlayingTone.value = toneName
        _isPlayingPreview.value = true

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 15
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (_: Exception) {}

        // Continuously lock volume at maximum while ringing to disable reducing volume
        volumeEnforcerJob?.cancel()
        volumeEnforcerJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    val currentVol = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: maxVolume
                    if (currentVol < maxVolume) {
                        audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(350)
            }
        }

        previewJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                if (!toneUri.isNullOrBlank()) {
                    val parsed = Uri.parse(toneUri)
                    val playUri = if (parsed.scheme == "content") {
                        saveCustomToneLocally(context, parsed, "cached_alarm_tone") ?: parsed
                    } else {
                        parsed
                    }
                    playUriSoundtrack(context, playUri, loop = true)
                } else {
                    // Continuous synthesized chime loop
                    while (isActive) {
                        playBuiltInTone(toneName, durationSeconds = 4)
                        kotlinx.coroutines.delay(800)
                    }
                }
            } catch (e: Exception) {
                Log.e("VerveAudioPlayer", "Error during alarm ringing, falling back to system sound", e)
                playSystemFallback(context, loop = true)
            }
        }
    }

    fun stopAll() {
        volumeEnforcerJob?.cancel()
        volumeEnforcerJob = null

        previewJob?.cancel()
        previewJob = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        _isPlayingPreview.value = false
        _currentPlayingTone.value = null
    }

    /**
     * Persistently copies a user-selected audio URI into the app's internal private storage.
     * This avoids Android transient permission revocation and SecurityExceptions across
     * process kills, app swipes, and system reboots.
     */
    fun saveCustomToneLocally(context: Context, sourceUri: Uri, originalName: String): Uri? {
        return try {
            val tonesDir = File(context.filesDir, "custom_tones").apply { mkdirs() }
            val cleanExt = originalName.substringAfterLast('.', "mp3")
            val cleanBase = originalName.substringBeforeLast('.')
                .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
                .take(30)
                .ifBlank { "custom_track" }
            val targetFile = File(tonesDir, "tone_${System.currentTimeMillis()}_${cleanBase}.${cleanExt}")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("VerveAudioPlayer", "Saved custom audio track to: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            Log.e("VerveAudioPlayer", "Failed copying audio to internal storage", e)
            null
        }
    }

    private suspend fun playUriSoundtrack(context: Context, uri: Uri, loop: Boolean) {
        withContext(Dispatchers.Main) {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                var initialized = false

                // 1. Check if it's a file:// scheme or direct file path on disk
                if (uri.scheme == "file" || uri.scheme == null) {
                    val filePath = uri.path ?: uri.toString()
                    val file = File(filePath)
                    if (file.exists() && file.canRead()) {
                        try {
                            FileInputStream(file).use { fis ->
                                setDataSource(fis.fd)
                                initialized = true
                            }
                        } catch (e: Exception) {
                            Log.w("VerveAudioPlayer", "FileInputStream.fd failed, trying path", e)
                        }
                    }
                }

                // 2. If it's a content:// URI, try opening file descriptor
                if (!initialized && uri.scheme == "content") {
                    try {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            setDataSource(pfd.fileDescriptor)
                            initialized = true
                        }
                    } catch (e: Exception) {
                        Log.w("VerveAudioPlayer", "ContentResolver openFileDescriptor failed", e)
                    }
                }

                // 3. Fallback to standard setDataSource(context, uri)
                if (!initialized) {
                    setDataSource(context, uri)
                }

                setVolume(1.0f, 1.0f)
                isLooping = loop
                prepare()
                start()
            }
            mediaPlayer = mp
        }
    }

    private fun playSystemFallback(context: Context, loop: Boolean = false) {
        val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (alertUri != null) {
            val mp = MediaPlayer.create(context, alertUri)
            mp?.isLooping = loop
            mp?.start()
            mediaPlayer = mp
        }
    }

    /**
     * Synthesizes resonant harmonic chime chords (PCM 16-bit 44.1kHz).
     */
    private suspend fun playBuiltInTone(toneName: String, durationSeconds: Int) = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val totalSamples = sampleRate * durationSeconds
        val buffer = ShortArray(totalSamples)

        // Harmonic notes (Hz) per tone profile
        val chords: List<DoubleArray> = when (toneName) {
            "Zenith Pulse" -> listOf(
                doubleArrayOf(659.25, 1318.5), // E5 + E6
                doubleArrayOf(880.0, 1760.0)   // A5 + A6
            )
            "Nordic Mist" -> listOf(
                doubleArrayOf(392.0, 587.33),  // G4 + D5
                doubleArrayOf(440.0, 659.25)   // A4 + E5
            )
            "Verve Synthesis" -> listOf(
                doubleArrayOf(523.25, 659.25), // C5 + E5
                doubleArrayOf(659.25, 783.99), // E5 + G5
                doubleArrayOf(783.99, 1046.5)  // G5 + C6
            )
            else -> listOf( // "Radiant Dawn" (Default ambient harmonic chime)
                doubleArrayOf(523.25, 659.25, 783.99), // C Major
                doubleArrayOf(587.33, 739.99, 880.0)   // D Major
            )
        }

        val stepSamples = totalSamples / chords.size
        for (i in buffer.indices) {
            val chordIndex = (i / stepSamples).coerceIn(0, chords.lastIndex)
            val activeChord = chords[chordIndex]
            val localIndex = i % stepSamples
            val progressInStep = localIndex.toDouble() / stepSamples.toDouble()

            // Envelope: Fast attack + long bell decay
            val attack = (progressInStep * 10.0).coerceAtMost(1.0)
            val decay = Math.exp(-progressInStep * 3.5)
            val envelope = attack * decay

            var sampleSum = 0.0
            for (freq in activeChord) {
                val t = 2.0 * Math.PI * freq * i / sampleRate
                sampleSum += sin(t)
            }
            sampleSum = (sampleSum / activeChord.size) * envelope * 0.85
            buffer[i] = (sampleSum * Short.MAX_VALUE).toInt().toShort()
        }

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            buffer.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack = track
        track.write(buffer, 0, buffer.size)
        track.play()

        kotlinx.coroutines.delay(durationSeconds * 1000L)
        try {
            track.stop()
            track.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
