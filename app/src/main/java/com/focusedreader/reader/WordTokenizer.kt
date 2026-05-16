package com.focusedreader.reader

object WordTokenizer {
    private val ws = Regex("\\s+")
    fun tokenize(text: String): List<String> =
        text.split(ws).filter { it.isNotBlank() }
}
