package com.myhobby.gutenbergwebaccess.util

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PdfViewerScreen(driveUrl: String) {
    // The gview wrapper works best for embedding
    val gviewUrl = "https://docs.google.com/gview?embedded=true&url=$driveUrl"

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
                loadUrl(gviewUrl)
            }
        },
        update = { webView ->
            webView.loadUrl(gviewUrl)
        },
        modifier = Modifier.fillMaxSize()
    )
}