package com.myhobby.gutenbergwebaccess.util

import android.content.Context
import android.webkit.WebView
import java.io.InputStream

fun loadRawHtml(context: Context, webView: WebView, rawResId: Int) {
    val htmlString = context.resources.openRawResource(rawResId)
        .bufferedReader()
        .use { it.readText() }

    //webView.loadDataWithBaseURL(null, htmlString, "text/html", "UTF-8", null)
    webView.loadUrl("file:///android_asset/filename.html")
}