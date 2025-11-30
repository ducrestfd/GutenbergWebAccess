package com.myhobby.gutenbergwebaccess.viewmodels

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myhobby.gutenbergwebaccess.MainApplication
import com.myhobby.gutenbergwebaccess.data.models.ScrollLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async


/**
 * ViewModel for managing scroll locations ([ScrollLocation]) for books.
 *
 * This ViewModel interacts with the [ScrollLocationDao] to perform CRUD (Create, Read, Update, Delete)
 * operations on scroll location data stored in a Room database. It provides functions to:
 * - Get a LiveData list of all saved scroll locations.
 * - Add a new scroll location for a book (initially with Y-coordinate 0).
 * - Delete a scroll location by its ID.
 * - Update an existing scroll location.
 * - Asynchronously retrieve a specific scroll location by book name.
 *
 * All database operations are performed on a background thread using `viewModelScope`
 * and `Dispatchers.IO`.
 */
class ScrollLocationViewModel : ViewModel() {

    // Instance of the DAO, obtained from the application's database instance.
    val scrollLocationDao = MainApplication.scrollLocationDatabase.getScrollLocationDao()

    /**
     * A [LiveData] list of all [ScrollLocation] objects stored in the database.
     * This can be observed by UI components to react to changes in the data.
     */
    val scrollLocationList : LiveData<List<ScrollLocation>> = scrollLocationDao.getAllScrollLocation()

    /**
     * Adds a new scroll location record to the database for a given book name.
     * The initial scroll location (Y-coordinate) is set to 0.
     * This operation is performed on a background thread.
     *
     * @param name The name of the book file (e.g., "mybook.html") for which to add the scroll location.
     */
    fun addScrollLocation(name : String){
        viewModelScope.launch(Dispatchers.IO) {
            scrollLocationDao.addScrollLocation(ScrollLocation(name = name, locy = 0))
        }
    }

    /**
     * Deletes a scroll location record from the database by its ID.
     * This operation is performed on a background thread.
     *
     * @param id The unique ID of the [ScrollLocation] record to delete.
     */
    fun deleteScrollLocation(id : Int){
        viewModelScope.launch(Dispatchers.IO) {
            scrollLocationDao.deleteScrollLocation(id)
        }
    }

    /**
     * Updates an existing scroll location record in the database.
     * This operation is performed on a background thread.
     *
     * @param scrollLocation The [ScrollLocation] object containing the updated data.
     *                       The existing record is identified by its primary key.
     */
    fun updateScrollLocation(scrollLocation : ScrollLocation) {
        viewModelScope.launch(Dispatchers.IO) {
            scrollLocationDao.updateScrollLocation(scrollLocation)
        }
    }

    /**
     * Asynchronously retrieves a specific [ScrollLocation] object from the database by book name.
     * This operation is performed on a background thread.
     *
     * @param name The name of the book file to retrieve the [ScrollLocation] for.
     * @return A [Deferred] object that will eventually hold the [ScrollLocation] if found, or `null` otherwise.
     *         Use `.await()` in a coroutine to get the result.
     */
    fun getScrollLocationAsync(name: String): Deferred<ScrollLocation?> {
        return viewModelScope.async(Dispatchers.IO) { // async returns a Deferred
            scrollLocationDao.getScrollLocation(name)
        }
    }


}