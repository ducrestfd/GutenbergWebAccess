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

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.myhobby.gutenbergwebaccess.viewmodels.AudioLocationViewModel
import com.myhobby.gutenbergwebaccess.viewmodels.BookPlaybackState
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import android.support.v4.media.session.PlaybackStateCompat


/**
 * Preprocesses a string of text to improve its suitability for Text-to-Speech (TTS) synthesis.
 *
 * Currently, this function focuses on removing periods from common honorifics (e.g., "Mr.", "Ms.")
 * to prevent the TTS engine from pausing unnaturally as if it were the end of a sentence.
 *
 * For example, "Mr. Smith" becomes "Mr Smith".
 *
 * @param text The input [String] to be preprocessed.
 * @return A new [String] with honorifics adjusted for better TTS flow.
 */
fun preprocessTextForTTS(text: String): String {
    var processedText = text

    // Add more honorifics as needed
    val honorifics = listOf("Mr.", "Ms.", "Mrs.", "Dr.", "Prof.")
    honorifics.forEach { honorific ->
        // Replace "Mr. " with "Mr " (notice the space to avoid replacing mid-word)
        processedText = processedText.replace("$honorific ", honorific.removeSuffix(".") + " ")
        // Handle cases where the honorific might be at the end of the input string
        if (processedText.endsWith(honorific)) {
            processedText = processedText.removeSuffix(".")
        }
    }
    return processedText
}


