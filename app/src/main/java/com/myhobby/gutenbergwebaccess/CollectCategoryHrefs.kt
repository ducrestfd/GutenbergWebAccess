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
 * Collects category hyperlinks from the Project Gutenberg categories page.
 *
 * This function scrapes the "https://www.gutenberg.org/ebooks/categories" URL
 * to extract links to various book categories (bookshelves). It parses the HTML
 * structure looking for `div.book-list` elements, then extracts the section title
 * from `h2` tags and individual category links from `a[href]` tags within `ul` lists.
 *
 * Each extracted link is stored as a [Link] object, containing the section title,
 * the absolute URL of the category, and the category's display text.
 *
 * It includes basic logging for the start of the operation and in case of exceptions.
 *
 * @return A [List] of [Link] objects, where each object represents a category hyperlink.
 *         Returns an empty list if an exception occurs during scraping or if no links are found.
 */
fun collectCategoryHrefs(): List<Link> {

    // Log.e("collectCategoryHrefs", "starting collectCategoryHrefs")

    val results = mutableListOf<Link>()

    val url = "https://www.gutenberg.org/ebooks/categories"

    return try {

        // Log.e("collectCategoryHrefs", "starting collectCategoryHrefs try")


        // Fetch and parse the document.
        // A user agent is specified for politeness.
        // A timeout is set to prevent indefinite blocking.
        val doc = org.jsoup.Jsoup
            .connect(url)
            .userAgent("Mozilla/5.0 (compatible; KotlinScraper/1.0)")
            .timeout(20_000)
            .get()

        // Log.e("collectCategoryHrefs", "starting collectCategoryHrefs try")

        // Select each "book-list" division which groups categories under a section.
        for (bookList in doc.select("div.book-list")) {

            // Log.e("collectCategoryHrefs", "in collectCategoryHrefs outer for")

            // Get the section title from the H2 tag.
            val h2 = bookList.selectFirst("h2")
            val section = h2!!.text().trim()

            // Find the unordered list (ul) containing the category links.
            val ul = bookList.selectFirst("ul")

            // Iterate over each anchor tag (a) with an href attribute within the ul.
            for (a in ul!!.select("a[href]")) {

                // Log.e("collectCategoryHrefs", "in collectCategoryHrefs inner for")
                val text = a.text().trim()

                // absUrl("href") resolves relative URLs against the base java . net . URL of the fetched document.
                // Fallback to attr("href") if absUrl is empty (though less likely for well-formed sites).
                val href = a.absUrl("href").ifEmpty { a.attr("href") }.trim()
                if (text.isNotEmpty() && href.isNotEmpty()) {
                    results.add(Link(section, href = href, label = text))
                }
            }
        }

        results
    } catch (e: Exception) {
        // Log.e("collectCategoryHrefs", "Exception: " + e.stackTraceToString())

        return results
    }
}
