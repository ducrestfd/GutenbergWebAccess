package com.myhobby.gutenbergwebaccess

/*
Gutenberg Access's raison d'être is to provide simple access to
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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myhobby.gutenbergwebaccess.screens.About
import com.myhobby.gutenbergwebaccess.screens.AccessibilityStatement
import com.myhobby.gutenbergwebaccess.screens.BookChoices
import com.myhobby.gutenbergwebaccess.screens.Browse
import com.myhobby.gutenbergwebaccess.screens.ChosenBook
import com.myhobby.gutenbergwebaccess.screens.Home
import com.myhobby.gutenbergwebaccess.screens.SavedBooks
import com.myhobby.gutenbergwebaccess.screens.BookListing
import com.myhobby.gutenbergwebaccess.screens.BrowseSection
import com.myhobby.gutenbergwebaccess.screens.ChosenAudioBook
import com.myhobby.gutenbergwebaccess.screens.OggPlayer
import com.myhobby.gutenbergwebaccess.screens.AudioPlayList
import com.myhobby.gutenbergwebaccess.screens.DirectResults
import com.myhobby.gutenbergwebaccess.screens.PrivacyPolicy
import com.myhobby.gutenbergwebaccess.screens.Settings
import com.myhobby.gutenbergwebaccess.ui.theme.GutenbergAccessTheme
import com.myhobby.gutenbergwebaccess.viewmodels.ScrollLocationViewModel
import com.myhobby.gutenbergwebaccess.screens.TextToSpeechBookReader
import com.myhobby.gutenbergwebaccess.util.HtmlText
import com.myhobby.gutenbergwebaccess.viewmodels.AudioLocationViewModel
import com.myhobby.gutenbergwebaccess.viewmodels.DirectResultsViewModel
import com.myhobby.gutenbergwebaccess.viewmodels.OggPlaybackViewModel




/**
 * The main and only activity for the Gutenberg Web Access application.
 *
 * This activity serves as the host for the entire Jetpack Compose UI. It is responsible for
 * setting up the application's core components, including the UI theme, navigation, and
 * all major ViewModels. By creating the ViewModels here with an activity-level scope,
 * it ensures that a single, shared instance of each ViewModel is passed down through the
 * navigation graph, allowing different screens to share and preserve state seamlessly.
 *
 * The `onCreate` method handles the initialization of the splash screen, ViewModels,
 * and sets the root composable, [MainScreen], as the activity's content.
 */
class MainActivity : ComponentActivity() {


