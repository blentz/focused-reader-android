package com.focusedreader.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.data.HapticMode
import com.focusedreader.data.ReaderFont
import com.focusedreader.data.SessionRepository
import com.focusedreader.data.Settings
import com.focusedreader.data.SettingsRepository
import com.focusedreader.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val sessions: SessionRepository
) : ViewModel() {
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
    fun setOrpColor(argb: Int) = viewModelScope.launch { repo.setOrpColor(argb) }

    fun reset() = viewModelScope.launch { repo.resetToDefaults() }

    /** Toast-friendly user-facing messages emitted by export/import flows. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun exportSessionTo(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            val json = sessions.exportJson()
            if (json == null) {
                _messages.tryEmit("No session to export")
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                        ?: return@runCatching false
                    true
                }.getOrDefault(false)
            }
            _messages.tryEmit(if (ok) "Session exported" else "Export failed")
        }
    }

    fun importSessionFrom(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }.getOrNull()
            }
            if (json.isNullOrBlank()) {
                _messages.tryEmit("Could not read backup")
                return@launch
            }
            val ok = runCatching { sessions.importBackup(json) }.isSuccess
            _messages.tryEmit(if (ok) "Session imported" else "Invalid backup file")
        }
    }
}
