package com.myhobby.gutenbergwebaccess.viewmodels

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

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myhobby.gutenbergwebaccess.data.PlaybackStateRepository // Import the new repository
import com.myhobby.gutenbergwebaccess.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Represents the persisted playback state and settings for a single audiobook.
 *
 * This data class holds all the necessary information to resume playback for a specific
 * book, including the chapter, the position within that chapter, and the user's preferred
 * speech rate and pitch settings. An instance of this class is typically associated with
 * a unique book folder.
 *
 * @property folderPath The unique identifier for the book, which corresponds to its folder
 *           name in the device's storage. This is used as the key to look up the book's state.
 * @property chapter The last played chapter number (1-indexed). This allows resuming from
 *           the correct audio file. Defaults to `1`.
 * @property position The last known playback position within the chapter, specified in
 *           milliseconds. This allows resuming from the exact moment the user left off.
 *           Defaults to `0`.
 * @property speechRate The user-defined speech rate for the Text-to-Speech engine. A value of
 *           `1.0f` represents normal speed. Defaults to `1.0f`.
 * @property speechPitch The user-defined speech pitch for the Text-to-Speech engine. A value
 *           of `1.0f` represents normal pitch. Defaults to `1.0f`.
 */
data class BookPlaybackState(
    val folderPath: String,
    var chapter: Int = 1,
    var position: Int = 0,
    var speechRate: Float = 1.0f,
    var speechPitch: Float = 1.0f
)

/**
 * Manages the playback state for all OGG audiobooks across the application.
 *
 * This [AndroidViewModel] is responsible for:
 * - Loading saved playback states from persistent storage via [PlaybackStateRepository] on startup.
 * - Holding the current playback state (chapter and position) for all books in memory.
 * - Persisting any updates to the playback state to ensure progress is not lost.
 * - Removing playback state data when a book is deleted.
 *
 * @param application The application context, required by [AndroidViewModel] to instantiate the repository.
 */
class OggPlaybackViewModel(application: Application) : AndroidViewModel(application) {
    // The repository handles all saving and loading
    private val repository = PlaybackStateRepository(application)
    private val settingRepository = SettingsRepository(application)


    private val _playbackStates = mutableStateMapOf<String, BookPlaybackState>()
    val playbackStates: Map<String, BookPlaybackState> = _playbackStates

    var mostRecentBookFolderPath: String? = null

    /**
     * A Flow that emits the user's preferred default speaking speed.
     * The UI can collect this to reactively update when the setting changes.
     * It defaults to 1.0f if no value has been set.
     */
    val defaultSpeakingSpeedFlow = settingRepository.defaultSpeakingSpeedFlow

    /**
     * Saves the new default speaking speed to persistent storage via the SettingsRepository.
     *
     * @param speed The new speed value to save (e.g., 1.0f, 1.5f).
     */
    fun saveDefaultSpeakingSpeed(speed: Float) {
        viewModelScope.launch {
            settingRepository.saveDefaultSpeakingSpeed(speed)
        }
    }

    /**
     * Initializes the ViewModel when it is first created.
     *
     * This block serves as the entry point for the ViewModel's setup logic. Its primary
     * responsibility is to trigger the loading of all persisted playback states from
     * storage by calling [loadInitialState]. This ensures that the ViewModel is populated
     * with the user's last known progress as soon as it comes into scope.
     */
    init {
        // When the ViewModel is created, load the persisted state
        loadInitialState()
    }

    /**
     * Asynchronously loads all persisted audiobook states from the [PlaybackStateRepository].
     *
     * This private function is called once during the ViewModel's initialization. It launches
     * a coroutine in the [viewModelScope] to perform the following disk I/O operations
     * without blocking the main thread:
     * 1.  Fetches the map of all saved `BookPlaybackState` objects and populates the
     *     in-memory `_playbackStates` map.
     * 2.  Fetches the folder path of the most recently accessed book and updates the
     *     `mostRecentBookFolderPath` property.
     */
    private fun loadInitialState() {
        viewModelScope.launch {
            // Load the saved map and the most recent book path
            val savedStates = repository.loadPlaybackStates()
            _playbackStates.putAll(savedStates)

            mostRecentBookFolderPath = repository.mostRecentBookFlow.first()
        }
    }

    /**
     * Updates the playback state for a given book and saves it to DataStore.
     */
    fun updatePlaybackState(folderPath: String, chapter: Int, position: Int) {
        val state = _playbackStates.getOrPut(folderPath) {
            BookPlaybackState(folderPath = folderPath)
        }
        state.chapter = chapter
        state.position = position
        mostRecentBookFolderPath = folderPath

        // Launch a coroutine to save the updated state
        viewModelScope.launch {
            repository.savePlaybackStates(_playbackStates)
            repository.saveMostRecentBook(mostRecentBookFolderPath)
        }
    }

    /**
     * Updates only the speech rate for a given book and saves it.
     * This is more efficient than updating the entire state when only the speed changes.
     */
    fun updatePlaybackSpeed(folderPath: String, speed: Float) {
        val state = _playbackStates.getOrPut(folderPath) {
            BookPlaybackState(folderPath = folderPath)
        }
        state.speechRate = speed // Update only the rate

        // Launch a coroutine to save the updated map
        viewModelScope.launch {
            repository.savePlaybackStates(_playbackStates)
        }
    }

    /**
     * Retrieves the playback state for a specific book.
     */
    fun getPlaybackStateForBook(folderPath: String): BookPlaybackState? {
        return _playbackStates[folderPath]
    }

    /**
     * Call this function when a book is deleted to remove its playback state.
     */
    fun deletePlaybackState(folderPath: String) {
        _playbackStates.remove(folderPath)
        if (mostRecentBookFolderPath == folderPath) {
            mostRecentBookFolderPath = null
        }
        // Save the changes to disk
        viewModelScope.launch {
            repository.savePlaybackStates(_playbackStates)
            repository.saveMostRecentBook(mostRecentBookFolderPath)
        }
    }
}
