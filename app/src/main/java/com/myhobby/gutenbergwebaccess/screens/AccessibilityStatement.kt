package com.myhobby.gutenbergwebaccess.screens

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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.navigation.NavController
import com.myhobby.gutenbergwebaccess.NavRoutes
import com.myhobby.gutenbergwebaccess.util.HtmlText
import com.myhobby.gutenbergwebaccess.util.scaled

@Composable
fun AccessibilityStatement(navController: NavController) {

    val myHtml = """
<!DOCTYPE html>
<html>
<body lang="en-US" link="#000080" vlink="#800000" dir="ltr"><h2 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">Accessibility Statement:
Gutenberg Web Access</font></h2>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">Our Commitment</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif"><b>Gutenberg
Web Access</b> was built with the belief that the world’s greatest
literature should be accessible to everyone, regardless of visual
ability. We prioritize a &quot;function-first&quot; design that
removes the visual noise of the modern web to provide a clean,
reliable experience for the blind and low-vision community or anyone
who uses a screen reader.</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">Key Accessibility Features</font></h3>
<ul>
	<li><p style="line-height: 114%; border: none; padding: 0in"><font face="Google Sans Text, sans-serif"><b>VoiceOver
	Optimization:</b> <i><b><span style="background: #ffff00">Goal –
	needs testing:</span></b></i><i><b> </b></i>Every element, button,
	and image in this app is labeled with descriptive accessibility
	traits. Navigation follows a logical, predictable flow intended for
	screen reader users.</font></p>
	<li><p style="line-height: 114%; border: none; padding: 0in"><font face="Google Sans Text, sans-serif"><b>Text-to-Speech
	(TTS) Integration:</b> For text-based eBooks, we utilize a
	specialized speech engine that allows for fine-grained control,
	including adjustable playback speed and skipping ahead by number of
	sentences.</font></p>
	<li><p style="line-height: 114%; border: none; padding: 0in"><font face="Google Sans Text, sans-serif"><b>High
	Contrast &amp; Large Text:</b><i><b><span style="background: #ffff00">
	</span></b></i ><i><b><span style="background: #ffff00">Goal –
	needs testing: </span></b></i>The interface utilizes high-contrast
	color ratios and supports <b>Dynamic Type</b>, allowing the app’s
	layout to scale gracefully with your system-wide font size settings.</font></p>
	<li><p style="line-height: 114%; border: none; padding: 0in"><font face="Google Sans Text, sans-serif"><b>Simplified
	Audio Controls:</b> <i><b><span style="background: #ffff00">Goal –
	needs testing: </span></b></i>Our audiobook player uses large,
	easy-to-target touch zones and standard media commands for a
	seamless listening experience.</font></p>
</ul>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">Technical Standards</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif"><i><b><span style="background: #ffff00">Goal
– needs testing:  </span></b></i>We aim to follow the <b>Web
Content Accessibility Guidelines (WCAG) 2.2</b> at the AA level as
our baseline for mobile interface design. We regularly test the app
using native Android accessibility assistive technologies to ensure a &quot;no-barrier&quot;
experience.</font></p>
<h3 class="western" style="line-height: 114%; margin-top: 0in; margin-bottom: 0.1in">
<font face="Google Sans, sans-serif">Feedback &amp; Support</font></h3>
<p style="line-height: 114%"><font face="Google Sans Text, sans-serif">Accessibility
is an ongoing journey. If you encounter a button that isn't labeled
correctly, a menu that is difficult to navigate, or if you have
suggestions for improvement, please contact us:</font></p>
<ul>
	<li><p style="line-height: 114%; border: none; padding: 0in"><font face="Google Sans Text, sans-serif"><b>Email:</b>
	ducrestfd@gmail.com</font></p>
</ul>
<hr/>

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
                Text(text = "Home", fontSize = 16.sp.scaled)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HtmlText(html = myHtml)

        }
    }
}