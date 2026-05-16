package com.focusedreader.capture

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PermissionStatusTest {
    private val pkg = "com.focusedreader"
    private val expected = "com.focusedreader/com.focusedreader.capture.FocusedReaderA11yService"

    @Test fun `null input is false`() {
        assertFalse(PermissionStatus.containsService(null, pkg, expected))
    }

    @Test fun `empty input is false`() {
        assertFalse(PermissionStatus.containsService("", pkg, expected))
    }

    @Test fun `whitespace input is false`() {
        assertFalse(PermissionStatus.containsService("   ", pkg, expected))
    }

    @Test fun `exact component match is true`() {
        assertTrue(PermissionStatus.containsService(expected, pkg, expected))
    }

    @Test fun `case-insensitive exact match is true`() {
        assertTrue(PermissionStatus.containsService(expected.uppercase(), pkg, expected))
    }

    @Test fun `legacy substring fallback matches package + service name`() {
        val legacy = "com.focusedreader.FocusedReaderA11yService"
        assertTrue(PermissionStatus.containsService(legacy, pkg, expected))
    }

    @Test fun `unrelated services are false`() {
        val other = "com.example/.AccessibilityService:com.foo/.BarService"
        assertFalse(PermissionStatus.containsService(other, pkg, expected))
    }

    @Test fun `colon-separated list with our service present is true`() {
        val list = "com.example/.A:$expected:com.foo/.B"
        assertTrue(PermissionStatus.containsService(list, pkg, expected))
    }
}
