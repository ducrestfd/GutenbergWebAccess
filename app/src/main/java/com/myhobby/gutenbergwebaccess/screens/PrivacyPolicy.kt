package com.myhobby.gutenbergwebaccess.screens

import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes
import com.myhobby.gutenbergwebaccess.util.HtmlText

@Composable
fun PrivacyPolicy(navController: NavController) {


    val myHtml = """
<!DOCTYPE html>
<html>
<body lang="en-US" link="#000080" vlink="#800000" dir="ltr"><p style="line-height: 114%">
<h2 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">Privacy
Policy for Gutenberg Web Access</b></font></h2></p>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif"><b>Last
Updated: February 4, 2026</b></font></p>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">Your
privacy is a priority. <b>Gutenberg Web Access</b> is designed to be
a private, secure tool for accessing public domain literature.</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">1. No Personal Data Collection</font></h3>

<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">We
do not collect, store, or transmit any personally identifiable
information (PII). We do not require account creation or email registration. We
do not collect your name, location, device ID, or contact information.</font></p>

<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">2. No Tracking or Analytics</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">We
do not use third-party tracking pixels, cookies, or analytics SDKs.
Your reading habits, search history, and download history stay
strictly on your device and are never shared with us or any third
parties.</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">3. Data Storage</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">Any
eBooks or audio files you download are stored locally on your device.
These files are managed by your operating system and may not be deleted if
you choose to uninstall the app.</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">4. Third-Party Content (Project
Gutenberg)</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">Our
app facilitates the download of content from Project Gutenberg. While
our app does not collect data, your device will connect to Project
Gutenberg's servers to fetch files. Please refer to Project
Gutenberg’s website (https://www.gutenberg.org/) for their specific terms of use regarding
their servers.</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">5. Children’s Privacy</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">Because
we do not collect any personal information, our app is fully
compliant with the Children’s Online Privacy Protection Act
(COPPA).</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">6. Changes to This Policy</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">We
may update this policy occasionally to reflect changes in our app.
Any updates will be posted on this page.</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">7. Contact Us</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">If
you have any questions about this Privacy Policy, please contact:</font></p>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif"><b>ducrestfd@gmail.com</b></font></p>
</body>
</html>
    """.trimIndent()


    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    navController.popBackStack()
                },
            ) {
                Text(text = "Home")
            }

            Spacer(modifier = Modifier.height(16.dp))

            HtmlText(html = myHtml)

        }
    }
}