package com.focusedreader.reader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RsvpEngine(private val dispatcher: CoroutineDispatcher) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var job: Job? = null
    private var tokens: List<String> = emptyList()
    private var current: Int = 0
    private val wpmFlow = MutableStateFlow(300)
    private var onComplete: () -> Unit = {}

    private val _index = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 64)
    val index: SharedFlow<Int> = _index.asSharedFlow()

    fun start(
        tokens: List<String>,
        startIndex: Int,
        wpm: Int,
        onComplete: () -> Unit = {}
    ) {
        this.tokens = tokens
        this.current = startIndex
        this.wpmFlow.value = Wpm.clamp(wpm)
        this.onComplete = onComplete
        launchTickLoop(emitFirst = true)
    }

    private fun launchTickLoop(emitFirst: Boolean) {
        job?.cancel()
        job = scope.launch {
            var first = emitFirst
            while (current in tokens.indices) {
                if (first) {
                    _index.emit(current)
                    first = false
                }
                val wpm = wpmFlow.value
                if (wpm == 0) {
                    // Pause: wait for wpm to become non-zero.
                    wpmFlow.first { it != 0 }
                    continue
                }
                delay(Wpm.tickMillis(wpm))
                val curWpm = wpmFlow.value
                if (curWpm == 0) continue
                current += Wpm.direction(curWpm)
                if (current in tokens.indices) {
                    _index.emit(current)
                }
            }
            // Natural completion (cancellation throws and skips this line).
            if (tokens.isNotEmpty() && current !in tokens.indices) {
                onComplete()
            }
        }
    }

    fun pause() { job?.cancel(); job = null }
    fun resume(wpm: Int) { start(tokens, current, wpm, onComplete) }
    fun setWpm(wpm: Int) {
        wpmFlow.value = Wpm.clamp(wpm)
        // Reschedule the in-flight delay with the new cadence if running.
        if (job?.isActive == true) {
            launchTickLoop(emitFirst = false)
        }
    }
    fun currentIndex(): Int = current
    fun shutdown() { scope.cancel() }
}
