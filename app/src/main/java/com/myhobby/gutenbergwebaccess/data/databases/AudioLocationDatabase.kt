package com.myhobby.gutenbergwebaccess.data.databases

/*
Gutenberg Access's raison d'être is to provide simple access to
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

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myhobby.gutenbergwebaccess.data.models.AudioLocation


/**
 * The Room database class for storing [AudioLocation] entities.
 *
 * This abstract class extends [RoomDatabase] and is annotated with `@Database`.
 * It lists the entities contained within the database and the database version.
 * Room will use this class to generate the necessary implementation code.
 *
 * The database provides access to the Data Access Object (DAO) for [AudioLocation]
 * through the `getAudioLocationDao()` abstract method.
 *
 * A companion object defines a constant for the database name, which is used
 * when building the database instance.
 *
 * @see AudioLocation
 * @see AudioLocationDao
 */
@Database(entities = [AudioLocation::class], version = 2)
abstract class AudioLocationDatabase : RoomDatabase() {

    /**
     * Companion object to hold constants related to the database, such as its name.
     */
    companion object {

        /**
         * The name of the database file.
         */
        const val NAME = "AudioLocation_DB"
    }

    /**
     * Abstract method to get the Data Access Object (DAO) for [AudioLocation] entities.
     *
     * Room will generate the implementation for this method.
     *
     * @return An instance of [AudioLocationDao] to interact with the AudioLocation table.
     */
    abstract fun getAudioLocationDao() : AudioLocationDao
}