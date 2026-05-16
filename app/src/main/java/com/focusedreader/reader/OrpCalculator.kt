package com.focusedreader.reader

object OrpCalculator {
    /**
     * Returns the original-string index of the pivot character.
     * Non-alphanumeric characters do not count toward the "middle":
     *   - odd alphanumeric count -> exact middle alphanumeric char
     *   - even alphanumeric count -> alphanumeric char just after the middle
     */
    fun pivotIndex(word: String): Int {
        if (word.isEmpty()) return 0
        val alphaPositions = word.indices.filter { word[it].isLetterOrDigit() }
        if (alphaPositions.isEmpty()) return word.length / 2
        return alphaPositions[alphaPositions.size / 2]
    }

    data class Split(val left: String, val pivot: Char, val right: String)

    fun split(word: String): Split {
        require(word.isNotEmpty()) { "Cannot split empty word" }
        val idx = pivotIndex(word)
        return Split(word.substring(0, idx), word[idx], word.substring(idx + 1))
    }
}
