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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes
import com.myhobby.gutenbergwebaccess.viewmodels.DirectResultsViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType.Companion.Uri
import com.myhobby.gutenbergwebaccess.R
import com.myhobby.gutenbergwebaccess.util.PdfViewerScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import android.webkit.WebView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.viewinterop.AndroidView



/**
 * The main landing screen of the application, serving as the central hub for navigation.
 *
 * This Composable provides the user with primary actions, including:
 * - A button to navigate to the list of locally [SavedBooks].
 * - A custom text field ([CustomTextField]) for entering search terms.
 * - A series of buttons to initiate searches on the Gutenberg website, with options to sort
 *   results by popularity (downloads), title, or release date.
 * - A set of buttons for browsing pre-defined categories like Popularity, Title, Author, etc.
 * - Navigation to the [About] screen.
 * - A settings icon in the [TopAppBar] to navigate to the [Settings] screen.
 *
 * It utilizes the [DirectResultsViewModel] to reset the pagination index before performing
 * a new search, ensuring that searches always start from the first page. The UI is structured
 * using a [Scaffold] and a main [Column] to vertically arrange the controls.
 *
 * @param navController The [NavController] instance used to handle all navigation events
 *   triggered by button clicks.
 * @param directResultsViewModel The [DirectResultsViewModel] used to manage state for
 *   paginated search results, primarily for resetting the index before a new search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navController: NavController, directResultsViewModel: DirectResultsViewModel) {

    var searchTerm by remember { mutableStateOf("") }
    val searchTermChange = { text: String ->
        searchTerm = text
    }

    val executeSearch = {
        // Only execute if the search term is not blank
        if (searchTerm.isNotBlank()) {
            directResultsViewModel.resetIndex()
            // Default to sorting by downloads (popularity)
            navController.navigate(NavRoutes.DirectResults.route + "/downloads/${searchTerm}") {
                launchSingleTop = true
            }
        }
    }

    val webView: WebView = WebView(LocalContext.current)


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gutenberg Web Access!",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings"
                        )
                    }
                }
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.SavedBooks.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Saved Books")
                }

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    title = "Enter search term:",
                    textState = searchTerm,
                    onTextChange = searchTermChange,
                    onSearch = executeSearch
                )

                Button(
                    onClick = {
                        directResultsViewModel.resetIndex()
                        navController.navigate(NavRoutes.DirectResults.route + "/downloads/${searchTerm}") {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Search")
                }

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.DirectResults.route + "/title/${searchTerm}") {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Search & Sort by Title")
                }
                Button(
                    onClick = {
                        navController.navigate(NavRoutes.DirectResults.route + "/release_date/${searchTerm}") {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Search & Sort by Release Date")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.Browse.route + "/Popularity") {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Browse by Popularity")
                }

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.Browse.route + "/Title") {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Browse by Title")
                }

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.Browse.route + "/Author") {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Browse by Author")
                }

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.Browse.route + "/Release Date") {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Browse by Release Date")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.About.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "About")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    TextButton(
                        onClick = {
                            navController.navigate(NavRoutes.PrivacyPolicy.route)
                        },
                        // We can adjust the padding to make it sit tighter with other text
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            text = "Privacy Policy",
                            color = Color(0xFF1A73E8), // Standard "Link Blue"
                            style = TextStyle(
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    }



                    TextButton(
                        onClick = {
                            navController.navigate(NavRoutes.AccessibilityStatement.route)
                        },
                        // We can adjust the padding to make it sit tighter with other text
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            text = "Accessibility Statement",
                            color = Color(0xFF1A73E8), // Standard "Link Blue"
                            style = TextStyle(
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    }
                }

            }
        }
    }
}


/**
 * A custom [OutlinedTextField] composable for text input.
 *
 * This function provides a styled text field with a label, single-line input,
 * and specific text styling (bold, 30sp font size).
 *
 * @param title The label text to be displayed for the text field.
 * @param textState The current string value of the text field.
 * @param onTextChange A lambda function that is invoked when the text field's value changes.
 *                     It receives the new text string as a parameter.
 */
@Composable
fun CustomTextField(
    title: String,
    textState: String,
    onTextChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = textState,
        onValueChange = { onTextChange(it) },
        singleLine = true,
        label = { Text(title) },
        modifier = Modifier.padding(10.dp),
        textStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search // 1. Show a "Search" icon on the keyboard
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() } // 2. Trigger the onSearch lambda when pressed
        )

    )
}
