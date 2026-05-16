package com.focusedreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.data.HapticMode
import com.focusedreader.data.ReaderFont
import com.focusedreader.data.Settings
import com.focusedreader.data.SettingsRepository
import com.focusedreader.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() {
    val settings: StateFlow<Settings?> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWpm(v: Int) = viewModelScope.launch { repo.setWpm(v) }
    fun setStep(v: Int) = viewModelScope.launch { repo.setStep(v) }
    fun setResume(v: Int) = viewModelScope.launch { repo.setResumeDelay(v) }
    fun setFaceDown(v: Boolean) = viewModelScope.launch { repo.setFaceDown(v) }
    fun setHaptic(m: HapticMode) = viewModelScope.launch { repo.setHapticMode(m) }
    fun setHapticIntensity(v: Int) = viewModelScope.launch { repo.setHapticIntensity(v) }
    fun setTts(v: Boolean) = viewModelScope.launch { repo.setTtsEnabled(v) }
    fun setTheme(t: ThemeMode) = viewModelScope.launch { repo.setTheme(t) }
    fun setKeepAwake(v: Boolean) = viewModelScope.launch { repo.setKeepAwake(v) }
    fun setFont(v: ReaderFont) = viewModelScope.launch { repo.setFont(v) }
}
