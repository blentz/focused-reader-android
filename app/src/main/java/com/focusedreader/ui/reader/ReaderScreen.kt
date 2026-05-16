package com.focusedreader.ui.reader

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.reader.ReaderState
import com.focusedreader.ui.theme.LocalReaderPalette

@Composable
fun ReaderScreen(
    onExit: () -> Unit,
    onSettings: () -> Unit,
    vm: ReaderViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val palette = LocalReaderPalette.current
    val ctx = LocalContext.current

    DisposableEffect(Unit) {
        val activity = ctx as? ComponentActivity
        val prior = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = prior ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    val step by vm.wpmStep.collectAsState()
    LaunchedEffect(step) {
        val activity = ctx as? com.focusedreader.MainActivity ?: return@LaunchedEffect
        activity.keyEvents.collect { code ->
            when (code) {
                android.view.KeyEvent.KEYCODE_VOLUME_UP -> vm.bumpWpm(+step)
                android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> vm.bumpWpm(-step)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable { vm.togglePause() }
    ) {
        when (val s = state) {
            ReaderState.Idle -> Text("No session", color = palette.word, modifier = Modifier.align(Alignment.Center))
            is ReaderState.Reading -> OrpWord(
                word = s.tokens.getOrNull(s.index) ?: "",
                wordColor = palette.word, orpColor = palette.orp, fontSize = 96.sp
            )
            is ReaderState.Paused -> PauseOverlay(
                wpm = s.wpm,
                onResume = { vm.togglePause() },
                onStop = { vm.stop(); onExit() },
                onSettings = onSettings,
                palette = palette
            )
            is ReaderState.Resuming -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Resuming in ${s.secondsLeft}…", color = palette.word)
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    wpm: Int,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSettings: () -> Unit,
    palette: com.focusedreader.ui.theme.ReaderPalette
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Paused", color = palette.word)
        Text("$wpm WPM", color = palette.word)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onResume) { Text("Resume") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onStop) { Text("Stop") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSettings) { Text("Settings") }
    }
}
