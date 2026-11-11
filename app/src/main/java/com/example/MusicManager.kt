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
        var loadedSourceDescription = ""

        // 1. Search inside APK zip (for MT Manager injected or replaced files in res/raw, res/song, assets/song, etc.)
        try {
            val apkPath = context.packageCodePath
            if (apkPath != null && File(apkPath).exists()) {
                ZipFile(File(apkPath)).use { zip ->
                    val targetBase = "song$targetNum"
                    val validExtensions = listOf(".mp4", ".mp3", ".m4a", ".aac", ".ogg", ".wav")

                    // First check preferred paths
                    val preferredPaths = listOf(
                        "res/raw/$songFile",
                        "res/song/$songFile",
                        "res/song/my_all_song/$songFile",
                        "assets/song/$songFile"
                    )

                    var matchedEntry: java.util.zip.ZipEntry? = preferredPaths.mapNotNull { zip.getEntry(it) }.firstOrNull()

                    // If not found, search all entries case-insensitively
                    if (matchedEntry == null) {
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val nameLower = entry.name.lowercase()
                            val fileName = nameLower.substringAfterLast('/')
                            val baseName = fileName.substringBeforeLast('.')
                            val ext = if (fileName.contains('.')) "." + fileName.substringAfterLast('.') else ""

                            if (baseName == targetBase && (ext in validExtensions || ext.isEmpty())) {
                                matchedEntry = entry
                                break
                            }
                        }
                    }

                    if (matchedEntry != null) {
                        val cacheTarget = File(context.cacheDir, "mt_song_$targetNum.mp4")
                        // Extract fresh copy if not existing or if size doesn't match
                        val needExtract = !cacheTarget.exists() || 
                                          (matchedEntry.size > 0 && cacheTarget.length() != matchedEntry.size) ||
                                          (matchedEntry.time > 0 && cacheTarget.lastModified() < matchedEntry.time)

                        if (needExtract) {
                            zip.getInputStream(matchedEntry).use { input ->
                                FileOutputStream(cacheTarget).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }

                        if (cacheTarget.exists() && cacheTarget.length() > 0) {
                            mp.setDataSource(cacheTarget.absolutePath)
                            sourceConfigured = true
                            loadedSourceDescription = "APK: ${matchedEntry.name}"
                            Log.d(TAG, "Loaded song $targetNum from APK entry: ${matchedEntry.name} (${cacheTarget.length()} bytes)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ZipFile search failed, falling back to resources", e)
        }

        // 2. Search compiled Android raw resources (e.g. R.raw.song1) via openRawResource stream (safe against closed FD bugs)
        if (!sourceConfigured) {
            val rawName = "song$targetNum"
            val rawId = context.resources.getIdentifier(rawName, "raw", context.packageName)
            if (rawId != 0) {
                try {
                    val cacheTarget = File(context.cacheDir, "res_raw_song_$targetNum.mp4")
                    context.resources.openRawResource(rawId).use { input ->
                        FileOutputStream(cacheTarget).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (cacheTarget.exists() && cacheTarget.length() > 0) {
                        mp.setDataSource(cacheTarget.absolutePath)
                        sourceConfigured = true
                        loadedSourceDescription = "res/raw/$rawName"
                        Log.d(TAG, "Loaded song from raw resource stream: $rawName (${cacheTarget.length()} bytes)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "openRawResource stream failed", e)
                }
            }
        }

        // 3. Search local device storage (e.g. /sdcard/song/, Download/song/, app files)
        if (!sourceConfigured) {
            val candidateFiles = listOf(
                File("/sdcard/song/$songFile"),
                File("/storage/emulated/0/song/$songFile"),
                File("/storage/emulated/0/Download/song/$songFile"),
                File("/storage/emulated/0/Music/song/$songFile"),
                File(context.getExternalFilesDir(null), "song/$songFile"),
                File(context.filesDir, "song/$songFile")
            )

            val foundFile = candidateFiles.firstOrNull { it.exists() && it.length() > 0 }
            if (foundFile != null) {
                mp.setDataSource(foundFile.absolutePath)
                sourceConfigured = true
                loadedSourceDescription = "Storage: ${foundFile.name}"
                Log.d(TAG, "Loaded song from local storage: ${foundFile.absolutePath}")
            }
        }

        // 4. Search AssetManager (assets/song/songX.mp4) via stream
        if (!sourceConfigured) {
            try {
                val assetPath = "song/$songFile"
                val cacheTarget = File(context.cacheDir, "asset_song_$targetNum.mp4")
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(cacheTarget).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cacheTarget.exists() && cacheTarget.length() > 0) {
                    mp.setDataSource(cacheTarget.absolutePath)
                    sourceConfigured = true
                    loadedSourceDescription = "assets/$assetPath"
                    Log.d(TAG, "Loaded song from assets stream: $assetPath")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Asset stream open failed", e)
            }
        }

        if (!sourceConfigured) {
            _statusText.value = "$songFile ফাইলটি পাওয়া যায়নি! res/raw/ বা res/song/-এ রাখুন।"
            _isPlaying.value = false
            return
        }

        mp.setOnPreparedListener { player ->
            player.start()
            _isPlaying.value = true
            _statusText.value = "চলছে: ${songNames[index]}"
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