/**
 * A Composable screen that provides a Text-to-Speech (TTS) interface for reading book content.
 *
 * This screen is a fully-featured audio player that uses Android's native TTS engine to read
 * text extracted from a local HTML file. It manages the entire playback lifecycle, including
 * playing, pausing, and navigating through sentences. It also integrates with the Android
 * media system to respond to system-wide commands, like the two-finger double-tap gesture
 * for play/pause when TalkBack is active.
 *
 * Key features:
 * - **State Persistence**: Uses [AudioLocationViewModel] to save and load the reader's progress,
 *   including the current sentence, custom speech rate, and pitch for each book.
 * - **Default Settings**: Fetches and applies the user's globally configured default speaking
 *   speed for books being read for the first time.
 * - **Media Session Integration**: Correctly implements [MediaSessionCompat] to handle media
 *   commands from the system, headphones, and accessibility services.
 * - **UI Controls**: Provides a comprehensive set of UI controls for playback (Play/Pause,
 *   Beginning, Back, Forward) and for adjusting TTS parameters (Rate and Pitch).
 * - **Lifecycle Management**: Safely initializes and shuts down the TTS engine and media
 *   session within a [DisposableEffect] to prevent memory leaks.
 * - **Text Processing**: Loads text content from an HTML file using Jsoup and splits it into
 *   sentences for playback.
 *
 * @param navController The [NavController] used for handling navigation actions, such as
 *   returning to the previous screen.
 * @param viewModel The [AudioLocationViewModel] instance that provides access to book progress
 *   and default settings.
 * @param encodedFilePath A URL-encoded [String] representing the absolute path to the local
 *   HTML file to be read.
 * @param languageCode An optional language code (e.g., "en", "fr") used to set the TTS
 *   language, falling back to system defaults if not supported.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSpeechBookReader( // Renamed for clarity
    navController: NavController,
    viewModel: AudioLocationViewModel,
    encodedFilePath: String?, // Keep if you use it for title or to load text content
    languageCode: String?
) {
    val filePath = remember(encodedFilePath) {
        encodedFilePath?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
    }

    val fileName = filePath?.substringAfterLast('/') ?: "Unknown File"

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsInitialized by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentSentenceIndex by remember { mutableStateOf(0) }
    var bookSentences by remember { mutableStateOf<List<String>>(emptyList()) }
    var isProgressLoaded by remember { mutableStateOf(false) }
    val defaultSpeed by viewModel.defaultSpeakingSpeedFlow.collectAsState(initial = 1.0f)
    var speechRate by remember { mutableStateOf(1.0f) }
    var speechPitch by remember { mutableStateOf(1.0f) }

    val context = LocalContext.current

    // Calculate progress for the LinearProgressIndicator
    val progress = if (bookSentences.isNotEmpty()) {
        (currentSentenceIndex + 1).toFloat() / bookSentences.size.toFloat()
    } else {
        0f
    }

    /**
     * An instance of [MediaSessionCompat] to integrate the TTS player with the Android system.
     *
     * This session allows the app to receive media control commands from external sources,
     * such as Bluetooth headphones, lock screen controls, or accessibility services like
     * TalkBack (e.g., handling the two-finger double-tap for play/pause). It communicates
     * the player's current state (playing, paused) back to the system.
     *
     * The session is remembered across recompositions and its lifecycle is managed within
     * a [DisposableEffect], ensuring it is properly released when the screen is destroyed.
     */
    val mediaSession = remember {
        MediaSessionCompat(context, "TextToSpeechReaderSession")
    }

    /**
     * The central function for initiating or resuming TTS playback.
     *
     * This function is the single entry point for all actions that result in speech.
     * It ensures that before any utterance is spoken, the TTS engine is correctly configured
     * with the latest `speechRate` and `speechPitch` values from the UI state.
     *
     * It handles multiple scenarios:
     * - Starting playback from a stopped state.
     * - Resuming playback from a paused state.
     * - Re-speaking the current sentence (e.g., after a rate or pitch change).
     * - Speaking the next sentence in a sequence (e.g., when called from `onDone`).
     *
     * @param isNewSentence A boolean flag indicating the context of the call.
     *   - `true` if playback is for a sentence that the player is advancing to (e.g., from `onDone` or "Next").
     *   - `false` if playback should start or resume on the `currentSentenceIndex` (e.g., from "Play" or after a rate change).
     */
    val startPlayback = { isNewSentence: Boolean ->
        if (isTtsInitialized && bookSentences.isNotEmpty()) {
            if (isNewSentence || isPaused || !isSpeaking) {
                isSpeaking = true
                isPaused = false // We are no longer paused

                // CRITICAL: Always apply the current rate and pitch before speaking.
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(speechPitch)

                tts?.speak(
                    preprocessTextForTTS(bookSentences[currentSentenceIndex]),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "BookSentence_$currentSentenceIndex"
                )
            }
        }
    }


    /**
     * A callback object that bridges the gap between system-wide media commands and the app's
     * specific TTS logic.
     *
     * This callback is registered with the [mediaSession] and defines how the player should
     * react to standard media events. For example, when an accessibility service like TalkBack
     * sends an `onPlay` command (triggered by a two-finger double-tap), this callback
     * translates it into a call to the app's internal [startPlayback] function.
     *
     * - `onPlay()`: Maps the system "play" command to resume or start TTS playback.
     * - `onPause()`: Maps the system "pause" command to stop the TTS engine and update the
     *   UI state to "paused".
     */
    val mediaSessionCallback = remember {
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                // When system says "Play", call our function to start speaking
                startPlayback(false)
            }

            override fun onPause() {
                // When system says "Pause", stop the TTS engine
                if (isSpeaking) {
                    isSpeaking = false
                    isPaused = true
                    tts?.stop() // This triggers onStop, which sets isSpeaking to false
                }
            }
        }
    }


    /**
     * A [LaunchedEffect] that keeps the [mediaSession] synchronized with the player's UI state.
     *
     * This effect observes the `isSpeaking` and `isPaused` state variables. Whenever either
     * of these variables changes, it reconstructs the [PlaybackStateCompat] and updates the
     * media session. This is crucial for informing the Android system about the player's
     * current status (e.g., playing, paused, or stopped).
     *
     * By providing an accurate state, it allows system services like TalkBack to correctly
     * interpret user actions, such as converting a two-finger double-tap into the appropriate
     * `onPlay()` or `onPause()` command.
     */
    LaunchedEffect(key1 = filePath, key2 = isTtsInitialized) {
        if (filePath != null && isTtsInitialized && !isProgressLoaded) {
            viewModel.viewModelScope.launch {
                // Fetch the complete playback state object, not just an Int
                val savedState: BookPlaybackState? =
                    viewModel.getBookProgressAsync(fileName).await()

                val locale = if (!languageCode.isNullOrBlank())
                    Locale(languageCode)
                else
                    Locale.US

                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language '$languageCode' not supported. Falling back to default.")
                    // Attempt to fall back to the device's default language
                    tts?.setLanguage(Locale.getDefault())
                } else {
                    // Log.d("TTS", "Language set to: ${locale.displayLanguage}")
                }

                if (savedState != null) {
                    // Apply all saved values from the state object
                    currentSentenceIndex =
                        if (savedState.chapter >= 0 && savedState.chapter < bookSentences.size) savedState.chapter else 0
                    speechRate = savedState.speechRate
                    speechPitch = savedState.speechPitch

                    // Apply the loaded rate and pitch to the TTS engine immediately
                    tts?.setSpeechRate(speechRate)
                    tts?.setPitch(speechPitch)
                } else {
                    // No saved state, use defaults
                    currentSentenceIndex = 0
                    // --- THIS IS THE FIX ---
                    speechRate = defaultSpeed // Use the default speed from settings
                    speechPitch = 1.0f

                    // Also apply this default to the TTS engine
                    tts?.setSpeechRate(speechRate)
                    tts?.setPitch(speechPitch)
                }
            }
            isProgressLoaded = true
        }
    }

    // Effect to reset isProgressLoaded if filePath changes, allowing reloading for a new book
    LaunchedEffect(filePath) {
        isProgressLoaded = false // Reset when filePath changes
        currentSentenceIndex = 0 // Also reset current index for the new book before loading
        // //Log.d(
        //    "TTS_AudioPlayer",
        //    "filePath changed to $filePath. Resetting isProgressLoaded and currentSentenceIndex."
        //)
    }


    // Effect to load text content for TTS
    // This is a placeholder. You need to implement how bookSentences gets populated.
    DisposableEffect(filePath) {
        if (filePath != null) {
            // Example: Load and parse HTML content for TTS
            // This is a simplified example. Error handling and more robust parsing would be needed.
            try {
                val bookFile = File(filePath)
                if (bookFile.exists() && (bookFile.extension.equals(
                        "html",
                        ignoreCase = true
                    ) || bookFile.extension.equals("htm", ignoreCase = true))
                ) {
                    val doc = Jsoup.parse(bookFile, StandardCharsets.UTF_8.name())
                    val textContent = doc.body().text() // Basic text extraction

                    // sentence splitting
                    //bookSentences = textContent.split(Regex("(?<=[.!?])\\s+"))
                    //    .filter { it.isNotBlank() }
                    val sentenceEndRegex = Regex("(?<!\\b(?:Mr|Mrs|Ms|Dr|Prof))[.!?]\\s+")
                    bookSentences = textContent.split(sentenceEndRegex)
                        .map { it.trim() } // Trim whitespace from each sentence
                        .filter { it.isNotBlank() }

                    // //Log.d("AudioOnlyPlayer", "Loaded ${bookSentences.size} sentences.")
                    for (sentence in bookSentences) {
                        // //Log.d("AudioOnlyPlayer", "*********** Sentence: $sentence")
                    }
                    currentSentenceIndex = 0 // Reset index when new content is loaded
                } else {
                    // Log.e("AudioOnlyPlayer", "Book file does not exist or is not HTML: $filePath")
                    bookSentences = emptyList()
                }
            } catch (e: Exception) {
                // Log.e("AudioOnlyPlayer", "Error loading or parsing book content: $filePath", e)
                bookSentences = emptyList()
            }
        }
        onDispose {
            // Cleanup if needed when filePath changes
        }
    }


    // This LaunchedEffect keeps the MediaSession state in sync with the player's state.
    LaunchedEffect(isSpeaking, isPaused) {
        val state = when {
            isSpeaking -> PlaybackStateCompat.STATE_PLAYING
            isPaused -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_STOPPED
        }

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE) // Tell the system we support play/pause
                .setState(state, 0L, 1.0f) // Position and speed are less critical for TTS
                .build()
        )
    }

    /**
     * A [DisposableEffect] responsible for the setup and teardown of the TTS engine and Media Session.
     *
     * This effect runs once when the composable enters the composition (`Unit` key). It performs
     * critical initializations and ensures proper cleanup to prevent resource leaks.
     *
     * **Setup:**
     * 1.  Registers the `mediaSessionCallback` with the `mediaSession`.
     * 2.  Activates the `mediaSession` to allow the system to send media commands.
     * 3.  Initializes the Android [TextToSpeech] engine asynchronously.
     * 4.  Upon successful TTS initialization, it sets up an [UtteranceProgressListener] to listen
     *     for playback events like `onStart`, `onDone`, and `onStop`, which drive the player's
     *     state machine.
     *
     * **Teardown (`onDispose`):**
     * 1.  Stops any active TTS playback.
     * 2.  Shuts down the TTS engine to release its native resources.
     * 3.  Saves the final playback state (current sentence, rate, pitch) to persistent storage.
     * 4.  Releases the `mediaSession` to unregister it from the system.
     */
    DisposableEffect(Unit) {

        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.isActive = true // Activate the session

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true

                tts?.language = Locale.getDefault()

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        isPaused = false
                    }

                    // --- 3. MODIFIED onDone TO SAVE FULL STATE ---
                    override fun onDone(utteranceId: String?) {
                        // Ensure this logic only runs for our book sentences and while actively speaking.
                        if (utteranceId?.startsWith("BookSentence_") == true && isSpeaking && !isPaused) {
                            val nextIndex = currentSentenceIndex + 1

                            if (nextIndex < bookSentences.size) {
                                // There is a next sentence, so update the index and save progress.
                                currentSentenceIndex = nextIndex
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    currentSentenceIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)

                                // Re-call the central playback function for the new sentence.
                                // This ensures the rate and pitch are correctly applied.
                                startPlayback(true)

                                // Now, speak the new sentence, flushing the queue.
                                /*
                                tts?.speak(
                                    preprocessTextForTTS(bookSentences[currentSentenceIndex]),
                                    TextToSpeech.QUEUE_FLUSH, // Use FLUSH to avoid queue conflicts
                                    null,
                                    "BookSentence_$currentSentenceIndex"
                                )
                                 */
                            } else {
                                // Reached the end of the book. Stop playback.
                                isSpeaking = false
                                isPaused = false
                                currentSentenceIndex = 0 // Optional: reset to beginning
                                // Save the final state (at the beginning)
                                val finalState = BookPlaybackState(
                                    fileName,
                                    0,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(finalState)
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        // This is fine, no changes needed
                        isSpeaking = false
                        isPaused = false
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        super.onStop(utteranceId, interrupted)

                        // Only fully reset the state if we are NOT in a paused state.
                        // If isPaused is true, it means the stop was triggered by the
                        // onPause callback, and we should preserve that state.
                        //if (!isPaused) {
                        isSpeaking = false
                        //}
                        // Never set isPaused = false here. Let the Play/Resume button control that.
                    }
                })
            }
        }


        /**
         * The cleanup block for the [DisposableEffect], executed when the composable is removed
         * from the composition (e.g., when the user navigates away from the screen).
         *
         * This block is crucial for preventing resource leaks and ensuring the user's
         * progress is not lost. It performs the following teardown actions in order:
         * 1.  Stops any currently active TTS utterance.
         * 2.  Shuts down the TTS engine, releasing all its associated native resources.
         * 3.  Saves the final playback state (current sentence, rate, and pitch) to persistent
         *     storage via the ViewModel, but only if the content has been successfully loaded.
         * 4.  Releases the [MediaSessionCompat], unregistering it from the Android system and
         *     freeing its resources.
         */
        onDispose {
            tts?.stop()
            tts?.shutdown()
            // --- 4. MODIFIED onDispose TO SAVE FULL STATE ---
            if (filePath != null && isProgressLoaded) {
                // Save the final state when the user leaves the screen
                val stateToSave = BookPlaybackState(
                    folderPath = fileName,
                    chapter = currentSentenceIndex,
                    position = 0,
                    speechRate = speechRate,
                    speechPitch = speechPitch
                )
                viewModel.saveBookProgress(stateToSave)
            }
            mediaSession.release() // Release the media session!
        }
    }

    // This LaunchedEffect tells the system (notifications, TalkBack) about your player's state.
    LaunchedEffect(isSpeaking, isPaused) {
        val state = when {
            isSpeaking -> PlaybackStateCompat.STATE_PLAYING
            isPaused -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_STOPPED
        }

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(state, 0L, 1.0f) // Position and speed are less relevant for TTS
                .build()
        )
    }


    /* The main layout structure for the TTS reader screen, built using [Scaffold].
    *
    * This Scaffold defines the top-level layout, providing slots for a top app bar
    * and the main content area.
    *
    * - `topBar`: A [TopAppBar] is used to display the book's title and a navigation
    *   icon to return to the previous screen. The back action also ensures that
    *   the current reading progress is saved before navigating.
    *
    * - `floatingActionButton`: This slot is repurposed to hold the main UI controls for
    *   playback and settings, centered on the screen for easy access. It contains
    *   buttons for Play/Resume, Pause, and returning to the beginning, as well as
    *   controls for adjusting speech rate and pitch.
    *
    * - `content`: The main content area contains the progress indicator and sentence
    *   counter, providing at-a-glance information about the current reading position.
    */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = filePath?.substringAfterLast('/') ?: "Audio Player") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            tts?.stop() // Ensure TTS stops when navigating back
                            if (fileName != null && isTtsInitialized) {
                                // Create the complete state object to save
                                val stateToSave = BookPlaybackState(
                                    folderPath = fileName,
                                    chapter = currentSentenceIndex,
                                    position = 0, // Not used for TTS, but required by the data class
                                    speechRate = speechRate,
                                    speechPitch = speechPitch
                                )
                                // Call the correct ViewModel function
                                viewModel.saveBookProgress(stateToSave)
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Return to Previous Screen" +
                                    ""
                        }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Gutenberg Web Access!",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.popBackStack()
                    },
                    // modifier = Modifier.semantics { contentDescription = "Back to Previous Screen" }
                ) {
                    Text(text = "Back to Previous Screen")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Button(
                        onClick = {

                            // When the user clicks play, it's not a "new" sentence from onDone, so pass false.
                            startPlayback(false)
                        },
                        //enabled = bookSentences.isNotEmpty()
                        enabled = isTtsInitialized && bookSentences.isNotEmpty() && !isSpeaking,
                    ) {
                        Text(if (isPaused) "Resume" else "Play")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (isSpeaking) {
                                // 1. Declare user intent FIRST.
                                isPaused = true
                                isSpeaking = false // Speaking will stop.
                                // 2. Then, execute the action.
                                tts?.stop()
                            }
                        },
                        enabled = isTtsInitialized && isSpeaking && !isPaused,
                    ) {
                        Text("Pause")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (isTtsInitialized && tts != null) {
                                isPaused = true // Set state to paused
                                isSpeaking = false // Stop speaking
                                currentSentenceIndex = 0 // Set current sentence to the beginning
                                tts?.stop() // Stop current TTS playback

                                // Persist the change in reading position
                                if (fileName != null) {
                                    // Create a full state object representing the "beginning"
                                    val stateToSave =
                                        BookPlaybackState(fileName, 0, 0, speechRate, speechPitch)
                                    viewModel.saveBookProgress(stateToSave)
                                }

                            } else {
                                // Log.w("TTS_AudioPlayer", "Beginning button: TTS not initialized or tts is null.")
                            }
                        },
                        enabled = isTtsInitialized && (isSpeaking || isPaused), // Enable if TTS is ready and either speaking or paused
                        // modifier = Modifier.semantics { contentDescription = "Pause Speaking and return to Beginning" }
                    ) {
                        Text("Beginning")
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (bookSentences.isNotEmpty()) { // Only show if there are sentences
                    LinearProgressIndicator(
                        progress = { progress }, // Use the calculated progress
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 32.dp,
                                vertical = 16.dp
                            ),// Give it some horizontal padding
                        color = MaterialTheme.colorScheme.primary, // Use theme colors
                        trackColor = MaterialTheme.colorScheme.surfaceVariant // Use theme colors
                    )
                }

                val currentProgress = (progress * 100).toInt()
                Text("Progress: $currentProgress%")

                Spacer(Modifier.height(16.dp))

                Row {

                    Button(
                        onClick = {
                            if (currentSentenceIndex - 30 >= 0) {
                                val prevIndex = currentSentenceIndex - 30

                                // Save the new position
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    prevIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)

                                // Set the new index
                                currentSentenceIndex = prevIndex

                                if (isSpeaking || isPaused) {
                                    startPlayback(true)
                                }

                            }
                        },
                        // Enable if we are not on the first sentence.
                        enabled = isTtsInitialized && currentSentenceIndex > 0
                    ) {
                        Text("-30")
                    }

                    Button(
                        onClick = {
                            if (currentSentenceIndex - 10 >= 0) {
                                val prevIndex = currentSentenceIndex - 10

                                // Save the new position
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    prevIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)

                                // Set the new index
                                currentSentenceIndex = prevIndex

                                if (isSpeaking || isPaused) {
                                    startPlayback(true)
                                }

                            }
                        },
                        // Enable if we are not on the first sentence.
                        enabled = isTtsInitialized && currentSentenceIndex > 0
                    ) {
                        Text("-10")
                    }

                    //Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (currentSentenceIndex + 10 < bookSentences.size - 1) {
                                val nextIndex = currentSentenceIndex + 10

                                // Save the new position immediately
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    nextIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)

                                // Set the new index
                                currentSentenceIndex = nextIndex

                                if (isSpeaking || isPaused) {
                                    startPlayback(true)
                                }
                            }
                        },
                        enabled = isTtsInitialized && currentSentenceIndex < bookSentences.size - 1
                    ) {
                        Text("+10")
                    }


                    Button(
                        onClick = {
                            if (currentSentenceIndex + 30 < bookSentences.size - 1) {
                                val nextIndex = currentSentenceIndex + 30

                                // Save the new position immediately
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    nextIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)

                                // Set the new index
                                currentSentenceIndex = nextIndex

                                if (isSpeaking || isPaused) {
                                    startPlayback(true)
                                }
                            }
                        },
                        enabled = isTtsInitialized && currentSentenceIndex < bookSentences.size - 1
                    ) {
                        Text("+30")
                    }


                    Button(
                        onClick = {
                            if (currentSentenceIndex + 100 < bookSentences.size - 1) {
                                val nextIndex = currentSentenceIndex + 100

                                // Save the new position immediately
                                val stateToSave =
                                    BookPlaybackState(
                                        fileName,
                                        nextIndex,
                                        0,
                                        speechRate,
                                        speechPitch
                                    )
                                viewModel.saveBookProgress(stateToSave)

                                // Set the new index
                                currentSentenceIndex = nextIndex

                                if (isSpeaking || isPaused) {
                                    startPlayback(true)
                                }
                            }
                        },
                        enabled = isTtsInitialized && currentSentenceIndex < bookSentences.size - 1
                    ) {
                        Text("+100")
                    }
                }

                Spacer(Modifier.height(16.dp))

                //Text("Rate: ${String.format("%.1f", speechRate)}x")
                Text("Rate: ${"%.1f%%".format(speechRate * 100f)}")

                Spacer(Modifier.height(16.dp))

                Row {
                    Button(
                        onClick = {
                            if (isTtsInitialized) {
                                // 1. ONLY update the state variable.
                                speechRate = (speechRate - 0.1f).coerceAtLeast(0.1f)

                                // 2. Let startPlayback do the work of applying the rate and re-speaking.
                                if (isSpeaking) {
                                    startPlayback(false) // Re-speak the current sentence with the new rate
                                }

                                // 3. Save the new state.
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    currentSentenceIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)
                            }
                        },
                        enabled = isTtsInitialized && speechRate > 0.1f,
                    ) {
                        Text("Slower")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (isTtsInitialized) {
                                // 1. ONLY update the state variable.
                                speechRate = (speechRate + 0.1f).coerceAtMost(3.0f)

                                // 2. Let startPlayback do the work.
                                if (isSpeaking) {
                                    startPlayback(false) // Re-speak the current sentence with the new rate
                                }

                                // 3. Save the new state.
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    currentSentenceIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)
                            }
                        },
                        enabled = isTtsInitialized && speechRate < 3.0f,
                    ) {
                        Text("Faster")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Pitch: ${"%.1f%%".format(speechPitch * 100f)}")
                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            if (isTtsInitialized) {
                                // 1. ONLY update the state variable.
                                speechPitch = (speechPitch - 0.1f).coerceAtLeast(0.1f)

                                // 2. Let startPlayback do the work.
                                if (isSpeaking) {
                                    startPlayback(false) // Re-speak the current sentence with the new pitch
                                }

                                // 3. Save the new state.
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    currentSentenceIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)
                            }
                        },
                        enabled = isTtsInitialized && speechPitch > 0.1f,
                    ) {
                        Text("Lower")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (isTtsInitialized) {
                                // 1. ONLY update the state variable.
                                speechPitch = (speechPitch + 0.1f).coerceAtMost(2.0f)

                                // 2. Let startPlayback do the work.
                                if (isSpeaking) {
                                    startPlayback(false) // Re-speak the current sentence with the new pitch
                                }

                                // 3. Save the new state.
                                val stateToSave = BookPlaybackState(
                                    fileName,
                                    currentSentenceIndex,
                                    0,
                                    speechRate,
                                    speechPitch
                                )
                                viewModel.saveBookProgress(stateToSave)
                            }
                        },
                        enabled = isTtsInitialized && speechPitch < 2.0f,
                    ) {
                        Text("Higher")
                    }
                }

                Spacer(Modifier.height(192.dp))

                Button(
                    onClick = {
                        if (isTtsInitialized && tts != null && (isSpeaking || isPaused)) {
                            tts?.stop() // Stops current speech and clears the queue.
                            isSpeaking = false
                            isPaused = false
                            currentSentenceIndex = 0 // Reset currentSentenceIndex to the beginning
                            // //Log.d("TTS_AudioPlayer", "Stopped playback. Sentence index reset to 0 from $oldIndex.")

                            // Persist the change to the beginning
                            if (fileName != null) {
                                // Create a state object representing the beginning of the book
                                val stateToSave = BookPlaybackState(
                                    folderPath = fileName,
                                    chapter = 0, // Reset chapter/sentence to 0
                                    position = 0,
                                    speechRate = speechRate, // Keep the user's preferred rate
                                    speechPitch = speechPitch  // Keep the user's preferred pitch
                                )
                                // Call the correct ViewModel function
                                viewModel.saveBookProgress(stateToSave)
                            }
                        }
                    },
                    enabled = isTtsInitialized && (isSpeaking || isPaused), // Enable if TTS is ready and either speaking or paused
                ) {
                    Text("Top") // Changed from "Stop" to "Top" to better reflect action of returning to beginning
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filePath == null) {
                Text("No file loaded.")
            } else if (bookSentences.isEmpty() && isTtsInitialized) {
                Text("Loading audio content or no speakable content found...")
            } else if (!isTtsInitialized) {
                Text("TTS engine initializing...")
            }
        }
    }
}