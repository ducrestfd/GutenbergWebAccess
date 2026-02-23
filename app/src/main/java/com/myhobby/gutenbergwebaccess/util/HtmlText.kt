package com.myhobby.gutenbergwebaccess.util

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