    /**
     * The entry point for the activity's lifecycle.
     *
     * This method is called when the activity is first created. It is responsible for:
     * 1.  Initializing and displaying the splash screen via `installSplashScreen()`.
     * 2.  Setting up all the major ViewModels ([ScrollLocationViewModel], [AudioLocationViewModel],
     *     [OggPlaybackViewModel], [DirectResultsViewModel]) with an activity-level scope. This
     *     ensures that the same ViewModel instances are shared across all Composables managed
     *     by this activity, preserving state during navigation and configuration changes.
     * 3.  Enabling edge-to-edge display for a modern, immersive UI.
     * 4.  Setting the main content of the activity to the [GutenbergAccessTheme] and rendering
     *     the [MainScreen] Composable, passing in the freshly created ViewModel instances to
     *     bootstrap the application's UI and state management.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously
     *   being shut down, this [Bundle] contains the data it most recently supplied in
     *   `onSaveInstanceState(Bundle)`. Otherwise, it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val scrollLocationViewModel = ViewModelProvider(this)[ScrollLocationViewModel::class.java]
        val audioLocationViewModel = ViewModelProvider(this)[AudioLocationViewModel::class.java]

        val oggPlaybackViewModel = ViewModelProvider(this)[OggPlaybackViewModel::class.java]
        val directResultsViewModel = ViewModelProvider(this)[DirectResultsViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            GutenbergAccessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        scrollLocationViewModel,
                        audioLocationViewModel,
                        oggPlaybackViewModel,
                        directResultsViewModel
                    )
                }
            }
        }
    }

}



/**
 * The root Composable that sets up the application's navigation structure.
 *
 * This function initializes a [rememberNavController] and configures a [NavHost] to define
 * the navigation graph for the entire application. It acts as a central hub, routing users
 * to various screens like [Home], [Settings], [OggPlayer], and [TextToSpeechBookReader].
 *
 * It receives all major ViewModels as parameters from the `MainActivity` and passes them
 * down to the appropriate composable destinations, ensuring that state is shared correctly
 * across the app.
 *
 * @param viewModel The [ScrollLocationViewModel] used for managing and persisting scroll positions
 *   in text-based book viewers like [HtmlBookViewer].
 * @param audioViewModel The [AudioLocationViewModel] responsible for managing the state
 *   (e.g., current sentence, rate, pitch) for the [TextToSpeechBookReader].
 * @param oggPlaybackViewModel The [OggPlaybackViewModel] which manages state for OGG audio books,
 *   including track position, speed, and chapter information for the [OggPlayer] and [AudioPlayList].
 *   It also handles saving global app settings.
 * @param directResultsViewModel The [DirectResultsViewModel] used for managing and paginating
 *   search results from the Gutenberg website, used by screens like [DirectResults] and [BookListing].
 */

@Composable
fun MainScreen(
    viewModel: ScrollLocationViewModel,
    audioViewModel: AudioLocationViewModel,
    oggPlaybackViewModel: OggPlaybackViewModel,
    directResultsViewModel: DirectResultsViewModel
    )
{

    // Creates and remembers a NavController for managing app navigation.
    val navController = rememberNavController()

    // NavHost defines the navigation graph.
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {

        // Defines the route for the Home screen.
        composable(NavRoutes.Home.route) {
            Home(navController = navController, directResultsViewModel)
        }

        //composable(NavRoutes.Saved.route) {
        //    Saved(navController = navController)
        //}

        // Defines the route for the Search screen, with a dynamic "typeOfSearch" argument.
        composable(NavRoutes.DirectResults.route + "/{typeOfSearch}/{searchTerm}") { backStackEntry ->
            val typeOfSearch = backStackEntry.arguments?.getString("typeOfSearch")
            val searchTerm = backStackEntry.arguments?.getString("searchTerm")

            // Pass the ViewModel to the screen
            DirectResults(
                navController = navController,
                viewModel = directResultsViewModel, // Pass the ViewModel
                searchType = typeOfSearch,
                searchTerm = searchTerm
            )
        }

        // Defines the route for the Browse screen, with a dynamic "typeOfBrowse" argument.
        composable(NavRoutes.Browse.route + "/{typeOfBrowse}") { backStackEntry ->
            val typeOfBrowse = backStackEntry.arguments?.getString("typeOfBrowse")
            Browse(navController = navController, typeOfBrowse)
        }


        // Defines the route for the ChosenBook screen, with a dynamic "textNumber" argument.
        composable(
            // Define arguments in the route pattern
            route = NavRoutes.ChosenBook.route + "/{label}/{url}/{textNumber}",
            arguments = listOf(
                navArgument("label") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("textNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            // Retrieve the arguments in the destination
            val label = backStackEntry.arguments?.getString("label") ?: ""
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val textNumber = backStackEntry.arguments?.getInt("textNumber") ?: -1

            ChosenBook(
                navController = navController,
                viewModel,
                audioViewModel,
                label = label, // Pass the encoded label
                url = url,     // Pass the encoded url
                textNumber = textNumber
            )
        }


        // Defines the route for the ChosenAudioBook screen, with a dynamic "textNumber" argument.
        composable(
            // Define arguments in the route pattern
            route = NavRoutes.ChosenAudioBook.route + "/{label}/{url}/{textNumber}",
            arguments = listOf(
                navArgument("label") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType },
                navArgument("textNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            // Retrieve the arguments in the destination
            val label = backStackEntry.arguments?.getString("label") ?: ""
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val textNumber = backStackEntry.arguments?.getInt("textNumber") ?: -1

            ChosenAudioBook(
                navController = navController,
                label = label, // Pass the encoded label
                url = url,     // Pass the encoded url
                textNumber = textNumber
            )
        }

        // Defines the route for the SavedBooks screen.
        composable(NavRoutes.SavedBooks.route) {
            SavedBooks(navController = navController, viewModel)
        }

        // Defines the route for the BookChoices screen, with a dynamic "book" (filename) argument.
        // Sets up reading, listening via speech to text or viewing a book.
        composable(NavRoutes.BookChoices.route + "/{book}") { backStackEntry ->
            val book = backStackEntry.arguments?.getString("book")
            BookChoices(navController = navController, viewModel, oggPlaybackViewModel, book)
        }

        // Defines the route for the BookChoices screen, with a dynamic "book" (filename) argument.
        // Sets up listening to an audio book.
        composable(NavRoutes.BookChoices.route + "/{book}/") { backStackEntry ->
            val book = backStackEntry.arguments?.getString("book")
            BookChoices(navController = navController, viewModel, oggPlaybackViewModel, book+"/")
        }

        // Defines the route for the About screen.
        composable(NavRoutes.About.route) {
            About(navController = navController)
        }

        // Defines the route for the BrowseSection screen, with dynamic "typeOfBrowse" and "subSection" arguments.
        composable(NavRoutes.BrowseSection.route + "/{typeOfBrowse}/{subSection}") { backStackEntry ->
            val typeOfBrowse = backStackEntry.arguments?.getString("typeOfBrowse")
            val subSection = backStackEntry.arguments?.getString("subSection")
            BrowseSection(navController = navController, typeOfBrowse, subSection)
        }

        // Defines the route for the BookListing screen, with dynamic "typeOfBrowse", "label", and "shelfNumber" arguments.
        composable(NavRoutes.BookListing.route + "/{typeOfBrowse}/{label}/{shelfNumber}") { backStackEntry ->
            val typeOfBrowse = backStackEntry.arguments?.getString("typeOfBrowse")
            val label =
                backStackEntry.arguments?.getString("label") // Retrieve the (potentially) encoded label
            val shelfNumber = backStackEntry.arguments?.getString("shelfNumber")
            BookListing(
                navController = navController,
                directResultsViewModel,
                typeOfBrowse = typeOfBrowse,
                label = label,
                shelfNumber = shelfNumber)
        }

        // Defines the route for the TextToSpeechBookReader screen, with an "encodedFilePath" argument.
        composable(
            route = "${NavRoutes.TextToSpeechBookReader.route}/{encodedFilePath}/{languageCode}",
            arguments = listOf(navArgument("encodedFilePath") { type = NavType.StringType }),
        ){ backStackEntry ->
            TextToSpeechBookReader(
                navController = navController,
                viewModel = audioViewModel,
                encodedFilePath = backStackEntry.arguments?.getString("encodedFilePath"),
                languageCode = backStackEntry.arguments?.getString("languageCode")
            )
        }

        // Defines the route for the AudioPlayList screen.
        composable(
            route = "${NavRoutes.AudioPlayList.route}/{encodedFolderPath}",
            arguments = listOf(navArgument("encodedFolderPath") { type = NavType.StringType })
        ) { backStackEntry ->
            AudioPlayList(
                navController = navController,
                oggPlaybackViewModel,
                oggFolderPath = backStackEntry.arguments?.getString("encodedFolderPath")
            )
        }

        // Defines the route for the OggPlayer screen.
        composable(
            route = "${NavRoutes.OggPlayer.route}/{oggFolderPath}/{currentChapterNumber}",
            arguments = listOf(
                navArgument("oggFolderPath") { type = NavType.StringType },
                navArgument("currentChapterNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            OggPlayer(
                navController = navController,
                oggPlaybackViewModel,
                oggFolderPath = backStackEntry.arguments?.getString("oggFolderPath"),
                currentChapterNumber = backStackEntry.arguments?.getInt("currentChapterNumber")
            )
        }

        // Defines the route to the Settings screen.
        composable("settings") {
            Settings(
                navController = navController,
                oggPlaybackViewModel = oggPlaybackViewModel // Pass the existing ViewModel
            )
        }

        composable("privacy_policy") {
            PrivacyPolicy(
                navController = navController
            )
        }

        composable("accessibility_statement") {
            AccessibilityStatement(
                navController = navController
            )
        }

    }
}



