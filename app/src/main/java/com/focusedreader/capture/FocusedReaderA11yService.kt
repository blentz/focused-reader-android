package com.focusedreader.capture

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focusedreader.data.ImportSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusedReaderA11yService : AccessibilityService() {

    @Inject lateinit var importer: ImportTextUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var capturePending = false

    companion object {
        const val ACTION_CAPTURE = "com.focusedreader.action.CAPTURE_NOW"
        @Volatile var instance: FocusedReaderA11yService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun requestCapture() {
        val text = collectText(rootInActiveWindow)
        scope.launch { importer(text, ImportSource.A11Y) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun collectText(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val out = StringBuilder()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            val t = n.text?.toString()?.trim().orEmpty()
            val d = n.contentDescription?.toString()?.trim().orEmpty()
            if (t.isNotBlank()) { out.append(t); out.append(' ') }
            else if (d.isNotBlank()) { out.append(d); out.append(' ') }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        return out.toString().trim()
    }
}
