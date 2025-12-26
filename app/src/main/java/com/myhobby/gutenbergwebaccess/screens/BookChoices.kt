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

import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhobby.gutenbergwebaccess.NavRoutes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.myhobby.gutenbergwebaccess.viewmodels.ScrollLocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.myhobby.gutenbergwebaccess.util.getMetaTagContent
import com.myhobby.gutenbergwebaccess.viewmodels.OggPlaybackViewModel
import kotlinx.coroutines.CoroutineScope
import org.jsoup.Jsoup
import java.io.File


/**
* Parses a local HTML file and extracts the content of a specific meta tag.
*
* @param file The local HTML [File] object to parse.
* @param metaName The value of the 'name' attribute of the meta tag to find (e.g., "dc.language").
* @param charSet The character set of the file. Defaults to "UTF-8".
* @return The content of the meta tag as a String, or null if not found or an error occurs.
*/
suspend fun getMetaTagContent(file: File, metaName: String, charSet: String = "UTF-8"): String? {
    // Perform file parsing on the IO dispatcher
    return withContext(Dispatchers.IO) {
        try {
            // 1. Parse the local file using Jsoup.parse()
            val doc = Jsoup.parse(file, charSet)

            // 2. Select the meta tag using a CSS query
            val metaTag = doc.select("meta[name=$metaName]").first()

            // 3. Extract the 'content' attribute from the found tag
            metaTag?.attr("content")
        } catch (e: Exception) {
            // Log the error for debugging
            Log.e("GetMetaTagFromFile", "Failed to get meta tag '$metaName' from file ${file.path}", e)
            null // Return null on any error (file not found, tag not present, etc.)
        }
    }
}

/**
 * Deletes a book file or directory and correctly forces the MediaStore to remove its cached entries.
 *
 * @param context The application context, needed to access the ContentResolver.
 * @param bookIdentifier The file name or directory name to delete.
 * @return True if the file system deletion was successful, false otherwise.
 */
fun deleteBookAndRefreshMediaStore(context: Context, bookIdentifier: String): Boolean {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val appBookDir = File(downloadsDir, "Gutenbergwebaccess")
    val fileOrDirectoryToDelete = File(appBookDir, bookIdentifier)

    if (!fileOrDirectoryToDelete.exists()) {
        //Log.d("DeleteUtil", "File/Dir '$bookIdentifier' does not exist. Considered success.")
        return true
    }

    // 1. Collect all file paths that will be deleted.
    val pathsToDelete = fileOrDirectoryToDelete.walkTopDown()
        .map { it.absolutePath }
        .toList()

    if (pathsToDelete.isEmpty()) {
        //Log.d("DeleteUtil", "No paths to delete for '$bookIdentifier'.")
        return true
    }

    // 2. Perform the actual file system deletion.
    val wasDeletionSuccessful = fileOrDirectoryToDelete.deleteRecursively()

    // 3. If deletion was successful, update MediaStore. THIS IS THE CRITICAL PART.
    if (wasDeletionSuccessful) {
        //Log.d("DeleteUtil", "Successfully deleted from filesystem. Now updating MediaStore for ${pathsToDelete.size} paths.")
        try {
            // CORRECTLY build the WHERE clause for a variable number of items.
            // Creates a string like: "?,?,?,...,?"
            val placeholders = pathsToDelete.map { "?" }.joinToString(",")
            val whereClause = "${MediaStore.Files.FileColumns.DATA} IN ($placeholders)"
            val selectionArgs = pathsToDelete.toTypedArray()

            val resolver = context.contentResolver
            val rowsDeleted = resolver.delete(
                MediaStore.Files.getContentUri("external"),
                whereClause,
                selectionArgs
            )
            //Log.d("DeleteUtil", "MediaStore cleanup deleted $rowsDeleted rows.")
        } catch (e: Exception) {
            Log.e("DeleteUtil", "Error updating MediaStore after deletion", e)
            // We don't return false here because the file IS gone from disk.
            // The worst case is a ghost entry remains, but the primary action succeeded.
        }
    } else {
        Log.e("DeleteUtil", "Failed to delete '$bookIdentifier' from filesystem.")
    }

    return wasDeletionSuccessful
}




/**
 * Initiates a share intent to allow the user to share a file with other apps.
 *
 * This function creates a content URI for the given file using a [FileProvider]
 * and then launches an `ACTION_SEND` intent. The receiving app is granted
 * temporary read permission to the file.
 *
 * @param context The current [Context] used to create the [FileProvider] URI and start the intent.
 * @param file The [File] object representing the file to be shared.
 * @param mimeType The MIME type of the file to be shared (e.g., "text/html", "application/pdf").
 */
