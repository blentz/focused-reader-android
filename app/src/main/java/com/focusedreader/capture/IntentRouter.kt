package com.focusedreader.capture

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton bridge from MainActivity intents (app shortcuts, NFC) to Compose UI.
 * MainActivity emits action strings; HomeScreen collects and acts.
 */
@Singleton
class IntentRouter @Inject constructor() {
    private val _actions = MutableSharedFlow<RouterEvent>(extraBufferCapacity = 8)
    val actions: SharedFlow<RouterEvent> = _actions.asSharedFlow()

    fun emit(event: RouterEvent) {
        _actions.tryEmit(event)
    }
}

sealed class RouterEvent {
    data object PasteClipboard : RouterEvent()
    data object Resume : RouterEvent()
    data object OpenFile : RouterEvent()
    /** Text or URL captured from an NFC NDEF tag. */
    data class ImportText(val text: String) : RouterEvent()
}
