package com.focusedreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalReaderPalette = staticCompositionLocalOf { ReaderPalette.DarkSoft }

@Composable
fun FocusedReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    readerPalette: ReaderPalette = if (darkTheme) ReaderPalette.DarkSoft else ReaderPalette.LightSoft,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalReaderPalette provides readerPalette) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
