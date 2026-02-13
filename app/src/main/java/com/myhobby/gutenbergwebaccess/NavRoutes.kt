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

/**
 * Defines the navigation routes used within the Gutenberg Access application.
 *
 * This sealed class encapsulates all possible navigation destinations as objects,
 * each associated with a unique route string. This approach provides a type-safe
 * way to manage navigation paths, reducing the likelihood of typos and making
 * refactoring easier.
 *
 * Each object represents a specific screen or destination in the app's navigation graph.
 * The `route` property of each object is the string identifier used by the Navigation
 * component to navigate to that screen.
 *
 */
sealed class NavRoutes(val route: String){
    object Home : NavRoutes("home")
    object About : NavRoutes("about")
    object Search : NavRoutes("search")
    object Browse : NavRoutes("browse")
    object Saved: NavRoutes("saved")
    object Results : NavRoutes("results")
    object ChosenBook : NavRoutes("chosenbook")
    object SavedBooks : NavRoutes("savedbooks")
    object BookChoices : NavRoutes("bookchoices")
    object HtmlBookViewer : NavRoutes("htmlbookviewer")
    object BrowseSection : NavRoutes("browsesection")
    object BookListing : NavRoutes("booklisting")
    object TextToSpeechBookReader : NavRoutes("texttospeechbookreader")
    object ChosenAudioBook : NavRoutes("chosenaudiobook")

    object OggPlayer : NavRoutes("oggplayer")

    object AudioPlayList : NavRoutes("audioplaylist")
    object DirectResults: NavRoutes("directresults")

    object PrivacyPolicy: NavRoutes("privacy_policy")

    object AccessibilityStatement: NavRoutes("accessibility_statement")

    object Settings: NavRoutes("settings")

    object HtmlText: NavRoutes("htmltext")
}

