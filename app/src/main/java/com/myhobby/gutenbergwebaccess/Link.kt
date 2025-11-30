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
 * Represents a hyperlink with associated section and label information.
 *
 * This data class is used to store details about links, often scraped from a webpage
 * or representing navigation items within the application. It typically includes the
 * URL itself, a displayable label for the link, and the section or category
 * it belongs to.
 *
 * @property section A [String] indicating the section or category of the link.
 *                   For example, this could be "Literature", "History", or a
 *                   bookshelf name if scraping from Project Gutenberg.
 * @property href The [String] URL (Hypertext Reference) of the link.
 * @property label The [String] display text or title for the link, which is
 *                 what the user typically sees and interacts with.
 */
data class Link(val section: String, val href: String, val label: String)