package com.myhobby.gutenbergwebaccess.screens

/*
Gutenberg Web Access's raison d'être is to provide simple access to
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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.myhobby.gutenbergwebaccess.util.scaled


/**
 * A composable that displays a block of text containing a single clickable hyperlink.
 * * This function takes a full string, identifies a specific part of it to act as a link,
 * and renders it with an underline and primary color to make it interactive. It uses
 * an [AnnotatedString] to combine regular text with a [LinkAnnotation.Url]. The entire
 * composable is wrapped in a [SelectionContainer] to allow users to both click the link
 * and select/copy the surrounding text.
 *
 * @param text The full string of text to be displayed, which includes the portion that will become a link.
 * @param url The destination URL that the link should open (e.g., "https://www.gnu.org" or "mailto:user@example.com").
 * @param linkText The exact substring within the `text` parameter that should be turned into the clickable link.
 */
@Composable
fun LinkifiedTextModern(text: String, url: String, linkText: String) {
    val annotatedString = buildAnnotatedString {
        append(text.substringBefore(linkText)) // Text before the link

        // Use withLink for URL interactions
        withLink(
            link = LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 16.sp.scaled,
                        fontWeight = FontWeight.Bold,
                    )
                )
            )
        ) {
            append(linkText) // The text to display for the link
        }
        append(text.substringAfter(linkText)) // Text after the link
    }

    // SelectionContainer allows users to select text, and also handles link clicks
    SelectionContainer {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 16.sp.scaled,
            fontWeight = FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp.scaled
        )
    }
}


/**
 * Composable function that displays the "About" screen of the Gutenberg Web Access application.
 *
 * This screen provides information about the app's purpose, licensing,
 * where saved books are located, and how to provide feedback.
 * It includes a button to navigate back to the previous screen (presumably the home screen).
 *
 * @param navController The NavController used for navigating between screens.
 */
@Composable
fun About(navController: NavController) {

    val fullTextEmail = "Comments and suggestions are welcome and will be taken into consideration. " +
            "They may be submitted to ducrestfd@gmail.com"

    val fullTextLicense = "Gutenberg Web Access's raison d'être is to provide simple access to " +
            "the Project Gutenberg website of 70,000 plus books to both " +
            "sighted and blind users.  It is provided without charge under the " +
            "agpl-3.0 license.\n\n" +
            "Copyright (C) 2026 Frank D. Ducrest\n\n" +
            "This program is free software: you can redistribute it and/or modify " +
            "it under the terms of the GNU Affero General Public License as published " +
            "by the Free Software Foundation, either version 3 of the License, or " +
            "any later version.\n\n" +
            " This program is distributed in the hope that it will be useful, " +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the " +
            " GNU Affero General Public License for more details.\n\n" +
            "You should have received a copy of the GNU Affero General Public License " +
            "along with this program.  If not, see https://www.gnu.org/licenses/agpl-3.0.en.html"

    val trademarkDisclaimer =
    "Project Gutenberg is a registered trademark of the Project Gutenberg Literary Archive Foundation. " +
            "This app is not an official Project Gutenberg application, nor is it affiliated with or endorsed by " +
            "the Foundation."

    val emailAddress = "ducrestfd@gmail.com"
    val mailtoUrl = "mailto:$emailAddress"
    val gnuAddress = "https://www.gnu.org/licenses/agpl-3.0.en.html"
    val uriHandler = LocalUriHandler.current
    val fullGutenbergAbout = "To find out more about the Project Gutenberg,\nsee https://www.gutenberg.org/about/"
    val gutenbergAddress = "https://www.gutenberg.org/about/"
    val gitHubAddress = "https://github.com/ducrestfd/GutenbergWebAccess"
    val gitHubText = "Code for this project is available at https://github.com/ducrestfd/GutenbergWebAccess"



    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "About Gutenberg Web Access",
                style = TextStyle(fontSize = 24.sp.scaled, fontWeight = FontWeight.Bold)
            )

            Text(
                "Release date 2026-02-13 Version 3.0",
                style = TextStyle(fontSize = 12.sp.scaled, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinkifiedTextModern(
                text = fullTextLicense,
                url = gnuAddress,
                linkText = gnuAddress
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = trademarkDisclaimer,
                style = TextStyle(fontSize = 16.sp.scaled))

            Spacer(modifier = Modifier.height(16.dp))

            LinkifiedTextModern(
                text = fullGutenbergAbout,
                url = gutenbergAddress,
                linkText = gutenbergAddress
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "All books saved by this app are HTML/Ogg files\nlocated on your device at\n\n" +
                        "Downloads/gutenbergwebaccess\n\n" +
                        "They may be copied, shared or \nopened in your preferred app\n(such as " +
                        "Speechify, Firefox, etc.)",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 16.sp.scaled,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp.scaled
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinkifiedTextModern(
                text = gitHubText,
                url = gitHubAddress,
                linkText = gitHubAddress
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinkifiedTextModern(
                text = fullTextEmail,
                url = mailtoUrl,
                linkText = emailAddress
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.popBackStack()
                },
            ) {
                Text("Home", fontSize = 16.sp.scaled)
            }
        }
    }
}