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


import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.myhobby.gutenbergwebaccess.data.models.AudioLocation


/**
 * Data Access Object (DAO) for the [AudioLocation] entity.
 *
 * This interface defines the database interactions for storing and retrieving
 * audio playback progress (sentence index) for books. Room generates the
 * necessary code to implement these methods.
 */
@Dao
interface AudioLocationDao {

    /**
     * Retrieves all [AudioLocation] records from the database.
     *
     * This method returns a [LiveData] list, which allows observing changes
     * to the data and updating the UI reactively.
     *
     * @return A [LiveData] list of all [AudioLocation] objects.
     */
    @Query("SELECT * FROM AudioLocation")
    fun getAllAudioLocation() : LiveData<List<AudioLocation>>

    /**
     * Inserts a new [AudioLocation] record into the database.
     *
     * If an [AudioLocation] with the same primary key (if `id` were not auto-generated
     * and a conflict occurred) already exists, the behavior would depend on the
     * `OnConflictStrategy` (not specified here, defaults to ABORT).
     *
     * @param audioLocation The [AudioLocation] object to be inserted.
     */
    @Insert
    fun addAudioLocation(audioLocation : AudioLocation)

    /**
     * Deletes an [AudioLocation] record from the database based on its ID.
     *
     * @param id The primary key (ID) of the [AudioLocation] record to delete.
     */
    @Query("Delete FROM AudioLocation where id = :id")
    fun deleteAudioLocation(id : Int)

    /**
     * Updates the playback state (sentenceIndex, speechRate, and speechPitch) of an
     * existing [AudioLocation] record.
     *
     * The record to update is identified by its `name` (book file name).
     *
     * @param name The name of the book file whose audio location is to be updated.
     * @param sentenceIndex The new sentence index to save for the book.
     * @param speechRate The new speech rate to save for the book.
     * @param speechPitch The new speech pitch to save for the book.
     */
    @Query("UPDATE AudioLocation SET sentenceIndex = :sentenceIndex, speechRate = :speechRate, speechPitch = :speechPitch WHERE name = :name")
    fun updateAudioLocation(name: String, sentenceIndex: Int, speechRate: Float, speechPitch: Float)


    /**
     * Retrieves the `sentenceIndex` for a specific [AudioLocation] record by its `name`.
     *
     * This method is designed to fetch only the sentence index directly.
     * If no record with the given name exists, the behavior for primitive return types
     * (like Int) can be problematic if the query returns no rows.
     * It's often safer to return a nullable type (Int?) or ensure the query always returns a value (e.g., with COALESCE).
     *
     * @param name The name of the book file to retrieve the sentence index for.
     * @return The sentence index as an [Int] if found. Consider making this nullable (Int?)
     *         if a book might not have an entry.
     */
    @Query("SELECT * FROM audioLocation where name = :name")
    fun getAudioLocation(name : String) : AudioLocation

    /**
     * Retrieves the `sentenceIndex` for a specific [AudioLocation] record by its `name`.
     *
     * This method is designed to fetch only the sentence index directly.
     * If no record with the given name exists, the behavior for primitive return types
     * (like Int) can be problematic if the query returns no rows.
     * It's often safer to return a nullable type (Int?) or ensure the query always returns a value (e.g., with COALESCE).
     *
     * @param name The name of the book file to retrieve the sentence index for.
     * @return The sentence index as an [Int] if found. Consider making this nullable (Int?)
     *         if a book might not have an entry.
     */
    @Query("SELECT sentenceIndex FROM audioLocation where name = :name")
    fun getAudioLocationByPath(name : String) : Int


}