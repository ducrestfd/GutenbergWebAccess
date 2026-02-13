// In app/src/main/java/com/myhobby/gutenbergwebaccess/data/SettingsRepository.kt

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
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a DataStore instance for app settings, tied to the application's context.
// It's important to use a different name ("app_settings") to avoid conflicts
// with your existing "playback_settings" DataStore.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")



/**
 * Manages the persistence of application-wide user settings using Jetpack DataStore.
 *
 * This class is responsible for saving and retrieving simple user preferences, such as
 * the default speaking speed. This abstracts the data persistence logic away from
 * ViewModels and UI components.
 *
 * @property context The application context, used to initialize and access the DataStore instance.
 */
class SettingsRepository(private val context: Context) {

    // Define a key for the default speaking speed. We use floatPreferencesKey for a Float value.
    private val DEFAULT_SPEAKING_SPEED_KEY = floatPreferencesKey("default_speaking_speed")
    private val DEFAULT_FONT_SCALE_KEY = floatPreferencesKey("default_font_scale")


    /**
     * A Flow that emits the user's preferred default speaking speed.
     * If the value has never been set, it will default to 1.0f.
     *
     * UI components can collect this Flow to reactively update when the setting changes.
     */
    val defaultSpeakingSpeedFlow: Flow<Float> = context.settingsDataStore.data
        .map { preferences ->
            // Retrieve the float value. If it's not set, return the default value of 1.0f.
            preferences[DEFAULT_SPEAKING_SPEED_KEY] ?: 1.0f
        }

    val defaultFontScaleFlow: Flow<Float> = context.settingsDataStore.data
        .map { preferences ->
            preferences[DEFAULT_FONT_SCALE_KEY] ?: 1.0f
        }

    /**
     * Saves the user's selected default speaking speed to the DataStore.
     * This is a suspend function as DataStore operations are asynchronous.
     *
     * @param speed The speaking speed to save (e.g., 1.0f, 1.2f).
     */
    suspend fun saveDefaultSpeakingSpeed(speed: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[DEFAULT_SPEAKING_SPEED_KEY] = speed
        }
    }

    suspend fun saveDefaultFontScale(scale: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[DEFAULT_FONT_SCALE_KEY] = scale
        }
    }
}