fun shareFile(context: Context, file: File, mimeType: String) {
    val fileUri: Uri? = getFileUri(context, file)

    if (fileUri != null) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Crucial for granting access

            // This flag is a hint to the system to launch the new activity
            // adjacent to the current one, which can improve the back stack
            // behavior, especially in multi-window environments. It makes
            // the new task part of the same overall user journey.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }

        val shareIntent = Intent.createChooser(sendIntent, null /* Chooser Title */)

        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(shareIntent)
        } else {
            // Handle no app can handle this
        }
    } else {
        // Handle file URI could not be created
    }
}


/**
 * Generates a content URI for a given file using a [FileProvider].
 *
 * This is necessary for granting other apps secure access to files stored by this app,
 * especially on Android 7.0 (API level 24) and higher. The authority string
 * must match the one declared in the app's AndroidManifest.xml for the [FileProvider].
 *
 * @param context The current [Context] used to access the [FileProvider].
 * @param file The [File] for which to generate a content URI.
 * @return A [Uri] representing the content URI for the file, or `null` if an error occurs
 *         (e.g., the file path is not configured in `file_paths.xml` or another
 *         `IllegalArgumentException` is thrown).
 */
fun getFileUri(context: Context, file: File): Uri? {
    return try {
        FileProvider.getUriForFile(
            context,
            "com.myhobby.gutenbergwebaccess.fileprovider", // Authority must match AndroidManifest
            file
        )
    } catch (e: IllegalArgumentException) {
        // Handle error (e.g., file path not configured in file_paths.xml)
        e.printStackTrace()
        null
    }
}

/**
 * Deletes a directory and all its contents recursively.
 *
 * @param directory The File object representing the directory to delete.
 * @return True if the directory was successfully deleted, false otherwise.
 */
fun deleteDirectory(directory: File): Boolean {
    if (directory.exists()) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    deleteDirectory(file) // Recursive call for subdirectories
                } else {
                    file.delete()
                }
            }
        }
    }
    return directory.delete() // Delete the empty directory itself
}


/**
 * A screen that provides the user with a set of actions for a selected saved book.
 *
 * This composable function displays a menu of options for a given book file or directory
 * that has been previously saved to the device. It allows the user to read, play,
 * delete, or share the book. The available options change based on whether the
 * selected item is an HTML book or an OGG audio book (directory).
 *
 * Key Actions:
 * - **Read/Play:** Navigates to the appropriate viewer/player (`TextToSpeechBookReader` for HTML, `AudioPlayList` for OGG).
 * - **Share:** Opens the standard Android share sheet to send the book file to another app.
 * - **Delete:** Permanently removes the book file or directory and its playback state after user confirmation.
 * - **Open with Chrome:** An alternative reader for HTML files, opening the book in the Chrome browser.
 *
 * @param navController The [NavController] used for handling navigation between screens.
 * @param viewModel The [ScrollLocationViewModel] used to manage scroll positions for text-based books.
 * @param oggPlaybackViewModel The [OggPlaybackViewModel] used to manage playback state for OGG audiobooks.
 * @param book The identifier for the selected book, which is its filename (for HTML) or folder name (for OGG),
 *             passed as a navigation argument.
 */
