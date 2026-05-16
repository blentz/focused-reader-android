package com.focusedreader.capture

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object PermissionStatus {
    fun isAccessibilityServiceEnabled(ctx: Context): Boolean {
        val expected = ComponentName(ctx, FocusedReaderA11yService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return containsService(enabled, ctx.packageName, expected)
    }

    internal fun containsService(
        enabledServices: String?,
        packageName: String,
        expectedComponent: String
    ): Boolean {
        if (enabledServices.isNullOrBlank()) return false
        return enabledServices.split(':').any { component ->
            component.equals(expectedComponent, ignoreCase = true) ||
                (component.contains(packageName) && component.contains("FocusedReaderA11yService"))
        }
    }
}
