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

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.DownloadCompletedReceiver
import com.myhobby.gutenbergwebaccess.NavRoutes
import com.myhobby.gutenbergwebaccess.R
import com.myhobby.gutenbergwebaccess.util.getMetaTagContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


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
    }

    // Create a chooser to always show the Android Sharesheet
    // This gives the user a choice of which app to share with
    val shareIntent = Intent.createChooser(sendIntent, null /* Chooser Title */)

    // Verify that the intent will resolve to an activity
    if (shareIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(shareIntent)
    }
}


/**
 * Enqueues a single audio file for download using Android's [DownloadManager]. *
 * This function constructs a download request with the specified URL and saves the file
 * to a dedicated subfolder within the public "Downloads/Gutenbergwebaccess" directory.
 * It configures the download to be visible in the system's notification tray and allows
 * downloading over metered connections.
 *
 * @param context The application [Context], used to access system services like [DownloadManager].
 * @param folderName The name of the subfolder within "Gutenbergwebaccess" where the file will be saved (e.g., the book's title).
 * @param fileName The name of the destination file (e.g., "chapter_01.ogg").
 * @param fileUrl The remote URL of the audio file to be downloaded.
 * @param downloadManager An instance of the system's [DownloadManager].
 * @param onDownloadEnqueued A callback function that is invoked with the unique download ID
 *                           once the download has been successfully enqueued. This ID can be
 *                           used to track the download's status.
 * @param onError A callback function that is invoked with an error message if an exception
 *                occurs while trying to enqueue the download.
 */
fun downloadAudioFile(
    context: Context,
    folderName: String,
    fileName: String,
    fileUrl: String,
    downloadManager: DownloadManager,
    onDownloadEnqueued: (Long) -> Unit, // Callback to store download ID
    onError: (String) -> Unit
) {
    try {
        val request = DownloadManager.Request(fileUrl.toUri())
            .setTitle(fileName)
            .setDescription("Downloading $fileName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Gutenbergwebaccess/${folderName}/${fileName}"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        onDownloadEnqueued(downloadId) // Pass the download ID back

    } catch (e: Exception) {
        onError("Download failed for $fileName: ${e.message}")
        e.printStackTrace()
    }
}


/**
 * Asynchronously checks if a given URL points to an accessible resource.
 *
 * This suspend function performs a network request on a background thread (`Dispatchers.IO`)
 * to verify the validity of a URL. It sends an HTTP HEAD request, which is efficient
 * as it only retrieves the headers and not the full content of the file.
 *
 * A URL is considered valid if the server responds with an HTTP status code in the
 * 2xx range (e.g., 200 OK), indicating success. The function includes timeouts to prevent
 * indefinite waiting and handles common network exceptions.
 *
 * @param fileUrlString The complete URL of the file to be checked.
 * @return `true` if the server responds with a 2xx success code, `false` otherwise
 *         (e.g., 404 Not Found, network error, timeout, or malformed URL).
 */
suspend fun fileUrlIsValid(fileUrlString: String): Boolean {
    // Ensure this function is called from a background thread (e.g., Dispatchers.IO)
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(fileUrlString)
            connection = url.openConnection() as HttpURLConnection
            // Set request method to HEAD to get only headers, not the full content
            connection.requestMethod = "HEAD"
            // Set timeouts to prevent indefinite blocking
            connection.connectTimeout = 5000 // 5 seconds
            connection.readTimeout = 5000    // 5 seconds
            connection.instanceFollowRedirects = true // Follow redirects

            // Perform the connection (implicitly done by getResponseCode)
            val responseCode = connection.responseCode

            // Log the URL and response code for debugging
            //Log.d("FileUrlCheck", "URL: $fileUrlString, Response Code: $responseCode")

            // Consider 2xx status codes as valid/existing
            // Common success codes:
            // 200 OK: Standard success
            // 201 Created
            // 202 Accepted
            // 204 No Content
            // You might want to be more specific based on expected server behavior.
            // For checking existence, 200 OK is the most common.
            return@withContext responseCode == HttpURLConnection.HTTP_OK ||
                    (responseCode >= 200 && responseCode < 300) // General success range

        } catch (e: Exception) {
            // Handle exceptions like MalformedURLException, IOException, etc.
            Log.e("FileUrlCheck", "Error checking URL $fileUrlString: ${e.message}", e)
            return@withContext false
        } finally {
            connection?.disconnect()
        }
    }
}


