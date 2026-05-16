package com.focusedreader.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.capture.ClipboardImporter
import com.focusedreader.capture.FilePicker
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.capture.IntentRouter
import com.focusedreader.capture.RouterEvent
import kotlinx.coroutines.flow.SharedFlow
import com.focusedreader.data.ImportSource
import com.focusedreader.data.Session
import com.focusedreader.data.SessionRepository
import com.focusedreader.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SessionRepository,
    private val clipboard: ClipboardImporter,
    private val importer: ImportTextUseCase,
    private val filePicker: FilePicker,
    private val settings: SettingsRepository,
    intentRouter: IntentRouter,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    val routerEvents: SharedFlow<RouterEvent> = intentRouter.actions

    val session: StateFlow<Session?> = repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val showOnboarding: StateFlow<Boolean> = settings.settings
        .map { !it.hasSeenOnboarding }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun importFromClipboard(onResult: (ImportTextUseCase.Result) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                onResult(clipboard.importFromClipboard())
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importFromFile(uri: Uri, onResult: (ImportTextUseCase.Result) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val res = filePicker.read(appContext, uri)
                val mapped = when (res) {
                    is FilePicker.Result.TooLarge -> ImportTextUseCase.Result.FileTooLarge(res.sizeBytes)
                    is FilePicker.Result.ReadFailed -> ImportTextUseCase.Result.FetchFailed
                    is FilePicker.Result.Ok -> importer(res.text, ImportSource.FILE)
                }
                onResult(mapped)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importRawText(text: String, onResult: (ImportTextUseCase.Result) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                onResult(importer(text, ImportSource.SHARE))
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun markOnboardingComplete() {
        viewModelScope.launch { settings.setOnboardingSeen(true) }
    }
}
