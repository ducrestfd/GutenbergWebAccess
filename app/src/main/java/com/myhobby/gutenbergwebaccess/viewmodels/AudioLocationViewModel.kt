package com.myhobby.gutenbergwebaccess.viewmodels

/*
Gutenberg Web Access's raison d'être is to provide simple access to
the Gutenberg Project website of 70,000 plus books to both
sighted and blind users.  It is provided without charge under the
agpl-3.0 license.

    Copyright (C) 2026 Frank D. Ducrest

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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myhobby.gutenbergwebaccess.MainApplication
import com.myhobby.gutenbergwebaccess.data.SettingsRepository
import com.myhobby.gutenbergwebaccess.data.models.AudioLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first


/**
 * A ViewModel responsible for managing the playback state for Text-to-Speech (TTS) books.
 *
 * This [AndroidViewModel] serves as the bridge between the UI (like [TextToSpeechBookReader])
 * and the data layer ([AudioLocationDao] and [SettingsRepository]). Its key responsibilities include:
 *
 * - **Creating and Deleting Book Records**: Adding new book entries to the database with a
 *   default speaking speed derived from user settings, and removing them when necessary.
 * - **State Persistence**: Saving the reader's progress, including the current sentence (chapter),
 *   speech rate, and pitch, to the database.
 * - **State Retrieval**: Asynchronously fetching the last known playback state for a given book.
 * - **Settings Integration**: Exposing the user's globally configured default speaking speed,
 *   allowing new books to start with a familiar setting.
 *
 * @param application The application context, required to instantiate database and repository dependencies.
 */
class AudioLocationViewModel(application: Application) : AndroidViewModel(application) {

    // Instance of the DAO, obtained from the application's database instance.
    val audioLocationDao = MainApplication.audioLocationDatabase.getAudioLocationDao()

    /* A private instance of the [SettingsRepository].
    *
    * This repository is responsible for handling the persistence of global application settings,
    * such as the user's preferred default speaking speed, using Jetpack DataStore.
    * It is initialized with the application context provided to the ViewModel.
    */
    private val settingsRepository = SettingsRepository(application)

    val defaultSpeakingSpeedFlow: Flow<Float> = settingsRepository.defaultSpeakingSpeedFlow

    /**
     * Adds a new audio location record to the database for a given book name.
     *
     * The initial sentence index is set to 0, and the initial speech rate
     * is set to the user's configured default speed.
     * This operation is performed on a background thread.
     *
     * @param name The name of the book file (e.g., "mybook.html") for which to add the audio location.
     */
    fun addAudioLocation(name : String){
        viewModelScope.launch(Dispatchers.IO) {
            // --- THIS IS THE FIX ---
            // 1. Asynchronously get the MOST RECENT value from the settings flow.
            val defaultSpeed = defaultSpeakingSpeedFlow.first()

            // 2. Use this defaultSpeed when creating the new database entry.
            audioLocationDao.addAudioLocation(
                AudioLocation(
                    name = name,
                    sentenceIndex = 0,
                    speechRate = defaultSpeed, // Use the user's default speed
                    speechPitch = 1.0f
                )
            )
        }
    }

    /**
     * Deletes an audio location record from the database by its ID.
     * This operation is performed on a background thread.
     *
     * @param id The unique ID of the [AudioLocation] record to delete.
     */
    fun deleteAudioLocation(id : Int){
        viewModelScope.launch(Dispatchers.IO) {
            audioLocationDao.deleteAudioLocation(id)
        }
    }

    /**
     * Saves the complete playback state (index, rate, pitch) for a book.
     * It finds the existing AudioLocation and updates it. If none exists, it should ideally create one.
     */
    fun saveBookProgress(state: BookPlaybackState) {
        viewModelScope.launch(Dispatchers.IO) {
            // Create an AudioLocation entity from the state object
            val audioLocation = AudioLocation(
                name = state.folderPath,
                sentenceIndex = state.chapter,
                speechRate = state.speechRate,
                speechPitch = state.speechPitch
            )
            // Use an upsert-like logic in the DAO if available, or update if exists.
            // For now, let's assume update is sufficient. We need to ensure the DAO's update method is correct.
            audioLocationDao.updateAudioLocation(
                name = audioLocation.name,
                sentenceIndex = audioLocation.sentenceIndex,
                speechRate = audioLocation.speechRate,
                speechPitch = audioLocation.speechPitch
            )
        }
    }

    /**
     * Asynchronously retrieves the last known playback state for a specific book.
     *
     * @param bookPathOrName The name of the book file to get the progress for.
     * @return A [Deferred] object that will eventually hold the [BookPlaybackState] if found,
     *         or `null` if no record is found.
     */
    fun getBookProgressAsync(bookPathOrName: String): Deferred<BookPlaybackState?> {
        return viewModelScope.async(Dispatchers.IO) {
            val audioLocation = audioLocationDao.getAudioLocation(bookPathOrName)
            // If an AudioLocation is found, map it to our BookPlaybackState data class
            audioLocation?.let {
                BookPlaybackState(
                    folderPath = it.name,
                    chapter = it.sentenceIndex,
                    speechRate = it.speechRate,
                    speechPitch = it.speechPitch
                )
            }
        }
    }

}