/**
 * A screen that displays details and actions for a selected audiobook.
 *
 * This composable function serves as the main user interface for interacting with an audiobook.
 * It dynamically fetches the book's title and author from its Project Gutenberg webpage
 * using a web scraping utility. The screen provides the user with options to download all
 * associated audio files (e.g., OGG chapters), navigate to other parts of the app, or
 * share information about the book.
 *
 * The download process is managed using Android's [DownloadManager]. The function iterates
 * through potential chapter files, verifies their existence on the server, and enqueues them
 * for download. A [DownloadCompletedReceiver] is registered to listen for system broadcasts,
 * allowing the app to react once downloads are finished.
 *
 * Key Features:
 * - Asynchronously fetches and displays the book's title and author.
 * - Provides a "Download All Chapters" button that intelligently finds and downloads all
 *   available OGG audio files for the book.
 * - Manages download state and registers a broadcast receiver to handle download
 *   completion events.
 * - Offers standard navigation controls ("Home", "Back", "Saved Books").
 * - Includes a "Share" button to send book information to other apps.
 *
 * @param navController The [NavController] instance used for handling navigation events,
 *   such as going home or back.
 * @param label The URL-encoded title or label of the book, passed as a navigation argument.
 *   This is decoded and displayed to the user.
 * @param url The URL-encoded relative path of the book on the Gutenberg website, passed
 *   as a navigation argument.
 * @param textNumber The unique numeric ID of the book on Project Gutenberg. This is the primary
 *   identifier used for fetching metadata and constructing download URLs.
 */
