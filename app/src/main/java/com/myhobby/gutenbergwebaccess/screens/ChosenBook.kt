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

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontStyle
import androidx.core.content.ContextCompat
import com.myhobby.gutenbergwebaccess.viewmodels.ScrollLocationViewModel
import androidx.lifecycle.viewModelScope
import com.myhobby.gutenbergwebaccess.DownloadCompletedReceiver
import com.myhobby.gutenbergwebaccess.viewmodels.AudioLocationViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import android.os.Build
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState




/**
 * Enqueues a book's HTML file for download and saves its metadata to local databases.
 *
 * This function uses Android's [DownloadManager] to fetch an HTML version of a book
 * from the Project Gutenberg website. The downloaded file is saved to a dedicated
 * subfolder within the public "Downloads/Gutenbergwebaccess" directory.
 *
 * Upon successfully enqueuing the download, the function performs several key actions:
 * 1.  It constructs a sanitized filename from the book's label.
 * 2.  It configures a [DownloadManager.Request] with the file URL, title, and destination.
 * 3.  The download request is enqueued, and the resulting download ID is passed back via the
 *     `onDownloadStarted` callback.
 * 4.  It launches coroutines to call `addScrollLocation` and `addAudioLocation` on the
 *     provided ViewModels, creating initial database entries for the book's scroll
 *     position and audio playback state.
 * 5.  A Toast message is displayed to the user to confirm that the download has started.
 *
 * @param context The application [Context], used to access system services like [DownloadManager].
 * @param textNumber The unique numeric ID of the book on Project Gutenberg, used to construct the download URL.
 * @param decodedLabel The title of the book, used to generate a user-friendly filename.
 * @param viewModel The [ScrollLocationViewModel] instance for managing the book's scroll position data.
 * @param audioViewModel The [AudioLocationViewModel] instance for managing the book's audio progress data.
 * @param onDownloadStarted A callback function that is invoked with the unique download ID
 *                          once the download has been successfully enqueued. This ID can be
 *                          used to track the download's status.
 */
fun startDownload(
    context: Context,
    textNumber: Int,
    decodedLabel: String?,
    viewModel: ScrollLocationViewModel,
    audioViewModel: AudioLocationViewModel,
    onDownloadStarted: (Long) -> Unit
) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val fileUrl = "https://www.gutenberg.org/cache/epub/${textNumber}/pg${textNumber}-images.html"

    var fileName = decodedLabel ?: "book_${textNumber}"
    fileName = fileName.replace(Regex("[.:]"), "").replace(" ", "_") + ".html"

    try {

        /**
         * A [DownloadManager.Request] object configured to download the book's HTML file.
         *
         * This object contains all the necessary parameters for the download, including:
         * - The source URL of the book on the Gutenberg website.
         * - The title and description to be displayed in the system's download notifications.
         * - The destination path within the public "Downloads/Gutenbergwebaccess" directory.
         * - Network policies, allowing the download over both metered (cellular) and roaming connections.
         */
        val request = DownloadManager.Request(fileUrl.toUri())
            .setTitle(fileName)
            .setDescription("Downloading $fileName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Gutenbergwebaccess/${fileName}"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val id = downloadManager.enqueue(request)
        onDownloadStarted(id) // Pass the download ID back

        Toast.makeText(context, "Download started...", Toast.LENGTH_LONG).show()

        //viewModel.viewModelScope.launch {
        //    viewModel.addScrollLocation(fileName)
        //}

        /**
        * Launches a coroutine to create and verify the initial database entry for
        * the book's text reading progress.
        *
        * This coroutine, tied to the [ScrollLocationViewModel]'s lifecycle, ensures
        * that as soon as a download is started, a corresponding record is made in the         * `scroll_location_table` so that its scroll position can be tracked later.
        *
        * After calling `addScrollLocation` to create the entry, it immediately calls
        * `getScrollLocationAsync` and awaits the result to confirm that the database
        * operation was successful. The logging within this block is for debugging
        * purposes to trace the lifecycle of the scroll location record.
        */
        viewModel.viewModelScope.launch {
            viewModel.addScrollLocation(fileName)
            // //Log.d(
            //    "ScrollLocationViewModel",
            //    "Location added: $fileName"
            //)

            val locationDeferred =
                viewModel.getScrollLocationAsync(fileName)
            // You can do other work here while it loads
            val location =
                locationDeferred.await() // await() is a suspend function
            if (location != null) {
                // //Log.d(
                //    "ScrollLocationViewModel",
                //    "Location found: $location"
                //)
            } else {
                // //Log.d("ScrollLocationViewModel", "Location not found")
            }
        }

        /**
         * Launches a coroutine to create and verify the initial database entry for
         * the book's audio (Text-to-Speech) playback state.
         *
         *
         * This coroutine, tied to the [AudioLocationViewModel]'s lifecycle, ensures that
         * a corresponding record is created in the`audio_location_table` as soon as
         * a book download is initiated. This record will store the TTS progress, speech rate,
         * and pitch.
         *
         * It first calls `addAudioLocation` to create the entry with the user's default         * speaking speed, then immediately calls `getBookProgressAsync` to confirm that the
         * database operation was successful.
         */
        audioViewModel.viewModelScope.launch {
            audioViewModel.addAudioLocation(fileName)

            val locationDeferred =
                audioViewModel.getBookProgressAsync(fileName)
            // You can do other work here while it loads
            val location =
                locationDeferred.await() // await() is a suspend function
            if (location != null) {
                // //Log.d(
                //    "AudioLocationViewModel",
                //    "Audio Location found: $location"
                //)
            } else {
                // //Log.d(
                //    "AudioLocationViewModel",
                //    "Audio Location not found"
                //)
            }
        }



    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e("ChosenBook", "Download failed", e)
    }
}



