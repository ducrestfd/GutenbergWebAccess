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
import com.myhobby.gutenbergwebaccess.collectCategoryHrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.myhobby.gutenbergwebaccess.Link
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


/**
 * Composable function that displays a screen listing subsections or specific bookshelves
 * within a broader category from the Gutenberg Project.
 *
 * This screen fetches a list of links (representing bookshelves or further categorizations)
 * based on a main category (e.g., "Literature," "History"). It then filters these links
 * to display only those relevant to the specified `subSection`. Each item in the list
 * is a button that, when clicked, navigates to the `BookListing` screen, passing along
 * the browsing type, the selected item's label (URL-encoded), and the item's href
 * (specifically, the trailing part, often an ID).
 *
 * The screen includes navigation buttons to go "Home" or "Back" to the previous screen.
 * It displays the main application title, the current `subSection` being viewed, and
 * the `typeOfBrowse` criteria (e.g., "Ordered by Popularity").
 *
 * Data fetching is performed in a `LaunchedEffect` coroutine.
 *
 * @param navController The [NavController] used for navigating between screens.
 * @param typeOfBrowse An optional [String] indicating the sorting criteria (e.g., "Popularity",
 *                     "Release Date") that will be passed to the `BookListing` screen.
 * @param subSection An optional [String] representing the specific sub-category or
 *                   section name (e.g., "Literature", "Science Fiction") whose items
 *                   are to be displayed.
 */
@Composable
fun BrowseSection(navController: NavController, typeOfBrowse: String?, subSection: String?) {


    var links: List<Link> by remember { mutableStateOf<List<Link>>(emptyList()) }
    var sectionLinks by remember { mutableStateOf<List<Link>>(emptyList()) }


    /**
     * A [LaunchedEffect] responsible for fetching and filtering the list of book categories or
     * bookshelves from the Gutenberg Project website.
     *
     * This effect runs once when the composable is first displayed (`key1 = Unit`). It performs
     * the following actions asynchronously to avoid blocking the UI thread:
     * 1.  Sets a `try-catch` block to gracefully handle potential network errors.
     * 2.  Switches to the `Dispatchers.IO` context, which is optimized for network operations.
     * 3.  Calls the `collectCategoryHrefs()` suspend function to scrape the website for all
     *     available category links.
     * 4.  Upon successful fetching, it populates the local `links` state with the full list.
     * 5.  It then filters this full list based on the `subSection` parameter passed to this
     *     screen, sorts the results alphabetically by their label, and populates the
     *     `sectionLinks` state, which is used to render the `LazyColumn`.
     * 6.  If an exception occurs during the fetch, it ensures the lists remain empty.
     */
    LaunchedEffect(Unit) { // Use Unit if it only needs to run once
        try {
            // Perform network operations in a background coroutine
            val fetchedLinks: List<Link> = withContext(Dispatchers.IO) {
                collectCategoryHrefs()
            }
            links = fetchedLinks
            sectionLinks = links
                .filter { it.section == subSection }
                .sortedBy { it.label }
        } catch (e: Exception) {
            // Log.e("LiteratureScreen", "Failed to load categories", e)
            links = emptyList() // Ensure links is empty on error
            sectionLinks = links.filter { it.section == subSection }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Consume insets for the entire screen if your Box is the root
            // .windowInsetsPadding(WindowInsets.systemBars) // Option 1: Apply to the root
            .padding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()), // Apply horizontal padding to the root if you only want vertical for the list
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize() // Column should fill the Box
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            Row() {
                Button(
                    onClick = {
                        navController.navigate(NavRoutes.Home.route) {
                            launchSingleTop = true
                        }
                    },
                    // modifier = Modifier.semantics {contentDescription = "Return to Home Screen"}
                ) {
                    Text(text = "Home")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        //navController.navigate(NavRoutes.Search.route + "/$typeOfSearch")
                        navController.popBackStack()
                    },
                    // modifier = Modifier.semantics {contentDescription = "Back to Previous Screen"}
                ) {
                    Text(text = "Back")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Gutenberg Web Access!",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                "${subSection}",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                "Ordered by ${typeOfBrowse}",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))


            LazyColumn (
                // com . google . firebase . crashlytics . buildtools . reloc . org . apache . commons . cli . Option 2: Apply padding only to the LazyColumn
                // This is often preferred to avoid shifting other UI elements unnecessarily.
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp // Add extra dp if needed
                ),
                modifier = Modifier.weight(1f)
            ) {
                items(sectionLinks) { item ->
                    Button(
                        onClick = {
                            val encodedLabel = URLEncoder.encode(item.label, StandardCharsets.UTF_8.toString())
                            navController.navigate(
                                NavRoutes.BookListing.route +
                                        "/${typeOfBrowse}" +
                                        "/${encodedLabel}" +  // Use the encoded label
                                        "/${item.href.substringAfterLast("/")}"
                            ){
                                launchSingleTop = true
                            }
                        },
                        // modifier = Modifier.semantics {contentDescription = item.label}
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 12.sp
                        )
                    }

                }
            }

        }

    }
    Spacer(modifier = Modifier.height(48.dp))
}