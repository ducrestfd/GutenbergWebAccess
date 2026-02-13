package com.myhobby.gutenbergwebaccess.screens

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

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.myhobby.gutenbergwebaccess.Link
import com.myhobby.gutenbergwebaccess.collectSectionHrefs
import com.myhobby.gutenbergwebaccess.util.scaled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.myhobby.gutenbergwebaccess.util.urlExists
import com.myhobby.gutenbergwebaccess.viewmodels.DirectResultsViewModel


/**
* A Composable screen that displays a paginated list of search results from the Project Gutenberg website.
*
* This screen takes a search term and a search type (e.g., "downloads", "release_date", "title")
* and constructs a URL to query the Gutenberg search engine. It then fetches the HTML results,
* scrapes the book links using [collectSectionHrefs], and displays them in a [LazyColumn].
*
* The screen supports pagination through "Next" and "Previous" buttons, which update the
* starting index of the search query and trigger a refetch of the data. While data is being
* fetched, a [CircularProgressIndicator] is shown.
*
* Each book in the results list is a clickable button. When clicked, the function checks if
* an associated audiobook (.ogg file) exists. Based on the result, it navigates either to the
* [ChosenAudioBook] screen or the [ChosenBook] screen, passing the necessary book details
* (label, URL, and book ID) as navigation arguments.
*
* @param navController The [NavController] used for handling navigation between screens.
* @param searchType A string indicating how the search results should be sorted.
*   Expected values include "term", "downloads", "release_date", or "title". This is
*   also used for display purposes.
* @param searchTerm The user-provided query string to search for on Project Gutenberg.
* @param initialStartingIndex The starting index for the search results, used for pagination.
*   Defaults to 1 for the first page.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectResults(
    navController: NavController,
    viewModel: DirectResultsViewModel, // Receive the ViewModel
    searchType: String?,
    searchTerm : String?
) {

    //Log.d("DirectResults", "searchTu[e: $searchType")
    //Log.d("DirectResults", "searchTerm: $searchTerm")


    val currentIndex = viewModel.currentIndex

    var isLoading by remember { mutableStateOf(true) } // Optional: for loading indicator
    var error by remember { mutableStateOf<String?>(null) } // Optional: for error message
    var bookLinks by remember { mutableStateOf<List<Link>>(emptyList()) }


    /**
     * A [LaunchedEffect] that triggers the fetching of book search results from the Project Gutenberg website.
     *
     * This effect is keyed to the `currentIndex`, meaning it will automatically re-execute
     * whenever the page index changes (e.g., when the user clicks "Next" or "Previous").
     *
     * Upon execution, it performs the following steps:
     * 1. Sets the `isLoading` state to `true` to display a loading indicator.
     * 2. Constructs the appropriate search URL based on the `searchType` and `searchTerm`.
     * 3. Switches to the `Dispatchers.IO` context to perform the network operation of fetching
     *    and scraping the HTML for book links using [collectSectionHrefs].
     * 4. Populates the `bookLinks` state with the results upon success.
     * 5. Sets `isLoading` to `false` in a `finally` block to ensure the loading indicator is
     *    always hidden, even if an error occurs.
     */
    LaunchedEffect(currentIndex) { // Use Unit if it only needs to run once
        isLoading = true
        error = null

        var url: String = ""

        if (searchType == "downloads")
            url =
                "https://www.gutenberg.org/ebooks/search/?query=" +
                        URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString()) +
                        "&sort_order=downloads"
        else if (searchType == "release_date")
            url =
                "https://www.gutenberg.org/ebooks/search/?query=" +
                        URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString()) +
                        "&sort_order=release_date"
        else
            url =
                "https://www.gutenberg.org/ebooks/search/?query=" +
                        URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString()) +
                        "&sort_order=title"
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



    /**
     * The main layout structure for the search results screen, built using [Scaffold].
     *
     * This Scaffold provides a standard Material Design layout, organizing the screen into a
     * top app bar for navigation and a main content area for displaying the results.
     *
     * - `topBar`: A [CenterAlignedTopAppBar] is used to provide persistent navigation controls.
     *   It includes a "Home" button in the title area, a "Previous" button in the `navigationIcon`
     *   slot, and a "Next" button in the `actions` slot. These buttons interact with the [DirectResultsViewModel]
     *   to handle pagination by updating the `currentIndex`.
     *
     * - `content`: The main body of the scaffold, which displays a `LazyColumn` of the fetched
     *   book links. It also handles the display of a `CircularProgressIndicator` while data is
     *   being loaded, or an error message if the fetch fails. The `innerPadding` provided
     *   by the Scaffold is applied to ensure content is not obscured by the top bar.
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
                        Text(text = "Home", fontSize = 16.sp.scaled)
                    }
                },
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
                        enabled = currentIndex > 1 || navController.previousBackStackEntry != null,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null // The text itself provides the description
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing)) // Standard spacing
                            Text(text = "Previous", fontSize = 16.sp.scaled)
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
                            Text(text = "Next", fontSize = 16.sp.scaled)
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
                        style = TextStyle(fontSize = 24.sp.scaled, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "sorted by ${
                            URLDecoder.decode(
                                searchType,
                                StandardCharsets.UTF_8.toString()
                            )
                        }",
                        style = TextStyle(fontSize = 16.sp.scaled, fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator()
                    } else if (error != null) {
                        Text("Error: $error", fontSize = 16.sp.scaled)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 48.dp) // Add padding to the bottom
                        ) {

                            // Log.d("DirectResults", "bookLinks: $bookLinks")

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
                                            if (isCheckingUrl) return@Button
                                            isCheckingUrl = true

                                            scope.launch {
                                                // CRITICAL: Ensure 'item.href' from a search result can be parsed like this.
                                                // It's possible the URL structure is different.
                                                val bookId = item.href.substringAfterLast("/")
                                                val oggFileUrl =
                                                    "https://www.gutenberg.org/files/$bookId/ogg/$bookId-01.ogg"

                                                //Log.d("DirectResults", "Section: " + item.section)

                                                val encodedUrl = URLEncoder.encode(
                                                    item.href,
                                                    StandardCharsets.UTF_8.toString()
                                                )
                                                val encodedLabel = URLEncoder.encode(
                                                    item.label,
                                                    StandardCharsets.UTF_8.toString()
                                                )

                                                val oggFilePresent =
                                                    urlExists(oggFileUrl) // Assuming urlExists is available or moved

                                                if (oggFilePresent) {
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
                                                isCheckingUrl = false
                                            }
                                        },
                                        modifier = Modifier
                                            //.fillMaxWidth()
                                            //.widthIn(min = 100.dp)
                                            //.padding(horizontal = 10.dp, vertical = 4.dp),
                                            .padding(vertical = 2.dp),
                                    ) {
                                        Row(
                                            //modifier = Modifier.widthIn(min = 100.dp),
                                            //modifier = Modifier.fillMaxWidth(), // Make the Row fill the button's width
                                            horizontalArrangement = Arrangement.Start // Align content to the left
                                        ) {
                                            if (item.label.length > 150) {
                                                Text(
                                                    text = item.label.substring(0, 150)
                                                        .substringBeforeLast("\n") + "...\n" +
                                                            item.label.substringAfterLast("\n"),
                                                    fontSize = 12.sp.scaled
                                                )
                                            } else {
                                                Text(
                                                    text = item.label,
                                                    fontSize = 12.sp.scaled
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

