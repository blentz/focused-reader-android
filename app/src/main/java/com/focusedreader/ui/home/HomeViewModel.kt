package com.focusedreader.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusedreader.capture.ClipboardImporter
import com.focusedreader.capture.FilePicker
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.data.ImportSource
import com.focusedreader.data.Session
import com.focusedreader.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SessionRepository,
    private val clipboard: ClipboardImporter,
    private val importer: ImportTextUseCase,
    private val filePicker: FilePicker,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    val session: StateFlow<Session?> = repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun importFromClipboard(onResult: (ImportTextUseCase.Result) -> Unit) {
        viewModelScope.launch { onResult(clipboard.importFromClipboard()) }
    }

    fun importFromFile(uri: Uri, onResult: (ImportTextUseCase.Result) -> Unit) {
        viewModelScope.launch {
            val text = filePicker.readText(appContext, uri)
            onResult(importer(text, ImportSource.FILE))
        }
    }
}
