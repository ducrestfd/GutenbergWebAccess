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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.viewmodels.OggPlaybackViewModel
import java.text.DecimalFormat

/**
 * A screen that allows the user to configure application-wide settings.
 *
 * Currently, this screen provides a control to set the default speaking speed for the
 * audio player. The selected value is observed from and saved to the [OggPlaybackViewModel],
 * which persists the setting using a DataStore repository.
 *
 * @param navController The NavController used for navigation, such as going back.
 * @param oggPlaybackViewModel The ViewModel that provides access to settings data and saving logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navController: NavController,
    oggPlaybackViewModel: OggPlaybackViewModel
) {
    // Collect the default speaking speed from the ViewModel's Flow.
    // `collectAsState` makes sure the UI recomposes whenever the value changes in the DataStore.
    // The `initial` value of 1.0f is used only until the first value is emitted from the flow.
    val defaultSpeed by oggPlaybackViewModel.defaultSpeakingSpeedFlow.collectAsState(initial = 1.0f)

    // A formatter to display the speed value nicely (e.g., "1.2x").
    val decimalFormat = DecimalFormat("0")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp), // Add our own padding for the content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Default Speaking Speed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display the current speed value (e.g., "1.5x")
            Text(
                text = "${decimalFormat.format(defaultSpeed * 100)}%",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // A Slider to let the user pick a speed.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Minus Button
                Button(
                    onClick = {
                        // Calculate the new speed, ensuring it doesn't go below the minimum.
                        val newSpeed = (defaultSpeed - 0.1f).coerceAtLeast(0.5f)
                        oggPlaybackViewModel.saveDefaultSpeakingSpeed(newSpeed)
                    },
                    // Disable the button if the speed is already at its minimum.
                    enabled = defaultSpeed > 0.5f
                ) {
                    Text("-")
                }

                // The Slider now takes up the available space in the middle.
                Slider(
                    value = defaultSpeed,
                    onValueChange = { newSpeed ->
                        oggPlaybackViewModel.saveDefaultSpeakingSpeed(newSpeed)
                    },
                    valueRange = 0.5f..2.0f,
                    // --- THIS IS THE KEY ---
                    // Number of steps = ((2.0 - 0.5) / 0.1) - 1 = 14
                    steps = 14,
                    modifier = Modifier.weight(1f) // Let the slider fill the space
                )

                // Plus Button
                Button(
                    onClick = {
                        // Calculate the new speed, ensuring it doesn't exceed the maximum.
                        val newSpeed = (defaultSpeed + 0.1f).coerceAtMost(2.0f)
                        oggPlaybackViewModel.saveDefaultSpeakingSpeed(newSpeed)
                    },
                    // Disable the button if the speed is already at its maximum.
                    enabled = defaultSpeed < 2.0f
                ) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Labels to show the min and max range of the slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "50% (Slower)")
                Text(text = "200% (Faster)")
            }
        }
    }
}
