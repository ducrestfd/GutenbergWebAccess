package com.myhobby.gutenbergwebaccess.screens

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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.myhobby.gutenbergwebaccess.Link
import com.myhobby.gutenbergwebaccess.collectSectionHrefs
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch
import java.net.URLEncoder
import com.myhobby.gutenbergwebaccess.util.urlExists
import com.myhobby.gutenbergwebaccess.viewmodels.DirectResultsViewModel


/**
 * A Composable screen that displays a list of books fetched from the Gutenberg Project website.
 *
 * This screen allows users to browse books based on different criteria like popularity,
 * release date, or alphabetical order within a specific bookshelf. It supports pagination
 * with "Next" and "Previous" buttons to load more books. Each book in the list is
 * a button that navigates to a screen displaying details or options for that chosen book.
 *
 * The screen shows a loading indicator while fetching data and an error message if
 * the data fetching fails.
 *
 * @param navController The [NavController] used for navigating to other screens,
 *                      such as the home screen or the chosen book screen.
 * @param typeOfBrowse A string indicating the sorting order for the books.
 *                     Expected values include "Popularity", "Release Date", or an
 *                     alphabetic character (e.g., "a", "b").
 * @param label A string representing the label or title for the current browsing context,
 *              which is URL-decoded and displayed on the screen.
 * @param shelfNumber A string representing the bookshelf number on the Gutenberg Project
 *                    website from which to fetch the books.
 * @param initialStartingIndex An integer representing the starting index for fetching
 *                             books, used for pagination. Defaults to 1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListing(
    navController: NavController,
    viewModel: DirectResultsViewModel,
    typeOfBrowse: String?,
    label: String?,
    shelfNumber: String?,
    initialStartingIndex: Int = 1
) {

    val currentIndex = viewModel.currentIndex

    var isLoading by remember { mutableStateOf(true) } // Optional: for loading indicator
    var error by remember { mutableStateOf<String?>(null) } // Optional: for error message
    var bookLinks by remember { mutableStateOf<List<Link>>(emptyList()) }


    /**
     * A [LaunchedEffect] that triggers the fetching of a paginated list of books from
     * the Project Gutenberg website.
     *
     * This effect is keyed to the `currentIndex` from the [DirectResultsViewModel].
     * It automatically re-executes whenever the `currentIndex` changes, allowing for
     * seamless pagination as the user clicks "Next" or "Previous".
     *
     * Upon execution, it performs the following steps:
     * 1.  Sets `isLoading` to `true` to display a [CircularProgressIndicator].
     * 2.  Constructs the appropriate
    search URL based on the `typeOfBrowse` (e.g., "Popularity",
     *     "Release Date") and the `shelfNumber`.
     * 3.  In an `IO` coroutine context, it calls [collectSectionHrefs] to perform the network
     *     request and scrape the resulting HTML for book links.
     * 4.  On success, it updates the `bookLinks`
    state, which populates the `LazyColumn`.
     * 5.  In a `finally` block, it always sets `isLoading` back to `false` to hide the
     *     loading indicator, even if a network error occurs.
     */
    LaunchedEffect(currentIndex) { // Use Unit if it only needs to run once
        isLoading = true
        error = null

        val url: String
        if (typeOfBrowse == "Popularity")
            url =
                "https://www.gutenberg.org/ebooks/bookshelf/" + shelfNumber + "?sort_order=" + "downloads"
        else if (typeOfBrowse == "Release Date")
            url =
                "https://www.gutenberg.org/ebooks/bookshelf/" + shelfNumber + "?sort_order=" + "release_date"
        else
            url =
                "https://www.gutenberg.org/ebooks/bookshelf/" + shelfNumber + "?sort_order=" + typeOfBrowse!!.lowercase()

        try {
            // Perform network operations in a background coroutine
            bookLinks = withContext(Dispatchers.IO) {
                collectSectionHrefs("", href = url, startingIndex = currentIndex)
            }
        } catch (e: Exception) {
            bookLinks = emptyList() // Ensure links is empty on error
        } finally {
            isLoading = false
        }
    }


    /**     * The main layout structure for the book listing screen, built using [Scaffold].
     *
     * This Scaffold provides a standard Material Design layout, organizing the UI with
     * a persistent top app bar for pagination and a main content area to display the list of books.
     *
     * - `topBar`: A [CenterAlignedTopAppBar] provides the main navigation controls. It features:
     *   - A "Home" button in the `title` area for quick navigation back to the main screen.
     *   - A "Previous" button in the `navigationIcon` slot, which decrements the `currentIndex`
     *     in the [DirectResultsViewModel] to load the previous page of results. It navigates
     *     up the back stack if on the first page.
     *   - A "Next" button in the `actions` slot, which increments the `currentIndex` to load
     *     the next page. This button is disabled while new data is being loaded.
     *
     * - `content`: The main body of the scaffold. It applies the `innerPadding` to ensure the
     *   content is not obscured by the top bar. It displays either a `CircularProgressIndicator`
     *   while `isLoading` is true, or a `LazyColumn` containing the list of fetched `bookLinks`.
     */
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Button(
                        onClick = {
                            navController.navigate(NavRoutes.Home.route) {
                                launchSingleTop = true
                            }
                        },
                    ) {
                        Text(text = "Home")
                    }                },
                navigationIcon = {
                    Button(
                        onClick = {
                            if (currentIndex > 1) { // Prevent going below the first page
                                viewModel.updateCurrentIndex((currentIndex - 25).coerceAtLeast(1))
                            } else {
                                // Optionally, navigate back if on the first page and "Previous" is clicked
                                navController.popBackStack()
                            }
                        },
                        // Disable if on the first page and not allowing popBackStack
                        enabled = currentIndex > 1 || navController.previousBackStackEntry != null,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null // The text itself provides the description
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing)) // Standard spacing
                            Text(text = "Previous")
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            // Simply update the currentIndex to trigger LaunchedEffect
                            viewModel.updateCurrentIndex(currentIndex + 25)
                        },
                        enabled = !isLoading, // Disable next if currently loading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Next")
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing)) // Standard spacing
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null // The text itself provides the description
                            )
                        }
                    }
                },

            )
        }
    ) { innerPadding ->
        // This is the main content of your screen.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply the padding here
                .padding(16.dp), // Your own additional padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Text(
                        "Gutenberg Web Access!",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${URLDecoder.decode(label, StandardCharsets.UTF_8.toString())}",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (error != null) {
                        Text("Error: $error")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 48.dp) // Add padding to the bottom
                        ) {
                            items(bookLinks) { item ->
                                val scope = rememberCoroutineScope()
                                var isCheckingUrl by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth() // 1. Make the Row take the full width of the list item.
                                        .padding(horizontal = 24.dp), // 2. Add consistent horizontal padding to the Row.
                                    horizontalArrangement = Arrangement.Start // 3. Align the content (the Button) to the left.
                                ) {
                                    Button(
                                        onClick = {
                                            if (isCheckingUrl) return@Button // Prevent multiple clicks
                                            isCheckingUrl = true

                                            scope.launch {
                                                val bookId = item.href.substringAfterLast("/")
                                                val oggFileUrl =
                                                    "https://www.gutenberg.org/files/$bookId/ogg/$bookId-01.ogg"
                                                val encodedUrl = URLEncoder.encode(
                                                    item.href,
                                                    StandardCharsets.UTF_8.toString()
                                                )
                                                val encodedLabel = URLEncoder.encode(
                                                    item.label,
                                                    StandardCharsets.UTF_8.toString()
                                                )


                                                //Log.d("BookListing", "bookId: $bookId")
                                                //Log.d("BookListing", "href: ${item.href}")

                                                val oggFilePresent = urlExists(oggFileUrl)

                                                if (oggFilePresent) {  // check to see if .ogg is present
                                                    navController.navigate(
                                                        NavRoutes.ChosenAudioBook.route + "/${encodedLabel}/${encodedUrl}/${bookId}"
                                                    ) {
                                                        launchSingleTop = true
                                                    }
                                                } else {
                                                    navController.navigate(
                                                        NavRoutes.ChosenBook.route + "/${encodedLabel}/${encodedUrl}/${bookId}"
                                                    ) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                                isCheckingUrl =
                                                    false // Reset state after navigation
                                            }
                                        },
                                        enabled = !isCheckingUrl, // Disable button while checking
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Start // Align content to the left
                                        ) {
                                            if (isCheckingUrl) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(
                                                        24.dp
                                                    )
                                                )
                                            } else if (item.label.length > 150) {
                                                Text(
                                                    text = item.label.substring(0, 150)
                                                        .substringBeforeLast("\n") + "...\n" +
                                                            item.label.substringAfterLast("\n"),
                                                    fontSize = 12.sp
                                                )
                                            } else {
                                                Text(
                                                    text = item.label,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }

            }

        }
    }
}




