package com.myhobby.gutenbergwebaccess.screens

/*
Gutenberg Web Access's raison d'être is to provide simple access to
the Gutenberg Project website of 70,000 plus books to both
sighted and blind users.  It is provided without charge under the
agpl-3.0 license.

    Copyright (C) 2025 Frank D. Ducrest

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt
import com.myhobby.gutenbergwebaccess.viewmodels.OggPlaybackViewModel
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat



/**
 * A media player screen for playing individual OGG audio chapters of a downloaded book.
 *
 * This composable function provides the user interface and playback logic for a single
 * audio chapter. It manages a [MediaPlayer] instance to handle playback, pausing,
 * seeking, and speed control. The player's state, such as the current position, is
 * periodically saved using the [OggPlaybackViewModel] to allow for seamless resuming.
 *
 * Key Features:
 * - **Playback Control:** Play, pause, and seek functionality via a slider.
 * - **Speed Control:** Allows the user to change the playback speed (on supported Android versions).
 * - **State Persistence:** Automatically saves the chapter and position to the [OggPlaybackViewModel].
 * - **Lifecycle Management:** Correctly initializes and releases the [MediaPlayer] instance
 *   using a [DisposableEffect] to prevent resource leaks.
 * - **UI Display:** Shows the current playback time, total duration, and the name of the file being played.
 *
 * @param navController The [NavController] used for handling navigation events, such as returning to the previous screen.
 * @param oggPlaybackViewModel The shared [OggPlaybackViewModel] used to save and retrieve the book's playback state.
 * @param oggFolderPath The folder path of the audiobook, used to construct the file path and as a key for state saving.
 * @param currentChapterNumber The specific chapter number within the book to be played.
 */