@SuppressLint("MissingPermission")
@Composable
fun ChosenAudioBook(
    navController: NavController,
    label: String?,
    url: String?,
    textNumber: Int,
) {

    val decodedLabel = remember(label) {
        try {
            URLDecoder.decode(label, StandardCharsets.UTF_8.toString()).substringBefore("\n")
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

    // Log.d("ChosenAudioBook", "decoded label: $decodedLabel")
    // Log.d("ChosenAudioBook", "decoded url: $decodedUrl")
    // Log.d("ChosenAudioBook", "textNumber: $textNumber")

    var titleAuthor by remember { mutableStateOf<String?>(null) }
    var justAuthor by remember { mutableStateOf<String?>(null) }

    // This effect runs once when the composable enters the screen
    // It will re-run if the `textNumber` changes
    LaunchedEffect(key1 = textNumber) {
        val bookPageUrl = "https://www.gutenberg.org/ebooks/$textNumber"
        // Launch two parallel jobs to fetch author and subject
        launch {
            titleAuthor = getMetaTagContent(bookPageUrl, "title")
        }
    }

    justAuthor = titleAuthor?.substringAfterLast("by ")
    // Log.d("ChosenBook", "justAuthor: $justAuthor")


    val context = LocalContext.current
    val downloadIds = remember { mutableStateOf<List<Long>>(emptyList()) }

    // Get a coroutine scope tied to the composable's lifecycle
    val coroutineScope = rememberCoroutineScope()

    // Create the receiver
    val downloadCompletedReceiver = remember(navController) { // Ensure receiver is stable
        DownloadCompletedReceiver(navController)
    }


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

    /**
     * Initiates an Android Sharesheet to share audio content.
     *
     * @param context The current [Context].
     * @param audioToShare The [String] content to be shared.
     * @param subject An optional [String] subject for the shared content (e.g., for email).
     */
    fun shareAudio(context: Context, audioToShare: String, subject: String = "") {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, audioToShare)
            if (subject.isNotBlank()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            type = "audio/ogg"

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
                        navController.navigate(NavRoutes.SavedBooks.route){
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
                    text = decodedLabel,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "by ${justAuthor ?: if (justAuthor == null) "Loading..." else "-no author-"}",
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row() {
                    Button(
                        onClick = {
                            val downloadManager =
                                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

                            val baseFileName = decodedLabel
                            // allDataList.find { it.textNumber == textNumber }?.title ?: "-no title-"
                            val sanitizedBaseFileName =
                                baseFileName.replace(".", "").replace(":", " ").replace(" ", "_")

                            val numberOfFiles = 100 // EXAMPLE: Replace with actual logic
                            var breakOut = false

                            val currentDownloads = mutableListOf<Long>()

                            // Launch a coroutine for the network operations
                            // Assuming oggPositionViewModel is available and has a viewModelScope
                            coroutineScope.launch @androidx.annotation.RequiresPermission(
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) { // Or another appropriate scope

                                val notificationManager =
                                    NotificationManagerCompat.from(context)
                                val notificationId =
                                    12345 // Unique ID for your summary notification
                                val YOUR_CHANNEL_ID = "12345"
                                val builder = NotificationCompat.Builder(context, YOUR_CHANNEL_ID)
                                    .setContentTitle("Downloading Audio Book")
                                    .setContentText("Preparing to download files...")
                                    .setSmallIcon(R.drawable.gutenbergsplashscreen) // Replace with better icon
                                    .setPriority(NotificationCompat.PRIORITY_LOW) // Or DEFAULT
                                    .setOngoing(true) // Makes the notification not easily dismissable
                                    .setOnlyAlertOnce(true) // Crucial to avoid repeated sounds/vibrations

                                notificationManager.notify(notificationId, builder.build())

                                val folderName =
                                    "/storage/emulated/0/Download/Gutenbergwebaccess/" + sanitizedBaseFileName

                                for (i in 1..numberOfFiles) {
                                    if (breakOut) {
                                        //Log.d("DownloadLoop", "Breaking out of loop due to previous error.")
                                        break
                                    }

                                    val fileIndex = String.format("%02d", i)
                                    val fileUrl =
                                        "https://www.gutenberg.org/files/${textNumber}/ogg/${textNumber}-${fileIndex}.ogg"
                                    val fileName = "${sanitizedBaseFileName}_${fileIndex}.ogg"

                                    //Log.d("DownloadLoop", "Checking URL: $fileUrl")
                                    if (fileUrlIsValid(fileUrl)) { // Call the suspend function
                                        //Log.d("DownloadLoop", "URL is valid, attempting download: $fileName")

                                        // Consider if downloadAudioFile needs to be called on a specific dispatcher
                                        // For now, assuming it's safe as is, or handles its own threading.
                                        downloadAudioFile(
                                            context = context,
                                            folderName = sanitizedBaseFileName,
                                            fileUrl = fileUrl,
                                            fileName = fileName,
                                            downloadManager = downloadManager,
                                            onDownloadEnqueued = { id ->
                                                currentDownloads.add(id)
                                                // Update MutableState on the main thread
                                                launch(Dispatchers.Main) {
                                                    downloadIds.value = currentDownloads.toList()
                                                }

                                            },
                                            onError = { error ->
                                                Log.e(
                                                    "DownloadError",
                                                    "Error for $sanitizedBaseFileName/$fileName: $error. Stopping further attempts."
                                                )
                                                launch(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "Downloads complete. Stopping.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                breakOut = true
                                            }
                                        )
                                    } else {
                                        Log.w(
                                            "DownloadLoop",
                                            "URL not found or invalid, skipping: $fileUrl"
                                        )
                                        // This Toast should ideally be on the Main thread
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Last audio file reached. Complete download may take some time.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        // If a file in the sequence is missing, you probably want to stop.
                                        breakOut = true
                                    }
                                } // end of for loop

                                if (currentDownloads.isNotEmpty() && !breakOut) { // Only show if downloads actually started and didn't break immediately
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Download of ${currentDownloads.size} files initiated.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else if (currentDownloads.isEmpty() && !breakOut) {
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "No audio files found or initiated for download.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                //Log.d("DownloadLoop", "Finished download processing loop.")
                            } // end of coroutine launch
                        },
                        enabled = downloadIds.value.isEmpty(),
                        // modifier = Modifier.semantics { contentDescription = "Download & Save all parts of the Audio Book" }
                    ) {
                        Text(text = "Download & Save All Parts")
                    }

                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Share",
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))


                Button(
                    onClick = {
                        shareText(
                            context,
                            "https://www.gutenberg.org/files/${textNumber}/ogg/\n",
                            subject = decodedLabel
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Share Ogg Audio Book Link"
                    }
                ) {
                    Text("Audio Link")
                }

            }
        }
    }
}

