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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.myhobby.gutenbergwebaccess.data.models.ScrollLocation


/**
 * Data Access Object (DAO) for the [ScrollLocation] entity.
 *
 * This interface defines the database interactions for storing and retrieving
 * scroll locations (Y-coordinates) for books viewed in an HTML reader.
 * Room generates the necessary code to implement these methods.
 */
@Dao
interface ScrollLocationDao {


    /**
     * Retrieves all [ScrollLocation] records from the database.
     *
     * This method returns a [LiveData] list, which allows observing changes
     * to the data and updating the UI reactively.
     *
     * @return A [LiveData] list of all [ScrollLocation] objects.
     */
    @Query("SELECT * FROM ScrollLocation")
    fun getAllScrollLocation() : LiveData<List<ScrollLocation>>

    /**
     * Inserts a new [ScrollLocation] record into the database.
     *
     * If a [ScrollLocation] with the same primary key (if `id` were not auto-generated
     * and a conflict occurred) already exists, the behavior would depend on the
     * `OnConflictStrategy` (not specified here, defaults to ABORT).
     *
     * @param scrollLocation The [ScrollLocation] object to be inserted.
     */
    @Insert
    fun addScrollLocation(scrollLocation : ScrollLocation)

    /**
     * Deletes a [ScrollLocation] record from the database based on its ID.
     *
     * @param id The primary key (ID) of the [ScrollLocation] record to delete.
     */
    @Query("Delete FROM ScrollLocation where id = :id")
    fun deleteScrollLocation(id : Int)

    /**
     * Updates an existing [ScrollLocation] record in the database.
     *
     * The record to update is identified by its primary key (implicitly, as Room
     * uses the primary key from the passed `scrollLocation` object to find the
     * existing record).
     *
     * @param scrollLocation The [ScrollLocation] object containing the updated data.
     */
    @Update
    fun updateScrollLocation(scrollLocation : ScrollLocation)

    /**
     * Retrieves a specific [ScrollLocation] record from the database by its `name`.
     *
     * This method is expected to return a single [ScrollLocation] object. If no
     * record with the given name exists, it will return `null`.
     *
     * @param name The name of the book file to retrieve the [ScrollLocation] for.
     * @return The [ScrollLocation] object if found, otherwise `null`.
     */
    @Query("SELECT * FROM ScrollLocation where name = :name")
    fun getScrollLocation(name : String) : ScrollLocation
}