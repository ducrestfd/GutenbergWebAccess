package com.myhobby.gutenbergwebaccess.data.databases

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

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myhobby.gutenbergwebaccess.data.models.ScrollLocation



/**
 * The Room database class for storing [ScrollLocation] entities.
 *
 * This abstract class extends [RoomDatabase] and is annotated with `@Database`.
 * It lists the entities contained within the database ([ScrollLocation]) and defines
 * the database version. Room uses this class to generate the necessary
 * implementation code for the database and its DAOs.
 *
 * The database provides access to the Data Access Object (DAO) for [ScrollLocation]
 * through the `getScrollLocationDao()` abstract method.
 *
 * A companion object defines a constant for the database name (`NAME`),
 * which is used when building the database instance (e.g., with `Room.databaseBuilder`).
 *
 * @see ScrollLocation
 * @see ScrollLocationDao
 */
@Database(entities = [ScrollLocation::class], version = 1)
abstract class ScrollLocationDatabase : RoomDatabase() {

    /**
     * Companion object to hold constants related to the database, primarily its name.
     */
    companion object {

        /**
         * The name of the database file. This will be used by Room when creating
         * the actual database file on the device.
         */
        const val NAME = "ScrollLocation_DB"
    }

    /**
     * Abstract method to get the Data Access Object (DAO) for [ScrollLocation] entities.
     *
     * Room will generate the implementation for this method, providing an instance
     * of [ScrollLocationDao] that can be used to interact with the `ScrollLocation` table.
     *
     * @return An instance of [ScrollLocationDao] for database operations on scroll locations.
     */
    abstract fun getScrollLocationDao() : ScrollLocationDao
}