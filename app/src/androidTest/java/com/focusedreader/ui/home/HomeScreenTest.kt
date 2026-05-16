package com.focusedreader.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun renders_buttons() {
        rule.setContent {
            HomeScreenContent(
                session = null,
                onRead = {},
                onSettings = {},
                onPasteFromClipboard = {},
                onOpenFile = {}
            )
        }
        rule.onNodeWithText("Read").assertIsDisplayed()
        rule.onNodeWithText("Settings").assertIsDisplayed()
        rule.onNodeWithText("Paste from clipboard").assertIsDisplayed()
    }
}
