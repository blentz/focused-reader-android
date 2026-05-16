package com.focusedreader.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusedreader.data.ImportSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accepts text imports from external automation apps (Tasker, Macrodroid, etc.).
 *
 *   adb shell am broadcast -a com.focusedreader.IMPORT_TEXT \
 *     --es text "Hello from Tasker" -n com.focusedreader/.capture.TaskerReceiver
 *
 * Uses goAsync() + an internal scope (NOT GlobalScope) so the receiver lifecycle
 * is bounded by pending.finish().
 */
@AndroidEntryPoint
class TaskerReceiver : BroadcastReceiver() {
    @Inject lateinit var importer: ImportTextUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("text") ?: return
        val pending = goAsync()
        scope.launch {
            try {
                importer(text, ImportSource.SHARE)
            } finally {
                pending.finish()
            }
        }
    }
}
