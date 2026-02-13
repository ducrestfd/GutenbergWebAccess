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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * A [ViewModel] designed to manage the state for paginated search results from the Gutenberg website.
 *
 * This ViewModel's primary responsibility is to hold and manage the `currentIndex`, which
 * represents the starting item number for a paged query (e.g., page 1 starts at index 1,
 * page 2 starts at index 26, etc.). By retaining this state, it ensures that the user's
 * position within a list of search results is preserved across configuration changes
 * (like screen rotation) and navigation events.
 *
 * It is used by screens like `DirectResults` and `BookListing` to fetch the correct
 * page of results from the website.
 */
class DirectResultsViewModel : ViewModel() {
    // Hold the current index state here. It will survive navigation.
    var currentIndex by mutableStateOf(1)
        private set // Allow external read but only internal write

    fun updateCurrentIndex(newIndex: Int) {
        currentIndex = newIndex
    }

    // A function to reset the index when starting a new search
    fun resetIndex() {
        currentIndex = 1
    }
}
