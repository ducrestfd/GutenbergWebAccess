package com.myhobby.gutenberglisten.screens

/*
Gutenberg Listen's raison d'être is to provide simple access to
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
import com.myhobby.gutenberglisten.NavRoutes
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.myhobby.gutenberglisten.util.formatTimeMillis
import com.myhobby.gutenberglisten.util.scaled
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.myhobby.gutenberglisten.viewmodels.OggPlaybackViewModel


/**
 * Lists file names within the "Download/gutenberglisten/folder" directory.
 *
 * This function checks for the presence of external storage and its readability.
 * It then attempts to access the "gutenberglisten" subdirectory within the
 * public "Downloads" directory. If the directory exists and is readable,
 * it returns a list of names for all files contained within it.
 *
 * @return A [List] of [String] objects, where each string is a file name.
 *         - Returns an empty list if the "gutenberglisten" directory doesn't exist,
 *           is not a directory, or if there's an I/O error listing files.
 *         - Returns `null` if the external storage is not readable (e.g., not mounted
 *           or mounted read-only without the necessary permissions).
 */

fun getgutenberglistenAudioFileNames(folderPath: String): List<String>? {
    // 1. Check if external storage is available and readable
    if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED &&
        Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED_READ_ONLY
    ) {
        Log.d("AudioPLayList:getAFN ", "external storage not available!!!")
        return null
    }

    // 2. Construct the path to the directory
    val gutenberglistenDir = File(folderPath)

    // 3. Check if the directory exists and is a directory
    if (!gutenberglistenDir.exists() || !gutenberglistenDir.isDirectory) {
        Log.d("AudioPLayList:getAFN ", "directory not found: ${gutenberglistenDir.absolutePath}")
        return emptyList()
    }

    // 4. List the files (names only)
    val files = gutenberglistenDir.listFiles()

    if (files == null) {
        Log.d("AudioPLayList:getAFN ", "files null for: ${gutenberglistenDir.absolutePath}")
        return emptyList()
    }

    Log.d("AudioPLayList:getAFN ", "Found ${files.size} files in ${gutenberglistenDir.absolutePath}")
    return files.filter { it.name.endsWith(".ogg", ignoreCase = true) }
        .map { file ->
            Log.d("mapped file: ", "${file.name}")
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
    Log.d("AudioPlayList", "oggFolderPath: $oggFolderPath")

    var fileNames by remember { mutableStateOf<List<String>?>(null) }
    val context = LocalContext.current

    val folderName = remember(oggFolderPath) {
        oggFolderPath?.removeSuffix("/")?.substringAfterLast("/") ?: ""
    }

    LaunchedEffect(oggFolderPath) {
        if (oggFolderPath != null) {
            fileNames = withContext(Dispatchers.IO) {
                getgutenberglistenAudioFileNames(oggFolderPath)?.sorted()
            }
        }
    }

    val savedState = remember(oggFolderPath) {
        oggFolderPath?.let { oggPlaybackViewModel.getPlaybackStateForBook(it) }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    "Gutenberg Listen!",
                    style = TextStyle(fontSize = 24.sp.scaled, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Button(onClick = { navController.navigate(NavRoutes.Home.route) }) {
                        Text(text = "Home", fontSize = 16.sp.scaled)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(onClick = { navController.popBackStack() }) {
                        Text(text = "Back", fontSize = 16.sp.scaled)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (savedState != null) {
                    val formattedPosition = formatTimeMillis(savedState.position)
                    Button(
                        onClick = {
                            val encodedPath = URLEncoder.encode(oggFolderPath, StandardCharsets.UTF_8.toString())
                            navController.navigate("${NavRoutes.OggPlayer.route}/$encodedPath/${savedState.chapter}")
                        }
                    ) {
                        Text(
                            text = "Continue Ch. ${savedState.chapter} at $formattedPosition",
                            fontSize = 16.sp.scaled
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(folderName, style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (fileNames == null) {
                item {
                    Text("Checking external storage...", fontSize = 16.sp.scaled)
                }
            } else if (fileNames!!.isEmpty()) {
                item {
                    Text("No saved audio files found.", fontSize = 16.sp.scaled)
                }
            } else {
                items(fileNames!!) { item ->
                    Button(
                        onClick = {
                            val encodedFilePath = URLEncoder.encode(
                                oggFolderPath,
                                StandardCharsets.UTF_8.toString()
                            )
                            val chapterNumber = item.substringBefore(".ogg").substringAfterLast("_")
                            navController.navigate("${NavRoutes.OggPlayer.route}/${encodedFilePath}/${chapterNumber.toIntOrNull() ?: 1}")
                        },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Chapter " + item.substringBefore(".ogg").substringAfterLast("_"),
                            fontSize = 16.sp.scaled
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}


