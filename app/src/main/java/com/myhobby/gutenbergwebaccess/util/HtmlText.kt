package com.myhobby.gutenbergwebaccess.util

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat


@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val fontScale = LocalFontScale.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            // Initialize the traditional TextView
            TextView(context).apply {
                textSize = 16f * fontScale
            }
        },
        update = { textView ->
            // This block runs whenever the 'html' state changes
            textView.text = HtmlCompat.fromHtml(
                html,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            textView.textSize = 16f * fontScale
        }
    )
}