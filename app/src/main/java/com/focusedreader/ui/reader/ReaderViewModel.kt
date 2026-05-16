package com.focusedreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.data.SessionRepository
import com.focusedreader.data.Settings
import com.focusedreader.data.SettingsRepository
import com.focusedreader.reader.FaceOrientation
import com.focusedreader.reader.HapticController
import com.focusedreader.reader.OrientationMonitor
import com.focusedreader.reader.TtsController
import com.focusedreader.reader.ReaderState
import com.focusedreader.reader.RsvpEngine
import com.focusedreader.reader.WordTokenizer
import com.focusedreader.reader.Wpm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val orientation: OrientationMonitor,
    private val haptic: HapticController,
    private val tts: TtsController
) : ViewModel() {

    private val engine = RsvpEngine(Dispatchers.Default)
    private val _state = MutableStateFlow<ReaderState>(ReaderState.Idle)
    val state: StateFlow<ReaderState> = _state

    /** WPM step for volume key adjustments, sourced from settings. */
    val wpmStep: StateFlow<Int> = settings.settings
        .map { it.wpmStep }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 50)

    val readerSettings: StateFlow<Settings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var tokens: List<String> = emptyList()
    private var saveCounter = 0

    init {
        viewModelScope.launch {
            val session = sessions.current() ?: return@launch
            val s = settings.settings.first()
            tokens = WordTokenizer.tokenize(session.text)
            val startIdx = session.position.coerceIn(0, tokens.size)
            val maxWpm = if (s.ttsEnabled) s.ttsWpmCap else Wpm.DEFAULT_MAX
            val wpm = Wpm.clamp(s.wpm, max = maxWpm)
            if (s.ttsEnabled) tts.init()
            _state.value = ReaderState.Reading(tokens, startIdx, wpm)
            startEngine(startIdx, wpm)
        }
        viewModelScope.launch {
            engine.index.collect { idx ->
                _state.update { cur ->
                    when (cur) {
                        is ReaderState.Reading -> cur.copy(index = idx)
                        else -> cur
                    }
                }
                val s = settings.settings.first()
                val word = tokens.getOrNull(idx) ?: ""
                haptic.tick(word, s.hapticMode, s.hapticIntensityPct)
                if (s.ttsEnabled) tts.speak(word)
                saveCounter++
                if (saveCounter % 5 == 0) sessions.updatePosition(idx)
            }
        }
        viewModelScope.launch {
            val s = settings.settings.first()
            if (!s.faceDownPauseEnabled) return@launch
            orientation.orientationEvents().collect { o ->
                when (val cur = _state.value) {
                    is ReaderState.Reading -> if (o == FaceOrientation.DOWN) togglePause()
                    is ReaderState.Paused -> if (o == FaceOrientation.UP) togglePause()
                    else -> Unit
                }
            }
        }
    }

    private fun startEngine(idx: Int, wpm: Int) {
        engine.start(tokens, idx, wpm, onComplete = ::onReadingComplete)
    }

    private fun onReadingComplete() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            sessions.updatePosition(0)
            _state.value = ReaderState.Idle
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            when (val cur = _state.value) {
                is ReaderState.Reading -> {
                    engine.pause()
                    sessions.updatePosition(cur.index)
                    _state.value = ReaderState.Paused(cur.tokens, cur.index, cur.wpm)
                }
                is ReaderState.Paused -> {
                    val delaySec = settings.settings.first().resumeDelaySec
                    _state.value = ReaderState.Resuming(cur.tokens, cur.index, cur.wpm, delaySec)
                    countdownThenResume(delaySec, cur)
                }
                is ReaderState.Resuming -> {
                    _state.value = ReaderState.Paused(cur.tokens, cur.index, cur.wpm)
                }
                ReaderState.Idle -> Unit
            }
        }
    }

    private fun countdownThenResume(seconds: Int, base: ReaderState.Paused) {
        viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                _state.value = ReaderState.Resuming(base.tokens, base.index, base.wpm, left)
                kotlinx.coroutines.delay(1000)
                if (_state.value !is ReaderState.Resuming) return@launch
                left--
            }
            _state.value = ReaderState.Reading(base.tokens, base.index, base.wpm)
            startEngine(base.index, base.wpm)
        }
    }

    fun bumpWpm(delta: Int) {
        viewModelScope.launch {
            val s = settings.settings.first()
            val maxWpm = if (s.ttsEnabled) s.ttsWpmCap else Wpm.DEFAULT_MAX
            val newWpm = Wpm.clamp(currentWpm() + delta, max = maxWpm)
            engine.setWpm(newWpm)
            settings.setWpm(newWpm)
            _state.update { cur ->
                when (cur) {
                    is ReaderState.Reading -> cur.copy(wpm = newWpm)
                    is ReaderState.Paused -> cur.copy(wpm = newWpm)
                    is ReaderState.Resuming -> cur.copy(wpm = newWpm)
                    ReaderState.Idle -> cur
                }
            }
        }
    }

    private fun currentWpm(): Int = when (val s = _state.value) {
        is ReaderState.Reading -> s.wpm
        is ReaderState.Paused -> s.wpm
        is ReaderState.Resuming -> s.wpm
        ReaderState.Idle -> 300
    }

    fun stop() {
        engine.pause()
        _state.value = ReaderState.Idle
    }

    override fun onCleared() {
        engine.shutdown()
        tts.shutdown()
        super.onCleared()
    }
}
