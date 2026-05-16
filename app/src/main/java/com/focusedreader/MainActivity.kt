package com.focusedreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.focusedreader.nav.FocusedReaderNavGraph
import com.focusedreader.ui.theme.FocusedReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusedReaderTheme {
                FocusedReaderNavGraph()
            }
        }
    }
}
