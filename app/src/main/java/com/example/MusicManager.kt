package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object MusicManager {
    private const val TAG = "MusicManager"

    private var mediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _currentSongTitle = MutableStateFlow("গান ১ (song1.mp4)")
    val currentSongTitle: StateFlow<String> = _currentSongTitle.asStateFlow()

    private val _statusText = MutableStateFlow("প্লেয়ার প্রস্তুত (Ready)")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    val songNames = listOf("song1.mp4", "song2.mp4", "song3.mp4")
    val songLabels = listOf("গান ১ (song1.mp4)", "গান ২ (song2.mp4)", "গান ৩ (song3.mp4)")

    fun playSong(context: Context, index: Int) {
        appContext = context.applicationContext
        val safeIndex = index.coerceIn(0, songNames.size - 1)
        _currentSongIndex.value = safeIndex
        _currentSongTitle.value = songLabels[safeIndex]

        try {
            stopMediaPlayerOnly()
            initMediaPlayer(context, safeIndex)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing song at index $safeIndex", e)
            _statusText.value = "গান চালু করতে সমস্যা হয়েছে: ${e.message}"
            _isPlaying.value = false
        }
    }

    fun togglePlayPause(context: Context) {
        appContext = context.applicationContext
        val mp = mediaPlayer
        if (mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                _statusText.value = "পজ করা হয়েছে (Paused)"
            } else {
                mp.start()
                _isPlaying.value = true
                _statusText.value = "চলছে (Playing): ${songNames[_currentSongIndex.value]}"
            }
        } else {
            playSong(context, _currentSongIndex.value)
        }
    }

    fun nextSong(context: Context) {
        val nextIdx = (_currentSongIndex.value + 1) % songNames.size
        playSong(context, nextIdx)
    }

    fun previousSong(context: Context) {
        val prevIdx = if (_currentSongIndex.value - 1 < 0) songNames.size - 1 else _currentSongIndex.value - 1
        playSong(context, prevIdx)
    }

    fun stopSong() {
        stopMediaPlayerOnly()
        _isPlaying.value = false
        _statusText.value = "বন্ধ রয়েছে (Stopped)"
    }

    private fun stopMediaPlayerOnly() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun initMediaPlayer(context: Context, index: Int) {
        val songFile = songNames[index]
        val targetNum = index + 1
        val mp = MediaPlayer()

        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )

        // Keep running in background when device goes to sleep / app is backgrounded
        try {
            mp.setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set wake mode", e)
        }

        var sourceConfigured = false

        // 1. Search inside APK zip (specifically for MT Manager injected files in res/song/, res/song/my_all_song/, etc.)
        try {
            val apkPath = context.packageCodePath
            if (apkPath != null && File(apkPath).exists()) {
                ZipFile(File(apkPath)).use { zip ->
                    val possiblePaths = listOf(
                        "res/song/$songFile",
                        "res/song/my_all_song/$songFile",
                        "assets/song/$songFile",
                        "res/raw/$songFile"
                    )

                    var matchedEntry = possiblePaths.mapNotNull { zip.getEntry(it) }.firstOrNull()

                    // If not found in exact path, search any entry matching song1.mp4, song2.mp4, etc.
                    if (matchedEntry == null) {
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val name = entry.name.lowercase()
                            if (name.endsWith("/song$targetNum.mp4") || name == "song$targetNum.mp4" || name.contains("my_all_song/song$targetNum.mp4")) {
                                matchedEntry = entry
                                break
                            }
                        }
                    }

                    if (matchedEntry != null) {
                        val cacheTarget = File(context.cacheDir, "mt_song_$targetNum.mp4")
                        if (!cacheTarget.exists() || cacheTarget.length() != matchedEntry.size) {
                            zip.getInputStream(matchedEntry).use { input ->
                                FileOutputStream(cacheTarget).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        if (cacheTarget.exists() && cacheTarget.length() > 0) {
                            mp.setDataSource(cacheTarget.absolutePath)
                            sourceConfigured = true
                            Log.d(TAG, "Loaded song $targetNum from APK entry: ${matchedEntry.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ZipFile search failed, falling back to resources", e)
        }

        // 2. Search local storage (e.g. app files, sdcard)
        if (!sourceConfigured) {
            val externalSong = File(context.getExternalFilesDir(null), "song/$songFile")
            val internalSong = File(context.filesDir, "song/$songFile")
            val sdcardSong = File("/sdcard/song/$songFile")

            val fileToUse = listOf(externalSong, internalSong, sdcardSong).firstOrNull { it.exists() && it.length() > 0 }
            if (fileToUse != null) {
                mp.setDataSource(fileToUse.absolutePath)
                sourceConfigured = true
                Log.d(TAG, "Loaded song from local storage: ${fileToUse.absolutePath}")
            }
        }

        // 3. Search compiled Android raw resources (e.g. R.raw.song1)
        if (!sourceConfigured) {
            val rawName = "song$targetNum"
            val rawId = context.resources.getIdentifier(rawName, "raw", context.packageName)
            if (rawId != 0) {
                try {
                    val afd = context.resources.openRawResourceFd(rawId)
                    if (afd != null) {
                        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                        sourceConfigured = true
                        Log.d(TAG, "Loaded song from raw resource: $rawName")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "openRawResourceFd failed", e)
                }
            }
        }

        // 4. Search AssetManager (assets/song/songX.mp4)
        if (!sourceConfigured) {
            try {
                val afd = context.assets.openFd("song/$songFile")
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                sourceConfigured = true
                Log.d(TAG, "Loaded song from assets: song/$songFile")
            } catch (e: Exception) {
                Log.w(TAG, "Asset openFd failed", e)
            }
        }

        if (!sourceConfigured) {
            _statusText.value = "$songFile ফাইলটি পাওয়া যায়নি! res/song/ ফোল্ডারে রাখুন।"
            _isPlaying.value = false
            return
        }

        mp.setOnPreparedListener { player ->
            player.start()
            _isPlaying.value = true
            _statusText.value = "চলছে (Playing): ${songNames[index]}"
        }

        // Loop automatically to the next song when one song finishes
        mp.setOnCompletionListener {
            nextSong(context)
        }

        mp.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
            _isPlaying.value = false
            _statusText.value = "প্লেব্যাক ত্রুটি (Error $what)"
            true
        }

        mediaPlayer = mp
        mp.prepareAsync()
    }
}
