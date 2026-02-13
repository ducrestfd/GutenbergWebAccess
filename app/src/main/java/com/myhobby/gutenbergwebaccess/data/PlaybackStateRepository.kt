package com.myhobby.gutenbergwebaccess.data

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


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myhobby.gutenbergwebaccess.viewmodels.BookPlaybackState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Create a DataStore instance, tied to the application's context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_settings")



/**
 * Manages the persistence of audiobook playback states using Jetpack DataStore.
 *
 * This class is responsible for saving and retrieving the playback progress (chapter and position)
 * for all audiobooks, as well as tracking the most recently played book. It uses [Gson] to
 * serialize the map of playback states into a JSON string for storage. This abstracts the
 * data persistence logic away from the ViewModels.
 *
 * @property context The application context, used to initialize and access the [DataStore] instance.
 */
class PlaybackStateRepository(private val context: Context) {

    private val gson = Gson()

    // Define keys for what we want to store
    private val PLAYBACK_STATES_KEY = stringPreferencesKey("playback_states_map")
    private val MOST_RECENT_BOOK_KEY = stringPreferencesKey("most_recent_book_path")

    // --- Save Functions ---

    /**
     * Saves the entire map of playback states as a JSON string.
     */
    suspend fun savePlaybackStates(states: Map<String, BookPlaybackState>) {
        val jsonString = gson.toJson(states)
        context.dataStore.edit { preferences ->
            preferences[PLAYBACK_STATES_KEY] = jsonString
        }
    }

    /**
     * Saves the path of the most recently played book.
     */
    suspend fun saveMostRecentBook(folderPath: String?) {
        context.dataStore.edit { preferences ->
            if (folderPath == null) {
                preferences.remove(MOST_RECENT_BOOK_KEY)
            } else {
                preferences[MOST_RECENT_BOOK_KEY] = folderPath
            }
        }
    }

    // --- Load Functions ---

    /**
     * Loads the map of playback states from a JSON string.
     * Returns an empty map if no data is found.
     */
    suspend fun loadPlaybackStates(): Map<String, BookPlaybackState> {
        val preferences = context.dataStore.data.first()
        val jsonString = preferences[PLAYBACK_STATES_KEY] ?: return emptyMap()

        // Define the type for Gson to deserialize into
        val type = object : TypeToken<Map<String, BookPlaybackState>>() {}.type
        return gson.fromJson(jsonString, type) ?: emptyMap()
    }

    /**
     * Creates a Flow to observe the most recently played book path.
     */
    val mostRecentBookFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MOST_RECENT_BOOK_KEY]
    }
}
