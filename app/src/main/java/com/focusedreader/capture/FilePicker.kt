package com.focusedreader.capture

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePicker @Inject constructor() {
    suspend fun readText(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val mime = ctx.contentResolver.getType(uri)
        runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { stream ->
                if (mime == "application/pdf") {
                    PDFBoxResourceLoader.init(ctx.applicationContext)
                    PDDocument.load(stream).use { doc ->
                        PDFTextStripper().getText(doc)
                    }
                } else {
                    readTextFromStream(stream)
                }
            }
        }.getOrNull()
    }

    internal fun readTextFromStream(stream: InputStream): String =
        stream.bufferedReader().use { it.readText() }
}
