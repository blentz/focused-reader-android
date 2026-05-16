package com.focusedreader.capture

import android.content.ClipboardManager
import android.content.Context
import com.focusedreader.data.ImportSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardImporter @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val importer: ImportTextUseCase
) {
    suspend fun importFromClipboard(): ImportTextUseCase.Result {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        return importer(text, ImportSource.CLIPBOARD)
    }
}
