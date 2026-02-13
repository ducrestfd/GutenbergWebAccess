package com.myhobby.gutenbergwebaccess

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

/**
 * Collects book links from a specific section URL on the Project Gutenberg website, supporting pagination.
 *
 * This function connects to a given URL (which typically represents a bookshelf or a search result page
 * on Project Gutenberg) and scrapes links to individual books. It specifically looks for `li` elements
 * with the class `booklink`, then extracts the book's title from a `span.title` and its href
 * from an `a[href]` tag within that list item.
 *
 * It supports pagination by appending a `start_index` query parameter to the URL if `startingIndex`
 * is greater than 1. This allows fetching results from subsequent pages.
 *
 * @param section A string representing the section or category name. This is used in the [Link]
 *                objects created but not directly in the scraping logic for this function.
 * @param href The base URL of the section or page to scrape book links from.
 * @param startingIndex The starting index for fetching books, used for pagination.
 *                      If 1, the base `href` is used. Otherwise, `&start_index=startingIndex`
 *                      is appended to the `href`.
 * @return A [List] of [Link] objects, where each object represents a book with its title and URL.
 *         Returns an empty list if an exception occurs during scraping or if no book links are found.
 */
fun collectSectionHrefs(section: String, href: String, startingIndex: Int): List<Link> {
    val results = mutableListOf<Link>()

    val url: String
    if (startingIndex == 1)
        url = href
    else
        url = "$href&start_index=$startingIndex"

    return try {

        // Fetch and parse the document.
        // A user agent is specified for politeness.
        // A timeout is set to prevent indefinite blocking.
        val doc = org.jsoup.Jsoup
            .connect(url)
            .userAgent("Mozilla/5.0 (compatible; KotlinScraper/1.0)")
            .timeout(20_000)
            .get()

        // Select each list item with class "booklink".
        for (bookList in doc.select("li.booklink")) {

            val a1 = bookList.selectFirst("a[href]")
            val bookHref = a1!!.absUrl("href").ifEmpty { a1.attr("href") }.trim()

            val title = "${bookList.selectFirst("span.title")?.text()?.trim().orEmpty()}\n\t\t${bookList.selectFirst("span.subtitle")?.text()?.trim().orEmpty()}"


            //val section = bookList.selectFirst("span.author")?.text()?.trim().orEmpty()
            //Log.d("collectSectionHrefs", "title: $title, href: $bookHref, section: $section")



            results.add(Link(section = section, href = bookHref, label = title))
        }

        results
    } catch (e: Exception) {
        return results
    }
}