package com.myhobby.gutenbergwebaccess.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A CompositionLocal to provide a font scaling factor throughout the app.
 * This allows for dynamic adjustment of font sizes.
 */
val LocalFontScale = compositionLocalOf { 1.0f }

/**
 * An extension property on TextUnit to apply the font scaling factor.
 * This makes it easy to apply the dynamic font size to any Text composable.
 *
 * Example:
 * Text("Hello", fontSize = 20.sp.scaled)
 */
val TextUnit.scaled: TextUnit
    @Composable
    get() = this * LocalFontScale.current
