package com.myhobby.gutenbergwebaccess.data.models

/*
Gutenberg Access's raison d'être is to provide simple access to
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

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a data entity for storing a user's progress and settings for a specific book's audio playback.
 *
 * This class is used with the Room persistence library to save and retrieve the user's state,
 * including the last sentence they were listening to and their preferred speech rate and pitch.
 *
 * @property id The unique auto-generated primary key for the database entry.
 * @property name The name of the book file (e.g., "TheAdventuresofSherlockHolmes.html") that this audio state belongs to.
 *                This acts as a unique identifier for the book's progress.
 * @property sentenceIndex The index of the last sentence that was being read by the Text-to-Speech engine.
 *                         This allows the user to resume listening from where they left off.
 * @property speechRate The playback speed for the Text-to-Speech engine. A value of 1.0f is normal speed.
 * @property speechPitch The pitch for the Text-to-Speech engine. A value of 1.0f is normal pitch.
 */
@Entity
data class AudioLocation (
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String,
    var sentenceIndex: Int,
    var speechRate: Float = 1.0f,
    var speechPitch: Float = 1.0f
)