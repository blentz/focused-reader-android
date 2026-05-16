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
        val LightPure = ReaderPalette(Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFFFF0000))
        val LightSoft = ReaderPalette(Color(0xFFFAFAFA), Color(0xFF121212), Color(0xFFE53935))
        val DarkPure  = ReaderPalette(Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFFF0000))
        val DarkSoft  = ReaderPalette(Color(0xFF121212), Color(0xFFFAFAFA), Color(0xFFE53935))
    }
}

enum class ThemeMode { LIGHT, DARK }
enum class PaletteMode { PURE, SOFT }

fun palette(theme: ThemeMode, mode: PaletteMode): ReaderPalette = when (theme to mode) {
    ThemeMode.LIGHT to PaletteMode.PURE -> ReaderPalette.LightPure
    ThemeMode.LIGHT to PaletteMode.SOFT -> ReaderPalette.LightSoft
    ThemeMode.DARK  to PaletteMode.PURE -> ReaderPalette.DarkPure
    else -> ReaderPalette.DarkSoft
}