/**
 * A screen that displays details for a chosen book and provides actions for the user.
 *
 * This composable serves as the main detail screen for a book selected by the user.
 * It dynamically fetches the book's author from its Project Gutenberg webpage using a
 * web scraping utility (`getMetaTagContent`).
 *
 * The screen provides several key functionalities:
 * - **Navigation**: Standard "Home", "Back", and "Saved Books" navigation controls.
 * - **Downloading**: A "Download HTML" button that initiates a download of the book's
 *   HTML version using Android's [DownloadManager]. It handles runtime permissions for
 *   older Android versions (API 28 and below). Upon starting a download, it also creates
 *   initial entries in the local database via the provided ViewModels for tracking
 *   reading and audio progress.
 * - **Sharing**: "Share" buttons allow the user to send links for the book's HTML and
 *   plain text versions to other applications using the Android Sharesheet.
 * - **State Management**: It uses a [DownloadCompletedReceiver] to listen for system
 *   broadcasts, enabling reactions to completed downloads (e.g., navigating to the
 *   saved books screen). The receiver is registered and unregistered safely using
 *   a [DisposableEffect].
 *
 * The UI displays the book's title and the fetched author, with loading indicators
 * while data is being retrieved.
 *
 * @param navController The [NavController] instance for handling navigation events.
 * @param viewModel The [ScrollLocationViewModel] used to manage and persist the reading
 *   scroll position for saved books.
 * @param audioViewModel The [AudioLocationViewModel] used to manage and persist the
 *   audio playback progress for saved books.
 * @param label The URL-encoded title of the book, passed as a navigation argument.
 *   This is decoded for display.
 * @param url The URL-encoded relative path of the book on the Gutenberg website.
 * @param textNumber The unique numeric ID of the book on Project Gutenberg, which serves
 *   as the primary key for fetching data and constructing URLs.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChosenBook(
    navController: NavController,
    viewModel: ScrollLocationViewModel,
    audioViewModel: AudioLocationViewModel,
    label: String?,
    url: String?,
    textNumber: Int
) {

    val decodedLabel = remember(label) {
        try {
            URLDecoder.decode(label, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            label ?: "" // Fallback to the raw value if decoding fails
        }
    }
    val decodedUrl = remember(url) {
        try {
            URLDecoder.decode(url, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            url ?: "" // Fallback
        }
    }

    //Log.d("ChosenBook", "decoded label: $decodedLabel")
    //Log.d("ChosenBook", "decoded url: $decodedUrl")
    //Log.d("ChosenBook", "textNumber: $textNumber")


    var titleAuthor by remember { mutableStateOf<String?>(null) }
    var justTitle by remember { mutableStateOf<String?>(null) }
    var justAuthor by remember { mutableStateOf<String?>(null) }


    justTitle = decodedLabel?.substringBeforeLast("\n")
    justAuthor = decodedLabel?.substringAfterLast("\t")
    //Log.d("ChosenBook", "justTitle: $justTitle, justAuthor: $justAuthor")


    val context = LocalContext.current
    var downloadId by remember { mutableStateOf(-1L) } // Store the download ID

    // Create the receiver
    val downloadCompletedReceiver = remember(navController) { // Ensure receiver is stable
        DownloadCompletedReceiver(navController)
    }

    // Log.d("ChosenBook", "downloadCompletedReceiver 1: $downloadCompletedReceiver")

    // Effect to register and unregister the receiver
    DisposableEffect(Unit) {
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            downloadCompletedReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(downloadCompletedReceiver)
        }
    }
    //Log.d("ChosenBook", "downloadCompletedReceiver 2: $downloadCompletedReceiver")



    /**
     * Initiates an Android Sharesheet to share text content.
     *
     * @param context The current [Context].
     * @param textToShare The [String] content to be shared.
     * @param subject An optional [String] subject for the shared content (e.g., for email).
     */
    fun shareText(context: Context, textToShare: String, subject: String = "") {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textToShare)
            if (subject.isNotBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            type = "text/plain"

            // This flag is a hint to the system to launch the new activity
            // adjacent to the current one, which can improve the back stack
            // behavior, especially in multi-window environments. It makes
            // the new task part of the same overall user journey.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }

        // Create a chooser to always show the Android Sharesheet
        // This gives the user a choice of which app to share with
        val shareIntent = Intent.createChooser(sendIntent, null /* Chooser Title */)

        // Verify that the intent will resolve to an activity
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(shareIntent)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        //Log.d("ChosenBook", "downloadId in box: $downloadId")

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Gutenberg Web Access!",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                    Text(text = "Home")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        navController.popBackStack()
                    },
                ) {
                    Text(text = "Back")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.SavedBooks.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Text(text = "Saved Books")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$justTitle" ?: if (justTitle == null) "Loading..." else "-no title-",
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "by ${justAuthor}" ?: if (justAuthor == null) "Loading..." else "-no author-",
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row() {

                    // Set up the permission state requester
                    val storagePermissionState = rememberPermissionState(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    Button(
                        onClick = {

                            // Check if we are on Android 9 (API 28) or older
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                if (storagePermissionState.status.isGranted) {
                                    // Permission is already granted, proceed with download
                                    startDownload(
                                        context,
                                        textNumber,
                                        justTitle,
                                        viewModel,
                                        audioViewModel
                                    ) { id ->
                                        downloadId = id
                                    }
                                } else {
                                    // Permission not granted, launch the request
                                    storagePermissionState.launchPermissionRequest()
                                }
                            } else {
                                // On Android 10+ (API 29+), no permission needed for DownloadManager
                                startDownload(
                                    context,
                                    textNumber,
                                    justTitle,
                                    viewModel,
                                    audioViewModel
                                ) { id ->
                                    downloadId = id
                                }


                            }
                        }
                    ) {
                        Text(text = "Download & Save")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Share",
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    // val context = LocalContext.current
                    Button(
                        onClick = {
                            shareText(
                                context,
                                "https://www.gutenberg.org/cache/epub/${textNumber}/pg${textNumber}-images.html\n",
                                subject = decodedLabel
                            )
                        },
                    ) {
                        Text("Html Link")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            shareText(
                                context,
                                "https://www.gutenberg.org/ebooks/${textNumber}.txt.utf-8\n",
                                subject = decodedLabel
                                    ?: "-no title-"
                            )
                        },
                    ) {
                        Text("Text Link")
                    }

                }
            }
        }
    }

}

