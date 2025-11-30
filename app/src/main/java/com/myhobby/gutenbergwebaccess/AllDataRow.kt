package com.myhobby.gutenbergwebaccess

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

/**
 * Represents a data row containing comprehensive information about a single book.
 *
 * This data class is typically used to hold all relevant metadata for a book,
 * such as its unique identifier, title, author, and subject category.
 *
 * @property textNumber The unique identifier for the book, often corresponding to its
 *                      number on Project Gutenberg or a similar repository.
 * @property title The full title of the book.
 * @property author The name(s) of the author(s) of the book.
 * @property subject The primary subject category or genre of the book.
 */
data class AllDataRow (
    val textNumber: Int,
    val title: String,
    val author: String,
    val subject: String
)


/**
 * Represents a condensed data row, typically used for search indexing or displaying
 * short lists where only a primary piece of information and its identifier are needed.
 *
 * This data class includes a `textNumber` to link back to a more comprehensive
 * [AllDataRow] and a `holder` string which can store various types of information
 * like a title, author name, or subject term, depending on the context.
 *
 * It implements [Comparable] to define a custom comparison logic, specifically
 * checking if the `holder` string of one `ShortRow` contains the `holder` string
 * of another (case-insensitive). This is primarily used for custom sorting or
 * filtering operations rather than a standard lexicographical comparison.
 *
 * @property textNumber The unique identifier for the book or item, allowing linkage
 *                      to more detailed data (e.g., in [AllDataRow]).
 * @property holder A string field that holds the primary piece of information for this
 *                  short row (e.g., a book title, an author's name, a subject term).
 */
data class ShortRow (
    val textNumber: Int,
    val holder: String
) : Comparable<ShortRow> {

    /**
     * Compares this [ShortRow] object with the specified [ShortRow] object for order.
     *
     * The comparison is based on whether the `holder` string of this instance
     * contains the `holder` string of the `other` instance, ignoring case.
     *
     * @param other The [ShortRow] object to be compared.
     * @return 0 if this `holder` contains `other.holder` (case-insensitive).
     *         -1 otherwise (indicating this instance is considered "less than" or "not matching"
     *         in the context of this specific comparison, though it doesn't represent
     *         a standard sorting order). This comparison logic is specific and might be
     *         used for filtering or specific matching scenarios rather than general sorting.
     */
    override fun compareTo(other: ShortRow): Int {
        if (holder.contains(other.holder, ignoreCase = true))
            return 0
        return -1
    }
}