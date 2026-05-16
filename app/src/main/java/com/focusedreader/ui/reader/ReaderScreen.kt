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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.reader.OrpCalculator
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
    val rs by vm.readerSettings.collectAsState()
    val s = rs ?: return

    DisposableEffect(Unit) {
        val activity = ctx as? ComponentActivity
        val prior = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = prior ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    DisposableEffect(s.keepScreenAwake) {
        val activity = ctx as? ComponentActivity
        val window = activity?.window
        if (window != null) {
            if (s.keepScreenAwake) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
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

    val fontFamily = resolveFontFamily(s.font)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val tokens: List<String> = when (val cur = state) {
        is ReaderState.Reading -> cur.tokens
        is ReaderState.Paused -> cur.tokens
        is ReaderState.Resuming -> cur.tokens
        ReaderState.Idle -> emptyList()
    }

    val fontSize: TextUnit = remember(tokens, s.font, configuration.screenWidthDp, configuration.screenHeightDp) {
        if (tokens.isEmpty()) {
            96.sp
        } else {
            val refSize = 100.sp
            val refStyle = TextStyle(fontSize = refSize, fontFamily = fontFamily, fontWeight = FontWeight.Medium)
            val measured = measurer.measure("Hg", refStyle, maxLines = 1)
            val heightAtRef = measured.size.height.coerceAtLeast(1)
            val targetHeightPx = with(density) { (configuration.screenHeightDp.dp * 0.6f).toPx() }
            val scale = (targetHeightPx / heightAtRef).coerceIn(1.0f, 12.0f)
            (refSize.value * scale).sp
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable { vm.togglePause() }
    ) {
        when (val sst = state) {
            ReaderState.Idle -> Text("No session", color = palette.word, modifier = Modifier.align(Alignment.Center))
            is ReaderState.Reading -> {
                val word = sst.tokens.getOrNull(sst.index) ?: ""
                val bucketScale = when {
                    word.length <= 8 -> 1.0f
                    word.length <= 16 -> 0.8f
                    else -> 0.6f
                }
                val candidate = (fontSize.value * bucketScale).sp
                val finalSize = remember(word, candidate, fontFamily, configuration.screenWidthDp) {
                    if (word.isEmpty()) candidate
                    else {
                        val style = TextStyle(fontSize = candidate, fontFamily = fontFamily, fontWeight = FontWeight.Medium)
                        val split = OrpCalculator.split(word)
                        val leftW = if (split.left.isEmpty()) 0 else measurer.measure(split.left, style, maxLines = 1).size.width
                        val pivotW = measurer.measure(split.pivot.toString(), style, maxLines = 1).size.width
                        val rightW = if (split.right.isEmpty()) 0 else measurer.measure(split.right, style, maxLines = 1).size.width
                        val anchorPx = with(density) { (configuration.screenWidthDp.dp * 0.5f).toPx() }
                        val screenPx = with(density) { (configuration.screenWidthDp.dp * 0.97f).toPx() }
                        val leftNeed = (leftW + pivotW / 2f).coerceAtLeast(1f)
                        val rightNeed = (rightW + pivotW / 2f).coerceAtLeast(1f)
                        val leftScale = anchorPx / leftNeed
                        val rightScale = (screenPx - anchorPx) / rightNeed
                        val widthScale = minOf(leftScale, rightScale, 1f)
                        (candidate.value * widthScale).sp
                    }
                }
                OrpWord(
                    word = word,
                    wordColor = palette.word, orpColor = palette.orp,
                    fontSize = finalSize, fontFamily = fontFamily
                )
            }
            is ReaderState.Paused -> PauseOverlay(
                wpm = sst.wpm,
                index = sst.index,
                total = sst.tokens.size,
                onSeek = { vm.seekTo(it) },
                onResume = { vm.togglePause() },
                onStop = { vm.stop(); onExit() },
                onSettings = onSettings,
                palette = palette
            )
            is ReaderState.Resuming -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Resuming in ${sst.secondsLeft}…", color = palette.word)
            }
        }

        // Volume-key WPM HUD
        var hudWpm by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(Unit) {
            vm.wpmHudPulse.collect { wpm ->
                hudWpm = wpm
                kotlinx.coroutines.delay(1500)
                if (hudWpm == wpm) hudWpm = null
            }
        }
        hudWpm?.let { wpm ->
            Surface(
                color = palette.word.copy(alpha = 0.85f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    "$wpm WPM",
                    color = palette.background,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    wpm: Int,
    index: Int,
    total: Int,
    onSeek: (Int) -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSettings: () -> Unit,
    palette: com.focusedreader.ui.theme.ReaderPalette
) {
    var scrub by remember(index) { mutableStateOf(index.toFloat()) }
    val maxIdx = (total - 1).coerceAtLeast(0).toFloat()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Paused", color = palette.word)
        Text("$wpm WPM", color = palette.word)
        Spacer(Modifier.height(16.dp))
        Text("Position: ${scrub.toInt()} / $total", color = palette.word)
        Slider(
            value = scrub,
            onValueChange = { scrub = it },
            onValueChangeFinished = { onSeek(scrub.toInt()) },
            valueRange = 0f..maxIdx.coerceAtLeast(0f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onResume) { Text("Resume") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onStop) { Text("Stop") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSettings) { Text("Settings") }
    }
}
