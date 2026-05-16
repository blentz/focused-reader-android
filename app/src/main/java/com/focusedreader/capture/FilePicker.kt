package com.focusedreader.capture

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePicker @Inject constructor() {
    suspend fun readText(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }
}