@SuppressLint("SuspiciousIndentation", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OggPlayer(
    navController: NavController,
    oggPlaybackViewModel: OggPlaybackViewModel,
    oggFolderPath: String?,
    currentChapterNumber: Int?
)
{

    //Log.d("OggPlayer", "OggPlayer Composable entered")
    //Log.d("OggPlayer start", "OggFolderPath: $oggFolderPath $currentChapterNumber")

    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var isMediaPlayerPrepared by remember { mutableStateOf(false) }

    val defaultSpeed by oggPlaybackViewModel.defaultSpeakingSpeedFlow.collectAsState(initial = 1.0f)

    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    val folderNameParts = oggFolderPath?.split("/")
    val oggFileName = folderNameParts?.last()

    var chapterNumber by remember { mutableStateOf(currentChapterNumber ?: 1) }


    var currentChapter = chapterNumber.toString().padStart(2, '0')
    //Log.d("OggPlayer 1", "chapterNumber: $chapterNumber")
    //Log.d("OggPlayer 1", "currentChapter: $currentChapter")

    var oggFilePath = "${oggFolderPath.toString()}/${oggFileName}_${currentChapter.toString()}.ogg"
    //Log.d("OggPlayer 1", "OggFilePath: $oggFilePath")

    var currentPosition by remember { mutableIntStateOf(0) }
    var totalDuration by remember { mutableIntStateOf(0) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    var isSeeking by remember { mutableStateOf(false) }
    var lastSaveTime by remember { mutableStateOf(0L) }

    var isFirstTime by remember { mutableStateOf(true) }

    val mediaSession = remember {
        MediaSessionCompat(context, "OggPlayerSession")
    }

/**
 * A [Runnable] object responsible for periodically updating the playback progress.
 *
 * This runnable is posted to a [Handler] to execute every second while the media
 *player is active. Its primary jobs are:
 * 1.  To fetch the `currentPosition` from the [MediaPlayer] and update the UI state,
 *     which moves the slider's thumb.
 * 2.  To call the [OggPlaybackViewModel] to  persist the current playback position
 *      (`folderPath`, `chapter`, `position`) to storage.
 *
 *      It ensures these updates only occur when the media is actively playing and not being
 *      manually seeked by the user, preventing conflicts. It reschedules itself to run
 *      again after a 1-second delay, creating a continuous update loop.
 */
val updateProgressRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isMediaPlayerPrepared && mediaPlayer.isPlaying && !isSeeking) {
                try {
                    currentPosition = mediaPlayer.currentPosition
                    if (oggFolderPath != null) {
                        oggPlaybackViewModel.updatePlaybackState(
                            folderPath = oggFolderPath,
                            chapter = chapterNumber,
                            position = currentPosition
                        )
                    }
                } catch (e: IllegalStateException) {
                    // Log.e("OggPlayer", "Error getting current position: ${e.message}")
                    // Optionally stop updates or handle error
                }
                if (System.currentTimeMillis() - lastSaveTime > 5000) {
                    lastSaveTime = System.currentTimeMillis()
                }
            }
            if (isMediaPlayerPrepared) {
                handler.postDelayed(this, 1000)
            }
        }
    }



    /**
     * Manages the lifecycle of the MediaPlayer.
     * It sets up the MediaPlayer when `oggFilePath` is valid and not null.
     * Handles MediaPlayer preparation, errors, and completion.
     * Resets and releases the MediaPlayer when the Composable is disposed or `oggFilePath` changes.
     */
    DisposableEffect(oggFilePath) {

        if (oggFilePath == null) {
            Toast.makeText(context, "Ogg file path is null", Toast.LENGTH_LONG).show()
            isMediaPlayerPrepared = false
            currentPosition = 0
            totalDuration = 0
            return@DisposableEffect onDispose {}
        }

        val mediaSessionCallback = object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                if (isMediaPlayerPrepared && !mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    isPlaying = true
                }
            }

            override fun onPause() {
                if (isMediaPlayerPrepared && mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                }
            }

            override fun onSeekTo(pos: Long) {
                if (isMediaPlayerPrepared) {
                    mediaPlayer.seekTo(pos.toInt())
                    currentPosition = pos.toInt()
                }
            }
        }
        mediaSession.setCallback(mediaSessionCallback)

        try {
            mediaPlayer.reset()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            val file = File(oggFilePath)
            if (file.exists()) {
                mediaPlayer.setDataSource(context, Uri.fromFile(file))
            } else {
                Toast.makeText(context, "Ogg file not found: $oggFilePath", Toast.LENGTH_LONG).show()
                isMediaPlayerPrepared = false
                currentPosition = 0
                totalDuration = 0
                return@DisposableEffect onDispose {}
            }

            /**
             * Called when the MediaPlayer is prepared for playback.
             * Retrieves the saved playback position, seeks to it, and starts playback.
             * Sets the initial playback speed and starts the progress updater.
             */
            mediaPlayer.setOnPreparedListener { mp ->
                isMediaPlayerPrepared = true
                totalDuration = mp.duration

                mediaSession.isActive = true

                // **NEW LOGIC**: Get the state for THIS specific book
                val savedState = oggFolderPath?.let { oggPlaybackViewModel.getPlaybackStateForBook(it) }

                currentSpeed = savedState?.speechRate ?: defaultSpeed


                val startPosition = if (savedState != null && savedState.chapter == chapterNumber) {
                    // If we're on the correct chapter for this book, resume from its saved position
                    savedState.position
                } else {
                    // Otherwise, it's a new chapter or a new book, start from the beginning
                    0
                }

                currentPosition = startPosition
                mp.seekTo(startPosition)


                // Apply the saved playback speed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val params = PlaybackParams().setSpeed(currentSpeed)
                    mp.playbackParams = params
                }

                // Start playing the audio
                try {
                    mp.start()
                    isPlaying = true // Update UI state *after* successfully starting
                    handler.post(updateProgressRunnable) // Start the progress updater
                } catch (e: IllegalStateException) {
                    // Log.e("OggPlayer", "Error starting player in onPrepared: ${e.message}")
                    Toast.makeText(context, "Error starting player.", Toast.LENGTH_SHORT).show()
                    isPlaying = false
                }
            }



            /**
             * Called when an error occurs during MediaPlayer operation.
             * Logs the error, displays a Toast, and resets player state.
             */
            mediaPlayer.setOnErrorListener { _, what, extra ->
                // Log.e("OggPlayer", "MediaPlayer Error: What: $what, Extra: $extra")
                Toast.makeText(context, "MediaPlayer Error: What: $what, Extra: $extra", Toast.LENGTH_LONG).show()
                isMediaPlayerPrepared = false
                isPlaying = false
                currentPosition = 0
                totalDuration = 0
                true
            }

            /**
             * Called when playback of the media source has completed.
             * Updates UI state and optionally saves the end position.
             */

            // In your setOnCompletionListener
            mediaPlayer.setOnCompletionListener {
                Toast.makeText(context, "Playback finished", Toast.LENGTH_SHORT).show()
                isPlaying = false
                currentPosition = totalDuration // Set slider to the end
                // Do NOT pop the back stack automatically. Let the user choose.
                // navController.popBackStack()
            }
            mediaPlayer.prepareAsync()

        } catch (e: Exception) {
            val message = when (e) {
                is IOException -> "Error setting data source: ${e.message}"
                is IllegalStateException -> "IllegalStateException during setup: ${e.message}"
                is SecurityException -> "SecurityException: ${e.message}"
                else -> "An unexpected error occurred during setup: ${e.message}"
            }
            // Log.e("OggPlayer", "Error in DisposableEffect: $message")
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            isMediaPlayerPrepared = false
            currentPosition = 0
            totalDuration = 0
        }

        /**
         * Cleans up MediaPlayer resources when the Composable is disposed.
         * Stops and releases the MediaPlayer, and removes callbacks for progress updates.
         */
        onDispose {
            //Log.d("OggPlayer", "onDispose called")

            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
                handler.removeCallbacks(updateProgressRunnable)
                mediaSession.release() // Release the media session
            } catch (e: IllegalStateException) {
                // Log.e("OggPlayer", "Error in onDispose while releasing media player: ${e.message}")
            }
            isMediaPlayerPrepared = false
            isPlaying = false
        }
    }

    /**
     * Helper function to manage the initial state of the Play/Pause button text.
     * Returns true only the first time it's called, then sets `isFirstTime` to false.
     * @return `true` if it's the first call, `false` otherwise.
     */
    fun isThisTheFirstTime(): Boolean {
        val result = isFirstTime
        isFirstTime = false
        return result
    }

    LaunchedEffect(isPlaying, currentPosition) {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, currentPosition.toLong(), currentSpeed)

        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }


    /**
     * Changes the playback speed of the MediaPlayer.
     * Requires Android M (API 23) or higher.
     * If the player is not prepared, the speed is stored and applied when the player becomes ready.
     *
     * @param newSpeed The new playback speed (e.g., 1.0f for normal, 0.5f for half speed).
     */
    fun changeSpeed(newSpeed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isMediaPlayerPrepared) {
                try {
                    val params = PlaybackParams().setSpeed(newSpeed)
                    mediaPlayer.playbackParams = params
                    currentSpeed = newSpeed
                    if (oggFolderPath != null) {
                        oggPlaybackViewModel.updatePlaybackSpeed(oggFolderPath, newSpeed)
                    }
                } catch (e: IllegalStateException) {
                    // Log.e("OggPlayer", "Error setting speed: ${e.message}")
                    Toast.makeText(context, "Error setting speed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                currentSpeed = newSpeed
                // Toast.makeText(context, "Speed will be applied when player is ready.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Playback speed control requires API 23+.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Formats a time duration in milliseconds into a "MM:SS" string.
     * @param millis The duration in milliseconds.
     * @return A string representation of the time in "MM:SS" format.
     */
    fun formatTimeMillis(millis: Int): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (oggFilePath == null) {
            Text("No audio file specified.")
            // Consider navigating back or showing a more prominent error
            // navController.popBackStack() // Already present, but ensure it's robust
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Gutenberg Web Access!",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isMediaPlayerPrepared && oggFolderPath != null) {
                    // The ViewModel is already up-to-date thanks to the runnable,
                    // but one final update ensures the very last position is saved.
                    oggPlaybackViewModel.updatePlaybackState(
                        folderPath = oggFolderPath,
                        chapter = chapterNumber,
                        position = currentPosition
                    )
                }
                navController.popBackStack()
            },
            // modifier = Modifier.semantics { contentDescription = "Back to Previous Screen" }
        ) {
            Text(text = "Back to Previous Screen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Playing: ${File(oggFilePath ?: "").name}", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        if (isMediaPlayerPrepared && totalDuration > 0) {
            Slider(
/*
                value = currentSpeed,
                onValueChange = { newSpeed ->
                    currentSpeed = newSpeed
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            // Apply speed immediately
                            val params = PlaybackParams().setSpeed(newSpeed)
                            mediaPlayer.playbackParams = params
                            // Save the new speed to the ViewModel for this book
                            if (oggFolderPath != null) {
                                oggPlaybackViewModel.updatePlaybackSpeed(oggFolderPath, newSpeed)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not set speed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = 0.5f..2.0f,
                // --- THIS IS THE KEY ---
                // Number of steps = ((2.0 - 0.5) / 0.1) - 1 = 14
                steps = 14

*/
                value = currentPosition.toFloat(),
                onValueChange = { newValue ->
                    isSeeking = true
                    currentPosition = newValue.roundToInt()
                    // No immediate save on onValueChange, save on onValueChangeFinished or periodically
                },
                onValueChangeFinished = {
                    try {
                        if (isMediaPlayerPrepared) {
                            mediaPlayer.seekTo(currentPosition)
                        }
                    } catch (e: IllegalStateException) {
                        // Log.e("OggPlayer", "Error seeking from slider: ${e.message}")
                    }
                    isSeeking = false
                    if (!mediaPlayer.isPlaying && isMediaPlayerPrepared) {
                        handler.removeCallbacks(updateProgressRunnable)
                        handler.post(updateProgressRunnable)
                    }
                },
                valueRange = 0f..totalDuration.toFloat(),
                modifier = Modifier.fillMaxWidth()

            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTimeMillis(currentPosition))
                Text(formatTimeMillis(totalDuration))
            }
        } else if (oggFilePath != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    if (!isMediaPlayerPrepared) {
                        Toast.makeText(context, "Player not ready yet", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isPlaying) { // If UI thinks it is playing, then pause
                        try {
                            mediaPlayer.pause()
                            isPlaying = false
                            // //Log.d("OggPlayer", "Player paused")
                        } catch (e: IllegalStateException) {
                            // Log.e("OggPlayer", "Error pausing player: ${e.message}")
                            // Handle error, maybe reset state
                        }
                    } else { // If UI thinks it is paused/stopped, then play
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val params = PlaybackParams().setSpeed(currentSpeed)
                                mediaPlayer.playbackParams = params
                            }
                            mediaPlayer.start()
                            isPlaying = true
                            // //Log.d("OggPlayer", "Player started")
                            // Ensure progress updates are active
                            handler.removeCallbacks(updateProgressRunnable)
                            handler.post(updateProgressRunnable)
                        } catch (e: IllegalStateException) {
                            // Log.e("OggPlayer", "Error starting player: ${e.message}")
                            Toast.makeText(context, "Error starting player. Please try again.", Toast.LENGTH_SHORT).show()
                            // Consider resetting player or state if start fails critically
                            isMediaPlayerPrepared = false // Force re-prepare if start fails badly
                            mediaPlayer.reset() // Example: reset to allow re-prepare
                            // This is aggressive, adapt based on expected errors
                        }
                    }
                },
                enabled = isMediaPlayerPrepared,
                // modifier = Modifier.semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
                //// modifier = Modifier.semantics {contentDescription = if (isThisTheFirstTime() || isPlaying) "Pause" else "Play"}
            ) {
                Text(if (isThisTheFirstTime() || isPlaying) "Pause" else "Play")
            }

            // In the "Beginning" Button
            Button(
                onClick = {
                    if (isMediaPlayerPrepared) {
                        try {
                            mediaPlayer.seekTo(0)
                            currentPosition = 0
                            // If paused, ensure the slider updates to the new position
                            if (!isPlaying) {
                                handler.removeCallbacks(updateProgressRunnable)
                                handler.post(updateProgressRunnable)
                            }
                        } catch (e: IllegalStateException) {
                            // Log.e("OggPlayer", "Error seeking to beginning: ${e.message}")
                        }
                    }
                },
                // modifier = Modifier.semantics { contentDescription = "Go to Beginning" }, // Changed for clarity
                enabled = isMediaPlayerPrepared
            ) {
                Text("Beginning")
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Speed: ${"%.1f%%".format((currentSpeed) * 100f)}\"")

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                onClick = {
                    // Calculate the new speed, ensuring it doesn't go below the minimum.
                    val newSpeed = (currentSpeed - 0.1f).coerceAtLeast(0.5f)
                    changeSpeed(newSpeed)
                },
                // The button is enabled as long as the speed is above the minimum
                enabled = isMediaPlayerPrepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed > 0.5f
            ) {
                Text(text = "Slower")
            }

            // Plus Button
            Button(
                onClick = {
                    // Calculate the new speed, ensuring it doesn't exceed the maximum.
                    val newSpeed = (currentSpeed + 0.1f).coerceAtMost(2.0f)
                    changeSpeed(newSpeed)
                },
                // The button is enabled as long as the speed is below the maximum
                enabled = isMediaPlayerPrepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed < 2.0f
            ) {
                Text(text = "Faster")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isMediaPlayerPrepared && oggFilePath != null && totalDuration == 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Loading audio...")
        }
    }
}
