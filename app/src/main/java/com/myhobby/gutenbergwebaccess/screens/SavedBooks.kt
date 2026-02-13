@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

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

package com.myhobby.gutenbergwebaccess.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.myhobby.gutenbergwebaccess.util.scaled
import com.myhobby.gutenbergwebaccess.viewmodels.ScrollLocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Lists file names within the "Download/Gutenbergwebaccess" directory.
 *
 * This function checks for the presence of external storage and its readability.
 * It then attempts to access the "Gutenbergwebaccess" subdirectory within the
 * public "Downloads" directory. If the directory exists and is readable,
 * it returns a list of names for all files contained within it.
 *
 * @return A [List] of [String] objects, where each string is a file name.
 *         - Returns an empty list if the "Gutenbergwebaccess" directory doesn't exist,
 *           is not a directory, or if there's an I/O error listing files.
 *         - Returns `null` if the external storage is not readable (e.g., not mounted
 *           or mounted read-only without the necessary permissions).
 */
fun getGutenbergwebaccessFileNames(): List<String>? {

    // 1. Check if external storage is available and readable
    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED &&
        Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED_READ_ONLY
    ) {
        return null
    }

    // 2. Get the public Downloads directory
    val downloadsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    // 3. Construct the path to the Gutenbergwebaccess subdirectory
    val GutenbergwebaccessDir =
        File(downloadsDir, "Gutenbergwebaccess")

    // 4. Check if the directory exists and is a directory
    if (!GutenbergwebaccessDir.exists() || !GutenbergwebaccessDir.isDirectory) {
        //println("Directory not found or is not a directory: ${GutenbergwebaccessDir.absolutePath}")
        return emptyList() // Or handle as an error
    }

    // 5. List the files (names only)
    //    listFiles() can return null if there's an I/O error or if it's not a directory
    val files = GutenbergwebaccessDir.listFiles()

    if (files == null) {
        //println("Could not list files in directory: ${GutenbergwebaccessDir.absolutePath}")
        return emptyList() // Or handle as an error
    }

    // Map to their names. You can differentiate them later if needed,
    // or add a prefix/suffix here.
    return files.mapNotNull { file ->
        if (file.isDirectory) {
            // Check if the directory is not empty before adding it to the list.
            if (file.listFiles()?.isNotEmpty() == true) {
                "${file.name}/" // Add a trailing slash to indicate it's a non-empty directory.
            } else {
                null // If the directory is empty, return null to have mapNotNull discard it.
            }
        } else {
            file.name // This is a file, so include it.
        }
    }
}



/**
 * A screen that displays a list of all books and audiobooks downloaded by the user.
 *
 * This composable function scans the application's dedicated subdirectory within the public
 * "Downloads" folder (`Download/Gutenbergwebaccess`) and displays a list of all found files
 * and folders. Each item in the list is presented as a clickable button.
 *
 * The file list is fetched asynchronously on a background thread using a [LaunchedEffect]
 * that is keyed to the current navigation route. This ensures the list is refreshed every
 * time the user navigates to this screen.
 *
 * Key Features:
 * - **File Listing**: Automatically finds and lists all saved content.
 * - **Dynamic Refresh**: The list of saved books is updated whenever the screen becomes visible.
 * - **User-Friendly Naming**: Filenames are cleaned up for display (e.g., replacing underscores
 *   with spaces and adding "(Audio)" or "(Text)" suffixes for clarity).
 * - **Navigation**: Provides "Home" and "Back" buttons for standard app navigation.
 * - **State Handling**: Displays informative messages to the user, such as "Checking external
 *   storage..." or "No saved books found."
 *
 * Clicking on a book or audiobook in the list navigates the user to the `BookChoices`
 * screen, passing the selected filename as a parameter so that actions like reading or
 * sharing can be performed.
 *
 * @param navController The [NavController] instance used for handling navigation events,
 *                      such as moving to the `BookChoices` screen.
 * @param viewModel The [ScrollLocationViewModel], passed for consistency and potential
 *                  future use, although not directly used in this composable's UI logic.
 */
@Composable
fun SavedBooks(navController: NavController, viewModel: ScrollLocationViewModel) {

    var fileNames by remember { mutableStateOf<List<String>?>(null) }
    val context = LocalContext.current // If you need context for permissions later

    // This key will change whenever the user navigates back to this screen.
    // currentBackStackEntryAsState gives us a state-aware snapshot of the navigation stack.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                "Gutenberg Web Access!",
                style = TextStyle(fontSize = 24.sp.scaled, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row() {
                Button(
                    onClick = {
                        navController.navigate(NavRoutes.Home.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Home", fontSize = 16.sp.scaled)
                }


                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        navController.popBackStack()
                    }
                ) {
                    Text(text = "Back", fontSize = 16.sp.scaled)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Saved Books", style = MaterialTheme.typography.headlineSmall, fontSize = 20.sp.scaled)

            Spacer(modifier = Modifier.height(16.dp))


            // Key the LaunchedEffect to the current route.
            // This ensures it re-runs when you navigate back to this screen.
            LaunchedEffect(currentRoute) {
                // To prevent running when leaving the screen, an extra check is good practice.
                if (currentRoute == NavRoutes.SavedBooks.route) { // Assuming you have a route name for this screen
                    fileNames = withContext(Dispatchers.IO) {
                        getGutenbergwebaccessFileNames()
                    }
                }
            }


            Column {
                if (fileNames == null) {
                    Text("Checking external storage...", fontSize = 16.sp.scaled)
                } else if (fileNames!!.isEmpty()) {
                    Text("No saved books found.", fontSize = 16.sp.scaled)
                } else {
                    LazyColumn (
                        // Option 2: Apply padding only to the LazyColumn
                        // This is often preferred to avoid shifting other UI elements unnecessarily.
                        contentPadding = PaddingValues(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp // Add extra dp if needed
                        ),
                        modifier = Modifier.weight(1f) // Make LazyColumn take available space
                    ) {

                        items(fileNames!!) { item ->
                            var virtualFilename = item.replace("_", " ")
                            virtualFilename = virtualFilename.replace(".html", " (Text)", ignoreCase = true)
                            virtualFilename = virtualFilename.replace("/", " (Audio)", ignoreCase = true)
                            Button(
                                onClick = {
                                    //Log.d("SavedBooks", "Clicked on $item")
                                    navController.navigate(NavRoutes.BookChoices.route + "/$item") {
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Text(
                                    text = virtualFilename.replace("_", " "),
                                    fontSize = 16.sp.scaled
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                    }
                }
            }
        }

    }
}
