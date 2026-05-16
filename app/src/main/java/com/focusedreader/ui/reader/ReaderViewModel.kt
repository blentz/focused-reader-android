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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel(
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val orientation: OrientationMonitor,
    private val haptic: HapticController,
    private val tts: TtsController,
    // Production uses Dispatchers.Default. Tests inject a TestDispatcher so
    // RsvpEngine ticks can be advanced via virtual time.
    engineDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Inject constructor(
        sessions: SessionRepository,
        settings: SettingsRepository,
        orientation: OrientationMonitor,
        haptic: HapticController,
        tts: TtsController
    ) : this(sessions, settings, orientation, haptic, tts, Dispatchers.Default)

    private val engine = RsvpEngine(engineDispatcher)
    private val _state = MutableStateFlow<ReaderState>(ReaderState.Idle)
    val state: StateFlow<ReaderState> = _state

    /** WPM step for volume key adjustments, sourced from settings. */
    val wpmStep: StateFlow<Int> = settings.settings
        .map { it.wpmStep }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 50)

    val readerSettings: StateFlow<Settings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Emits a value each time WPM is bumped by the user. UI shows a brief HUD. */
    private val _wpmHudPulse = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val wpmHudPulse: SharedFlow<Int> = _wpmHudPulse.asSharedFlow()

    /** Emits when the reader has finished a text (engine reached the end). UI exits to Home. */
    private val _finishedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val finishedEvents: SharedFlow<Unit> = _finishedEvents.asSharedFlow()

    /** Fires once the first time WPM goes negative, to teach Vol-Up recovery. */
    private val _reverseHintPulse = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reverseHintPulse: SharedFlow<Unit> = _reverseHintPulse.asSharedFlow()

    fun tokenCount(): Int = tokens.size

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
        // Reactively (re-)initialise or tear down TTS when the setting toggles
        // mid-session. Without this, enabling TTS while the reader is running
        // is silently ignored.
        viewModelScope.launch {
            settings.settings
                .map { it.ttsEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) tts.init() else tts.shutdown()
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
            _finishedEvents.emit(Unit)
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
            val prevWpm = currentWpm()
            val newWpm = Wpm.clamp(prevWpm + delta, max = maxWpm)
            engine.setWpm(newWpm)
            settings.setWpm(newWpm)
            _wpmHudPulse.tryEmit(newWpm)
            if (prevWpm >= 0 && newWpm < 0 && !s.seenReverseHint) {
                _reverseHintPulse.tryEmit(Unit)
                settings.setReverseHintSeen(true)
            }
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

    fun seekTo(index: Int) {
        viewModelScope.launch {
            val target = index.coerceIn(0, (tokens.size - 1).coerceAtLeast(0))
            sessions.updatePosition(target)
            _state.update { cur ->
                when (cur) {
                    is ReaderState.Reading -> cur.copy(index = target)
                    is ReaderState.Paused -> cur.copy(index = target)
                    is ReaderState.Resuming -> cur.copy(index = target)
                    ReaderState.Idle -> cur
                }
            }
            if (_state.value is ReaderState.Reading) startEngine(target, currentWpm())
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
