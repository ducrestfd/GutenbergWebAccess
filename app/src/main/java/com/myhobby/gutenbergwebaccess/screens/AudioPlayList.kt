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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.myhobby.gutenbergwebaccess.util.formatTimeMillis
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.myhobby.gutenbergwebaccess.viewmodels.OggPlaybackViewModel


/**
 * Lists file names within the "Download/Gutenbergwebaccess/folder" directory.
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

fun getGutenbergwebaccessAudioFileNames(folder: String): List<String>? {
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
        File(downloadsDir, "Gutenbergwebaccess/" + folder)

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
    return files.map { file ->
            file.name
    }
}


/**
 * A screen that displays the list of chapters for a downloaded OGG audiobook.
 *
 * This composable function serves as the table of contents for a specific audiobook.
 * It fetches the list of `.ogg` files from the book's directory, displays them as a scrollable
 * list of chapters, and allows the user to start playing any chapter.
 *
 * Key features:
 * - A "Continue" button appears if there is saved progress for the book, allowing the user
 *   to resume from their last position.
 * - Each file is presented as a "Chapter" button, which navigates to the [OggPlayer] screen.
 * - Standard navigation controls ("Home", "Back") are provided.
 *
 * @param navController The [NavController] used for handling navigation events, such as
 *   going back or navigating to the player screen.
 * @param oggPlaybackViewModel The [OggPlaybackViewModel] instance used to retrieve any
 *   saved playback state (e.g., last played chapter and position) for the current book.
 * @param oggFolderPath The encoded folder path of the audiobook, passed as a navigation
 *   argument. This is used to locate the audio files and query the playback state.
 */
@Composable
fun AudioPlayList(
    navController: NavController,
    oggPlaybackViewModel: OggPlaybackViewModel,
    oggFolderPath: String?
) {
    var fileNames by remember { mutableStateOf<List<String>?>(null) }
    val context = LocalContext.current // If you need context for permissions later

    //var currentLocation by remember { mutableStateOf<Int?>(null) }
    var currentChapterNumber by remember { mutableStateOf<String?>(null) }

    val folderName = oggFolderPath!!.substringAfterLast("/")


    LaunchedEffect(folderName) {
        fileNames = withContext(Dispatchers.IO) {
            getGutenbergwebaccessAudioFileNames(folderName)?.sorted()
        }
    }

    /**
     * A root layout container that centers all of its content on the screen.
     *
     * This [Box] is configured to fill the entire screen and apply center alignment,
     * which serves as a simple way to vertically and horizontally center the main [Column]
     * that holds all the UI elements for this screen.
     *
     * A [Column] that serves as the main content area, arranging all UI elements
     * vertically with a centered horizontal alignment.
     *
     * It contains the following controls and information, from top to bottom:
     * 1.  The main application title ("Gutenberg Web Access!").
     * 2.  A [Row] with "Home" and "Back" navigation buttons.
     * 3.  A conditional "Continue" button, which only appears if there is saved progress
     *     for the book. It displays the last chapter and position and navigates directly
     *     to the player at that point.
     * 4.  The name of the audiobook folder, displayed as a headline.
     * 5.  A `LazyColumn` that dynamically lists each `.ogg` file as a "Chapter" button.
     *     Clicking a chapter navigates to the [OggPlayer], passing the specific chapter
     *     number to start playback from the beginning of that file.
     */

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                "Gutenberg Web Access!",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row() {
                Button(
                    onClick = {
                        navController.navigate(NavRoutes.Home.route)
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
                    // modifier = Modifier.semantics {contentDescription = "Return to Previous Screen"}
                ) {
                    Text(text = "Back")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val savedState = oggFolderPath?.let { oggPlaybackViewModel.getPlaybackStateForBook(it) }

            if (savedState != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val formattedPosition = formatTimeMillis(savedState.position) // Assuming you have a formatTime helper
                Button(
                    onClick = {
                        val encodedPath = URLEncoder.encode(oggFolderPath, StandardCharsets.UTF_8.toString())
                        // Navigate to the player with the book's saved chapter
                        navController.navigate("${NavRoutes.OggPlayer.route}/$encodedPath/${savedState.chapter}")
                    },
                    // Modifier.semantics {contentDescription = "Continue Chapter ${savedState.chapter} at $formattedPosition"}
                ) {
                    Text(text = "Continue Ch. ${savedState.chapter} at $formattedPosition")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val folderName = oggFolderPath!!.substringAfterLast("/")

            Text(folderName, style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                if (fileNames == null) {
                    Text("Checking external storage...")
                } else if (fileNames!!.isEmpty()) {
                    Text("No saved books found.")
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
                                    //if (virtualFilename.endsWith(" (Text)")) {
                                    val encodedFilePath = URLEncoder.encode(
                                        oggFolderPath,
                                        StandardCharsets.UTF_8.toString()
                                    )
                                    val chapterNumber = item.substringBefore(".ogg").substringAfterLast("_")
                                    navController.navigate("${NavRoutes.OggPlayer.route}/${encodedFilePath}/${chapterNumber.toInt()}")
                                },
                                modifier = Modifier
                                //    .semantics {contentDescription = item}
                                    .height(36.dp)
                            ) {
                                Text(
                                    text = "Chapter " + item.substringBefore(".ogg").substringAfterLast("_"),
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                    }
                }
            }
        }

    }
}

