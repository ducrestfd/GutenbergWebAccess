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

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.viewmodels.ScrollLocationViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.layout.onSizeChanged


/**
 * A Composable screen that displays an HTML book using a [WebView].
 *
 * This screen is responsible for loading and showing the content of a local HTML file.
 * It features a top app bar with a title derived from the filename and a back button
 * to navigate to the previous screen.
 *
 * The [WebView] is configured to enable JavaScript and allow file access, which is
 * crucial for loading local HTML files. It also uses a [WebViewClient] to ensure
 * that links within the HTML content are opened within the same [WebView] rather
 * than an external browser.
 *
 * This screen integrates with a [ScrollLocationViewModel] to:
 * 1. Restore the last known scroll position when the book is opened.
 * 2. Save the current scroll position when the user scrolls through the book.
 *
 * The scroll position is calculated as a percentage of the WebView's content height
 * to attempt a more consistent restoration across different screen sizes or content
 * loading variations, though this can be challenging with WebView's dynamic content.
 *
 * If the provided file path is invalid, or the file does not exist or is not an
 * HTML file, an error message is displayed.
 *
 * @param navController The [NavController] used for navigation, primarily for the back button.
 * @param viewModel The [ScrollLocationViewModel] instance for saving and retrieving
 *                  the scroll position of the book.
 * @param encodedFilePath A URL-encoded [String] representing the absolute path to the
 *                        local HTML file to be displayed. This path is decoded internally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlBookViewer(
    navController: NavController,
    viewModel: ScrollLocationViewModel,
    encodedFilePath: String?
) {

    var locY: Int = 0;

    val filePath = remember(encodedFilePath) {
        encodedFilePath?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
    }


    /* Defines the basic visual layout for the HTML book viewer screen.
    *
    * This [Scaffold] provides the standard Material Design structure, including a top
    * app bar and the main content area.
    *
    * - `topBar`: Configured with a [TopAppBar] that displays the book's filename as
    *   a title and provides a navigation icon ([Icons.AutoMirrored.Filled.ArrowBack])
    *   to allow the user to easily return to the previous screen.
    *
    * - `content`: The main body of the scaffold, which contains a [Column] that either
    *   displays the [AndroidView] wrapping the [WebView] for rendering the book, or
    *   shows an error message if the file path is invalid or the file cannot be found.
    */
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = filePath?.substringAfterLast('/') ?: "Book") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Previous Screen"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filePath != null) {
                val bookFile = File(filePath)
                if (bookFile.exists() && (bookFile.extension.equals(
                        "html",
                        ignoreCase = true
                    ) || bookFile.extension.equals("htm", ignoreCase = true))
                ) {

                    var webViewHeight by remember { mutableStateOf(0) }


                    /**
                     * An [AndroidView] Composable used to embed a classic Android [WebView] within the
                     * Jetpack Compose UI.
                     *
                     * This is the core component for rendering the HTML book content. It is configured
                     * via its `factory` lambda, which runs once to create and initialize the WebView.
                     *
                     * **Initialization (`factory`):**
                     * - Creates a [WebView] instance.
                     * - Enables JavaScript and local file access, which are essential for rendering
                     *   local book files correctly.
                     * - Assigns a custom [WebViewClient] to handle page loading events, resource
                     *   errors, and URL overrides. This client is responsible for restoring scroll
                     *   position, handling missing images, and intercepting external links.
                     *
                     * **Update (`update`):**
                     * - This block runs whenever the `filePath` changes.
                     * - It loads the specified local HTML file into the WebView using `loadUrl`,
                     *   triggering the entire page rendering and progress restoration process.
                     */
                    AndroidView(
                        factory = { ctx -> // Renamed context to ctx to avoid conflict
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.allowFileAccess = true
                                webViewClient = object : WebViewClient() {

                                    // This method is called when a new URL is about to be loaded in the WebView
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString()
                                        if (url != null) {
                                            // Only override for http or https schemes.
                                            // You might want to adjust this if you expect other schemes
                                            // to be handled by external apps (e.g., mailto:, tel:).
                                            // For local file links (file://), you typically want them to load
                                            // within the WebView if they are part of the book's content.
                                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                                try {
                                                    val intent =
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    context.startActivity(intent) // Use the context from LocalContext
                                                    return true // Indicate that we've handled the URL
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "HtmlBookViewer",
                                                        "Could not open link $url",
                                                        e
                                                    )
                                                    // Optionally, inform the user that the link couldn't be opened
                                                }
                                            }
                                        }
                                        // For other cases (e.g., local file links, or if URL is null),
                                        // let the WebView handle it by returning false.
                                        return false
                                    }

                                    /**
                                     * A callback invoked when the WebView begins to load a page.
                                     *
                                     * This method is part of the [WebViewClient] lifecycle. While it is not
                                     * currently used for complex logic in this implementation, it serves as a
                                     * placeholder for any actions that might be needed as soon as page loading
                                     * commences, such as displaying a loading indicator.
                                     *
                                     * @param view The WebView that is starting to load.
                                     * @param url The URL of the page that is starting to load.
                                     * @param favicon The favicon for the page, or `null` if it is not available.
                                     */
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                    }

                                    /**
                                    * A callback invoked when the WebView has finished loading the page.
                                    *
                                    * This function is the primary mechanism for restoring the user's
                                    * last known reading position. Once the page content is fully rendered,
                                    * it launches a coroutine to:
                                    * 1. Asynchronously fetch the saved scroll location for the current book
                                    *    from the [ScrollLocationViewModel].
                                    * 2. If a location is found, it calculates the precise pixel value (`scrollY`)
                                    *    to scroll to, based on the saved position and the WebView's content height.
                                    * 3. It then programmatically scrolls the WebView to that exact position,
                                    *    seamlessly returning the user to where they left off.
                                    *
                                    * This ensures a smooth and continuous reading experience across sessions.
                                    *
                                    * @param view The WebView that has finished loading.
                                    * @param url The URL of the page that has finished loading.
                                    */
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        viewModel.viewModelScope.launch {
                                            val locationDeferred =
                                                viewModel.getScrollLocationAsync(bookFile.name)
                                            val location = locationDeferred.await()
                                            if (location != null && webViewHeight > 0) {
                                                val contentHeight =
                                                    view?.contentHeight ?: webViewHeight
                                                val scrollPercentage =
                                                    location.locy / contentHeight.toFloat()
                                                val targetScrollY =
                                                    (scrollPercentage * contentHeight).toInt()
                                                val maxScrollY = contentHeight - webViewHeight
                                                val finalScrollY =
                                                    if (maxScrollY > 0) targetScrollY.coerceIn(
                                                        0,
                                                        maxScrollY
                                                    ) else 0
                                                view?.scrollTo(0, finalScrollY)
                                                locY = finalScrollY
                                            }
                                        }
                                    }

                                    /**
                                     * Handles errors encountered while loading resources within the WebView.
                                     *                                     * This overridden method is specifically tailored to gracefully handle
                                     * "404 Not Found" errors for image resources, which are common in
                                     * scraped HTML files. Instead of showing a broken image icon,
                                     * this function injects JavaScript to hide the missing image element,
                                     * providing a cleaner reading experience.
                                     *
                                     * It checks if the failed request is for a common image type (jpg, png, etc.)
                                     * and is not for the main HTML frame itself. If these conditions are met,
                                     * it finds the corresponding `<img>` tag in the DOM by its `src` and sets
                                     * its display style to 'none'.
                                     *
                                     * All other errors are passed to the superclass for default handling.
                                     *
                                     * @param view The WebView that is initiating the callback.
                                     * @param request The request that failed.
                                     * @param error The error that occurred.
                                     */
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        // Check if the error is for an image and it's not for the main frame
                                        if (request?.url?.toString()
                                                ?.endsWith(".jpg", ignoreCase = true) == true ||
                                            request?.url?.toString()
                                                ?.endsWith(".jpeg", ignoreCase = true) == true ||
                                            request?.url?.toString()?.endsWith(
                                                ".png",
                                                ignoreCase = true
                                            ) == true || request?.url?.toString()
                                                ?.endsWith(".gif", ignoreCase = true) == true ||
                                            request?.url?.toString()
                                                ?.endsWith(".webp", ignoreCase = true) == true
                                        ) {

                                            if (request.isForMainFrame == false) {
                                                Log.w(
                                                    "HtmlBookViewer",
                                                    "Failed to load image resource: ${request.url}, Error: ${error?.description}"
                                                )
                                                // Attempt to hide the broken image using JavaScript
                                                // This is a common approach, but its effectiveness can vary based on HTML structure
                                                // and WebView version.
                                                view?.evaluateJavascript(
                                                    """
                (function() {
                    var imgs = document.getElementsByTagName('img');
                    for (var i = 0; i < imgs.length; i++) {
                        if (imgs[i].src === '${request.url}') {
                            imgs[i].style.display='none';
                            // You could also set visibility to 'hidden' or replace the src
                            // with a transparent pixel, depending on desired behavior.
                            break;
                        }
                    }
                })();
                """.trimIndent(),
                                                    null
                                                )
                                                return // Prevent further processing by the WebView for this error
                                            }
                                        }
                                        // For other errors or main frame errors, call the superclass method
                                        super.onReceivedError(view, request, error)
                                    }

                                }

                                loadUrl("file://${bookFile.absolutePath}")


                                /**
                                 * Attaches a listener to the WebView's ViewTreeObserver to detect scroll changes.
                                 *
                                 * This listener is the core mechanism for saving the user's reading progress.
                                 * It fires every time the user scrolls the [WebView].
                                 *
                                 * Inside the listener:
                                 * 1. It gets the current vertical scroll position (`scrollY`).
                                 * 2. It compares this to the last known position (`locY`) to avoid redundant saves.
                                 * 3. If the position has changed, it updates the local `locY` state.
                                 * 4. It then launches a coroutine in the `viewModelScope` to update the persistent
                                 *    storage with the new scroll position via the [ScrollLocationViewModel].
                                 *
                                 * This ensures that the user's progress is continuously and efficiently saved
                                 * as they read through the book.
                                 */
                                this.viewTreeObserver.addOnScrollChangedListener {
                                    val currentScrollY = this.scrollY
                                    if (locY != currentScrollY) {
                                        locY = currentScrollY
                                        viewModel.viewModelScope.launch {
                                            val currentContentHeight = this@apply.contentHeight
                                            if (currentContentHeight > 0) {
                                                val locationData =
                                                    viewModel.getScrollLocationAsync(bookFile.name)
                                                        .await()
                                                if (locationData != null) {
                                                    locationData.locy = locY
                                                    viewModel.updateScrollLocation(locationData)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                webViewHeight =
                                    size.height // Height of the AndroidView, which wraps WebView
                            }
                    )
                } else {
                    Text("Error: HTML Book file not found or invalid.")
                }
            } else {
                Text("Error: File path not provided.")
            }
        }
    }
}
