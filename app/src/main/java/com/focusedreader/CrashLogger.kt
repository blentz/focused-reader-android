package com.focusedreader

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Installs a default uncaught-exception handler that writes the stack trace
 * to the app's internal files dir as `last_crash.txt`. Replaces previously
 * stored crash. Chains to the existing handler so the system still kills
 * the process normally.
 *
 * No network. The user can view + share the saved log via
 * Settings → Maintenance.
 */
object CrashLogger {
    private const val FILENAME = "last_crash.txt"

    fun install(appContext: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val file = File(appContext.filesDir, FILENAME)
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val sw = StringWriter().apply {
                    appendLine("Focused Reader crash log")
                    appendLine("Time: $ts")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Throwable: ${throwable::class.java.name}")
                    appendLine()
                    PrintWriter(this).also(throwable::printStackTrace)
                }
                file.writeText(sw.toString())
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(appContext: Context): String? {
        val file = File(appContext.filesDir, FILENAME)
        return if (file.exists()) file.readText() else null
    }

    fun clear(appContext: Context) {
        File(appContext.filesDir, FILENAME).delete()
    }
}
