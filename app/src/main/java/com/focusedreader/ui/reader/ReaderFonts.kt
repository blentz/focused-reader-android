package com.focusedreader.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.focusedreader.R
import com.focusedreader.data.ReaderFont

@Composable
fun resolveFontFamily(font: ReaderFont): FontFamily = when (font) {
    ReaderFont.OPEN_DYSLEXIC -> FontFamily(Font(R.font.opendyslexic_regular))
    ReaderFont.LEXEND -> FontFamily(Font(R.font.lexend_regular))
    ReaderFont.ATKINSON_HYPERLEGIBLE -> FontFamily(Font(R.font.atkinson_hyperlegible_regular))
    ReaderFont.INCLUSIVE_SANS -> FontFamily(Font(R.font.inclusive_sans_regular))
}
