package com.myhobby.gutenbergwebaccess.util

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

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    val textColor = MaterialTheme.colorScheme.onBackground

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context)
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(
                html,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            textView.textSize = 16f * fontScale
            textView.setTextColor(textColor.toArgb())
        }
    )
}