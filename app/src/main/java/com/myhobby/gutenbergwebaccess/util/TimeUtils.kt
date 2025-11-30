package com.myhobby.gutenbergwebaccess.util

/*
Gutenberg Web Access's raison d'être is to provide simple access to
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


import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import org.jsoup.Jsoup

/**
 * Converts a duration in milliseconds into a formatted time string (HH:MM:SS or MM:SS).
 *
 * @param millis The duration in milliseconds.
 * @return A string formatted as HH:MM:SS if the duration is an hour or more,
 *         otherwise formatted as MM:SS.
 */
fun formatTimeMillis(millis: Int): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.toLong())
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        // Format as HH:MM:SS
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        // Format as MM:SS
        String.format("%02d:%02d", minutes, seconds)
    }
}


/**
 * Checks if a given URL points to an existing resource by making a HEAD request.
 * Should be called from a coroutine.
 *
 * @param urlString The URL to check.
 * @return `true` if the URL returns an HTTP 200 OK status, `false` otherwise.
 */
suspend fun urlExists(urlString: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD" // Use HEAD to be efficient, we don't need the body
            connection.connectTimeout = 3000 // 3-second timeout
            connection.readTimeout = 3000   // 3-second timeout

            val responseCode = connection.responseCode
            connection.disconnect()

            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            Log.e("urlExists", "Error checking URL: $urlString", e)
            false // An exception means we couldn't connect, so it doesn't "exist" for our app
        }
    }
}


/**
 * Fetches an HTML page and extracts the content of a specific meta tag.
 *
 * @param url The URL of the webpage to scrape.
 * @param metaName The value of the 'name' attribute of the meta tag to find (e.g., "title", "author").
 * @return The content of the meta tag as a String, or null if not found or an error occurs.
 */
suspend fun getMetaTagContent(url: String, metaName: String): String? {
    // Perform network and parsing on the IO dispatcher
    return withContext(Dispatchers.IO) {
        try {
            // 1. Connect to the URL and get the document
            val doc = Jsoup.connect(url).get()

            // 2. Select the meta tag using a CSS query
            // This query finds a <meta> tag with a name attribute equal to metaName
            val metaTag = doc.select("meta[name=$metaName]").first()

            // 3. Extract the 'content' attribute from the found tag
            metaTag?.attr("content")
        } catch (e: Exception) {
            // Log the error for debugging
            Log.e("GetMetaTag", "Failed to get meta tag '$metaName' from $url", e)
            null // Return null if any error occurs (e.g., network error, tag not found)
        }
    }
}

