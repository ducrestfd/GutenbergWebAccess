package com.myhobby.gutenbergwebaccess

/*
Gutenberg Access's raison d'être is to provide simple access to
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

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.navigation.NavController


/**
 * A [BroadcastReceiver] that listens for download completion events from the [DownloadManager].
 *
 * When a download initiated by the application completes successfully, this receiver
 * displays a "Download Complete!" toast message and then navigates the user to the
 * `NavRoutes.SavedBooks.route` screen using the provided [NavController].
 *
 * This receiver needs to be registered (and unregistered) by the component that
 * initiates the download (e.g., an Activity or Composable screen) to listen for
 * the `DownloadManager.ACTION_DOWNLOAD_COMPLETE` intent.
 *
 * @param navController The [NavController] instance used to navigate to the
 *                      "Saved Books" screen after a download is complete.
 */
class DownloadCompletedReceiver(private val navController: NavController) : BroadcastReceiver() {

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
     *
     * It checks if the received action is `DownloadManager.ACTION_DOWNLOAD_COMPLETE`.
     * If it is, it retrieves the ID of the completed download. If the download ID is valid,
     * it shows a toast notification and navigates to the "Saved Books" screen.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (downloadId != -1L) {
                Toast.makeText(context, "Download Complete!", Toast.LENGTH_SHORT).show()
                navController.navigate(NavRoutes.SavedBooks.route) {
                    // Optional: Configure navigation options, like popping the current screen
                    // popUpTo(NavRoutes.Home.route) // Or your previous screen
                    launchSingleTop = true // Prevents multiple instances of the same screen
                }
            }
        }
    }
}