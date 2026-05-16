package com.focusedreader.reader

object OrpCalculator {
    fun pivotIndex(length: Int): Int = when {
        length <= 1 -> 0
        length <= 5 -> 1
        length <= 9 -> 2
        length <= 13 -> 3
        else -> 4
    }

    data class Split(val left: String, val pivot: Char, val right: String)

    fun split(word: String): Split {
        require(word.isNotEmpty()) { "Cannot split empty word" }
        val idx = pivotIndex(word.length)
        return Split(word.substring(0, idx), word[idx], word.substring(idx + 1))
    }
}
