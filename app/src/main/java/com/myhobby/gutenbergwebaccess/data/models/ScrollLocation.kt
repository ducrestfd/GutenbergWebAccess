package com.myhobby.gutenbergwebaccess.data.models

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


import androidx.room.Entity
import androidx.room.PrimaryKey



/**
 * Represents a data entity for storing the last known scroll location (Y-coordinate) for a book.
 *
 * This class is used with the Room persistence library to save and retrieve the user's
 * vertical scroll position within a book when viewed in an HTML reader (e.g., a WebView).
 * This allows the application to restore the user's reading position when they reopen a book.
 *
 * @property id The unique auto-generated primary key for the database entry.
 *              This is typically managed by Room and does not need to be set manually.
 * @property name The name of the book file (e.g., "mybook.html") for which the scroll location is stored.
 *                This acts as a unique identifier for the book's scroll progress.
 * @property locy An integer representing the vertical scroll position (Y-coordinate) in pixels.
 *                This value indicates how far down the user had scrolled in the book content.
 */
@Entity
data class ScrollLocation(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String,
    var locy: Int
)