@Composable
fun BookChoices(navController: NavController,
                viewModel: ScrollLocationViewModel,
                oggPlaybackViewModel: OggPlaybackViewModel,
                book: String?
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Scope for launching coroutines
    val isOggFile by remember { mutableStateOf(book?.endsWith("/")) }
    var languageCode by remember { mutableStateOf<String?>(null) }

    //Log.d("BookChoices", "BookChoices Composable entered")
    //Log.d("BookChoices", "Book: $book")

    /**
     * Opens the given HTML file in the Google Chrome browser.
     *
     * It first checks if the file exists. If so, it creates a content URI for the
     * file using a [FileProvider] and then launches an `ACTION_VIEW` intent
     * explicitly targeting the Chrome browser package. Read permission is granted
     * to Chrome for the URI.
     *
     * @param file The HTML [File] to be opened.
     */
    fun openFileWithChrome(file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist.", Toast.LENGTH_SHORT).show()
            return
        }

        val authority = "com.myhobby.gutenbergwebaccess.fileprovider" // Matches your AndroidManifest
        val fileUri: Uri? = try {
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, "Error creating URI: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }

        if (fileUri == null) {
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "text/html")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Explicitly set Chrome's package
            setPackage("com.android.chrome")
        }

        try {
            context.startActivity(intent)
        } catch (e: SecurityException) {
            // This can happen if Chrome doesn't have permission,
            // though FLAG_GRANT_READ_URI_PERMISSION should handle it.
            Toast.makeText(
                context,
                "Security error opening with Chrome: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error opening with Chrome: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


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
            Text(
                "$book",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {

                        if (book != null) {
                            // Assuming HTML files end with .htm, .html or .ogg
                            if (book.endsWith(".htm", ignoreCase = true)
                                || book.endsWith(
                                    ".html",
                                    ignoreCase = true
                                )
                                || book.endsWith(
                                    "/",
                                    ignoreCase = true
                                )
                            ) {

                                // Construct the file path
                                val downloadsDir =
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val appBookDir = File(downloadsDir, "Gutenbergwebaccess")
                                val bookFile = File(appBookDir, book)

                                if (bookFile.exists() &&
                                    (bookFile.path.endsWith(".html", ignoreCase = true))
                                    || (bookFile.path.endsWith(".htm", ignoreCase = true))) {

                                    coroutineScope.launch {
                                        // --- THIS IS THE FIX ---
                                        // 1. Call the new getMetaTagContent with the File object
                                        val lang = getMetaTagContent(bookFile, "dc.language")

                                        // 2. Log the result for debugging
                                        // Log.d("BookChoices", "Detected language for ${bookFile.name}: $lang")

                                        // 3. Encode the file path for navigation
                                        val encodedFilePath = URLEncoder.encode(
                                            bookFile.absolutePath,
                                            StandardCharsets.UTF_8.toString()
                                        )

                                        // 4. Navigate using the fetched language code
                                        //    Use "en" as a fallback if the language couldn't be found
                                        navController.navigate("${NavRoutes.TextToSpeechBookReader.route}/$encodedFilePath/${lang ?: "en"}") {
                                            launchSingleTop = true
                                        }
                                    }
                                }else if (bookFile.exists()) {
                                    val encodedFilePath = URLEncoder.encode(
                                        bookFile.absolutePath,
                                        StandardCharsets.UTF_8.toString()
                                    )
                                    //Log.d("Book Choices", "Encoded audio file path: $encodedFilePath")
                                    navController.navigate("${NavRoutes.AudioPlayList.route}/${encodedFilePath}") {
                                        launchSingleTop = true
                                    }
                                    //Log.d("Book Choices", "Encoded audio file path: ${bookFile.path}")
                                    //navController.navigate("${NavRoutes.OggPlayer.route}/${bookFile.path}")
                                }
                                else {
                                    Toast.makeText(
                                        context,
                                        "Book file not found: $book",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                // Handle non-HTML files (e.g., open with Intent as before, or show a message)
                                Toast.makeText(
                                    context,
                                    "This book is not in HTML or OGG format.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "No book selected.", Toast.LENGTH_SHORT).show()
                        }

                    },
                    // modifier = Modifier.semantics { contentDescription = "Listen to the book" },
                ) {
                    Text(text = "Listen")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = {
                    if (book != null) {
                        // Assuming HTML files end with .htm or .html
                        if (book.endsWith(".htm", ignoreCase = true) || book.endsWith(
                                ".html",
                                ignoreCase = true
                            )
                        ) {
                            // Construct the file path
                            val downloadsDir =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val appBookDir = File(downloadsDir, "Gutenbergwebaccess")
                            val bookFile = File(appBookDir, book)

                            if (bookFile.exists()) {
                                // Encode the file path to be safely passed as a navigation argument
                                val encodedFilePath = URLEncoder.encode(
                                    bookFile.absolutePath,
                                    StandardCharsets.UTF_8.toString()
                                )
                                navController.navigate("${NavRoutes.HtmlBookViewer.route}/$encodedFilePath") {
                                    launchSingleTop = true
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Book file not found: $book",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            // Handle non-HTML files (e.g., open with Intent as before, or show a message)
                            Toast.makeText(
                                context,
                                "This book is not in HTML format. Opening with system viewer.",
                                Toast.LENGTH_LONG
                            ).show()
                            // You can re-add the Intent logic here for other file types if needed
                            openBookWithIntent(
                                context,
                                book
                            ) // You'd need to recreate or move this function
                        }
                    } else {
                        Toast.makeText(context, "No book selected.", Toast.LENGTH_SHORT).show()
                    }
                },
                    // modifier = Modifier.semantics {contentDescription = "Open the book as text"},
                    enabled = isOggFile == false
                ) {
                    Text(text = "Text")
                }


            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {

                Button(onClick = {
                    if (book != null) {
                        // Assuming HTML files end with .htm or .html
                        if (book.endsWith(".htm", ignoreCase = true) || book.endsWith(
                                ".html",
                                ignoreCase = true
                            )
                        ) {
                            // Construct the file path
                            val downloadsDir =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val appBookDir = File(downloadsDir, "Gutenbergwebaccess")
                            val bookFile = File(appBookDir, book)
                            shareFile(context, bookFile, "text/html")
                        }
                    }
                },
                    // modifier = Modifier.semantics {contentDescription = "Share the book as an HTML file"},
                    enabled = isOggFile == false
                ) {
                    Text("Share HTML File")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = {
                    if (book != null) {
                        if (book.endsWith(".htm", ignoreCase = true) || book.endsWith(
                                ".html",
                                ignoreCase = true
                            )
                        ) {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            )
                            val appBookDir = File(downloadsDir, "Gutenbergwebaccess")
                            val bookFile = File(appBookDir, book)

                            if (bookFile.exists()) {
                                openFileWithChrome(bookFile)
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "This book is not in HTML format. Opening with system viewer.",
                                Toast.LENGTH_LONG
                            ).show()
                            openBookWithIntent(context, book)
                        }
                    } else {
                        Toast.makeText(context, "No book selected.", Toast.LENGTH_SHORT).show()
                    }
                },
                    // modifier = Modifier.semantics {contentDescription = "Open book in Chrome browser"},
                    enabled = isOggFile == false
                ) {
                    Text(text = "Open in Chrome")
                }

            }




            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.popBackStack()
                },
                //modifier = Modifier.semantics {
                //    contentDescription = "Go back to the previous screen"
                //}
            ) {
                Text(text = "Back")
            }

            Spacer(modifier = Modifier.height(96.dp))

            Button(
                onClick = {
                    if (book != null) {
                        // Launch a single coroutine to manage the entire deletion process
                        coroutineScope.launch {
                            //Log.d("Book Choices", "Starting deletion for book: $book")

                            // Step 1: Perform file deletion on the IO thread and WAIT for the result.
                            val wasDeletionSuccessful = withContext(Dispatchers.IO) {
                                deleteBookAndRefreshMediaStore(context, book)
                            }

                            // Step 2: Check the result and act accordingly.
                            if (wasDeletionSuccessful && !book.endsWith("/")) {
                                //Log.d("Book Choices", "File deletion successful. Now handling UI and DB.")

                                // Step 3 (Optional but good practice): Delete the scroll location from the database.
                                // This can also be done on a background thread.
                                withContext(Dispatchers.IO) {
                                    val location = viewModel.getScrollLocationAsync(book).await()
                                    location?.let {
                                        viewModel.deleteScrollLocation(it.id)
                                        //Log.d("ScrollLocationViewModel", "Scroll location deleted for ${it.id}")
                                    }
                                }

                                // Step 4: Now that everything is done, update the UI and navigate back.
                                // This must run on the Main thread.
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Book deleted successfully.", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }

                            } else if (book.endsWith("/")) {
                                oggPlaybackViewModel.deletePlaybackState(book)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Book deleted successfully.", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            } else {
                                // If deletion failed, just show a message on the Main thread.
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to delete book.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "No book selected to delete.", Toast.LENGTH_SHORT).show()
                    }
                },
                // modifier = Modifier.semantics { contentDescription = "Delete the book" }
            ) {
                Text(text = "Delete")
            }
        }
    }
}


/**
 * Attempts to open a book file using a system intent.
 *
 * This function is intended as a fallback for file types that are not directly
 * handled by the app's internal viewers (e.g., non-HTML files). It constructs
 * a file path to the "Downloads/Gutenbergwebaccess" directory and attempts to
 * get a content URI via [FileProvider]. If successful, it launches an `ACTION_VIEW`
 * intent, allowing the Android system to choose an appropriate app to handle the file.
 *
 * Currently, if the file is not found or a URI cannot be created, it displays a Toast message.
 *
 * @param context The current [Context] used to create [FileProvider] URIs and start the intent.
 * @param bookFileName The name of the book file to be opened (e.g., "mybook.txt").
 */
fun openBookWithIntent(context: Context, bookFileName: String) {
    Toast.makeText(
        context,
        "Opening $bookFileName with system viewer (Not implemented for this type in WebView).",
        Toast.LENGTH_LONG,
    ).show()
}