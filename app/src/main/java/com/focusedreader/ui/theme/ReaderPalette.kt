package com.focusedreader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ReaderPalette(
    val background: Color,
    val word: Color,
    val orp: Color
) {
    companion object {
        val Light = ReaderPalette(Color(0xFFFAFAFA), Color(0xFF121212), Color(0xFFE53935))
        val Dark  = ReaderPalette(Color(0xFF121212), Color(0xFFFAFAFA), Color(0xFFE53935))
    }
}

enum class ThemeMode { LIGHT, DARK, AUTO }

fun effectiveThemeMode(themeMode: ThemeMode, systemDark: Boolean): ThemeMode = when (themeMode) {
    ThemeMode.AUTO -> if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
    else -> themeMode
}

fun palette(theme: ThemeMode): ReaderPalette = when (theme) {
    ThemeMode.LIGHT -> ReaderPalette.Light
    else -> ReaderPalette.Dark
}
