package com.focusedreader.reader

sealed interface ReaderState {
    data object Idle : ReaderState
    data class Reading(val tokens: List<String>, val index: Int, val wpm: Int) : ReaderState
    data class Paused(val tokens: List<String>, val index: Int, val wpm: Int) : ReaderState
    data class Resuming(val tokens: List<String>, val index: Int, val wpm: Int, val secondsLeft: Int) : ReaderState
}
