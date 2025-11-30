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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes



/**
 * Composable function that displays the "Browse" screen of the Gutenberg Web Access application.
 *
 * This screen allows users to select a category of books to browse. The categories are
 * predefined and include topics like "Arts & Culture", "Education & Reference", "History", etc.
 * Clicking on a category button will navigate the user to another screen (presumably
 * `BrowseSection`) displaying books within that category, further refined by the
 * `typeOfBrowse` parameter (e.g., "Popularity", "Release Date").
 *
 * The screen also includes a "Home" button to navigate back to the previous screen.
 *
 * @param navController The [NavController] used for navigating between screens.
 * @param typeOfBrowse An optional [String] that specifies the initial browsing criteria
 *                     (e.g., "Popularity", "Release Date", or an alphabetical character)
 *                     which will be passed along when a category is selected. This helps
 *                     determine how books are sorted or filtered on the subsequent screen.
 */
@Composable
fun Browse(navController: NavController, typeOfBrowse: String?) {

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Button(onClick = {
                navController.popBackStack()
            },
                // modifier = Modifier.semantics {contentDescription = "Return to the home screen"}
            ) {
                Text(text = "Home")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Gutenberg Web Access!",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Browse by $typeOfBrowse", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Arts & Culture") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Arts & Culture Books"}
            ) {
                Text(text = "Arts & Culture")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Education & Reference") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Education & Reference Books"}
            ) {
                Text(text = "Education & Reference")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Health & Medicine") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Health & Medicine Books"}
            ) {
                Text(text = "Health & Medicine")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/History") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "History Books"}
            ) {
                Text(text = "History")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Lifestyle & Hobbies") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Lifestyle & Hobbies Books"}
            ) {
                Text(text = "Lifestyle & Hobbies")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Literature") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Literature Books"}
            ) {
                Text(text = "Literature")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Religion & Philosophy") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Religion & Philosophy Books"}
            ) {
                Text(text = "Religion & Philosophy")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Science & Technology") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Science & Technology Books"}
            ) {
                Text(text = "Science & Technology")
            }
            Button(onClick = {
                navController.navigate(NavRoutes.BrowseSection.route + "/${typeOfBrowse}/Social Sciences & Society") {
                    launchSingleTop = true
                }
            },
                // modifier = Modifier.semantics {contentDescription = "Social Sciences & Society Books" }
            ) {
                Text(text = "Social Sciences & Society")
            }

        }
    }
}