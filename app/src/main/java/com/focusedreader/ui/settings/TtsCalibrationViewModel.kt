package com.focusedreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.data.SettingsRepository
import com.focusedreader.reader.TtsController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TtsCalibrationViewModel @Inject constructor(
    private val tts: TtsController,
    private val settings: SettingsRepository
) : ViewModel() {

    data class State(val low: Int = 100, val high: Int = 900, val current: Int = 500, val done: Boolean = false)

    private val testSentence = "The quick brown fox jumps over the lazy dog while reading at calibration speed"
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun init() = viewModelScope.launch { tts.init() }

    fun speakCurrent() = viewModelScope.launch {
        val words = testSentence.split(" ")
        val delayMs = 60_000L / _state.value.current
        for (w in words) { tts.speak(w); kotlinx.coroutines.delay(delayMs) }
    }

    fun answer(understandable: Boolean) {
        val s = _state.value
        val newState = if (understandable) {
            s.copy(low = s.current, current = (s.current + s.high) / 2)
        } else {
            s.copy(high = s.current, current = (s.current + s.low) / 2)
        }
        if (newState.high - newState.low <= 25) {
            _state.value = newState.copy(done = true, current = newState.low)
            viewModelScope.launch { settings.setTtsCap(newState.low) }
        } else {
            _state.value = newState
        }
    }
}
