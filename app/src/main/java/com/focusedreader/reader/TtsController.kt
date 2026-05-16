package com.focusedreader.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TtsController @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready: Boolean = false

    suspend fun init(): Boolean {
        if (ready && tts != null) return true
        // Dispose any prior instance before constructing a new one to avoid leaks
        // when callers re-init after a settings toggle.
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
        ready = false
        return suspendCancellableCoroutine { cont ->
            val engine = TextToSpeech(ctx) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (cont.isActive) cont.resume(ready)
            }
            tts = engine
            cont.invokeOnCancellation {
                try {
                    engine.stop()
                    engine.shutdown()
                } catch (_: Throwable) {}
            }
        }
    }

    fun speak(word: String) {
        if (!ready) return
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, word)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
        tts = null
        ready = false
    }
}
