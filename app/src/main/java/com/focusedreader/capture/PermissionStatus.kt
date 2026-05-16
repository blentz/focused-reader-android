package com.focusedreader.capture

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object PermissionStatus {
    fun isAccessibilityServiceEnabled(ctx: Context): Boolean {
        val expected = ComponentName(ctx, FocusedReaderA11yService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val component = splitter.next()
            if (component.equals(expected, ignoreCase = true)) return true
            if (component.contains(ctx.packageName) && component.contains("FocusedReaderA11yService")) return true
        }
        return false
    }
}
