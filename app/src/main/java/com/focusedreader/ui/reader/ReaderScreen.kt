package com.focusedreader.ui.reader

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.launch

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

    // Mid-session back press: pause instead of exit. From Paused/Idle, fall
    // through to default back nav (which exits the reader).
    BackHandler(enabled = state is ReaderState.Reading || state is ReaderState.Resuming) {
        vm.togglePause()
    }

    // End of text → exit to Home instead of sitting on the Idle "No session" screen.
    LaunchedEffect(Unit) {
        vm.finishedEvents.collect { onExit() }
    }

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

    // Account for display cutout (camera hole on Pixel 10) so the centred
    // word never disappears behind the lens. In landscape the cutout is on
    // one edge; padding by displayCutout insets shrinks the usable rect.
    val cutoutInsets = androidx.compose.foundation.layout.WindowInsets.displayCutout
    val leftInsetPx = cutoutInsets.getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr)
    val rightInsetPx = cutoutInsets.getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr)
    val topInsetPx = cutoutInsets.getTop(density)
    val bottomInsetPx = cutoutInsets.getBottom(density)
    val usableWidthPx = with(density) {
        (configuration.screenWidthDp.dp.toPx() - leftInsetPx - rightInsetPx).coerceAtLeast(1f)
    }
    val usableHeightPx = with(density) {
        (configuration.screenHeightDp.dp.toPx() - topInsetPx - bottomInsetPx).coerceAtLeast(1f)
    }

    // Per-word fit-to-width sizing: each word's pivot is anchored at screen
    // centre, so the wider of left-of-pivot or right-of-pivot dictates the
    // scale. A small height cap prevents single-char words from blowing up
    // beyond the screen vertically.
    val maxHeightPx = usableHeightPx * 0.7f
    val leftBudgetPx = usableWidthPx * 0.475f
    val rightBudgetPx = leftBudgetPx

    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(cutoutInsets)
            .semantics {
                contentDescription = when (state) {
                    is ReaderState.Reading -> "Reading. Tap to pause."
                    is ReaderState.Paused -> "Paused."
                    is ReaderState.Resuming -> "Resuming. Tap to pause."
                    ReaderState.Idle -> "Reader idle."
                }
                role = Role.Button
            }
            .clickable { vm.togglePause() }
    ) {
        when (val sst = state) {
            ReaderState.Idle -> Text("No session", color = palette.word, modifier = Modifier.align(Alignment.Center))
            is ReaderState.Reading -> {
                val word = sst.tokens.getOrNull(sst.index) ?: ""
                val finalSize = remember(word, fontFamily, configuration.screenWidthDp, configuration.screenHeightDp) {
                    if (word.isEmpty()) 96.sp
                    else {
                        val refSize = 100.sp
                        val style = TextStyle(fontSize = refSize, fontFamily = fontFamily, fontWeight = FontWeight.Medium)
                        val split = OrpCalculator.split(word)
                        val leftW = if (split.left.isEmpty()) 0 else measurer.measure(split.left, style, maxLines = 1).size.width
                        val pivotW = measurer.measure(split.pivot.toString(), style, maxLines = 1).size.width
                        val rightW = if (split.right.isEmpty()) 0 else measurer.measure(split.right, style, maxLines = 1).size.width
                        val heightAtRef = measurer.measure("Hg", style, maxLines = 1).size.height.coerceAtLeast(1)
                        val leftNeed = (leftW + pivotW / 2f).coerceAtLeast(1f)
                        val rightNeed = (rightW + pivotW / 2f).coerceAtLeast(1f)
                        val scaleByLeft = leftBudgetPx / leftNeed
                        val scaleByRight = rightBudgetPx / rightNeed
                        val scaleByHeight = maxHeightPx / heightAtRef
                        val scale = minOf(scaleByLeft, scaleByRight, scaleByHeight)
                        (refSize.value * scale).sp
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
                tokens = sst.tokens,
                index = sst.index,
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
        // One-shot reverse-direction hint
        var showReverseHint by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            vm.reverseHintPulse.collect {
                showReverseHint = true
                kotlinx.coroutines.delay(3000)
                showReverseHint = false
            }
        }
        if (showReverseHint) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Text(
                    "Reading backwards • Vol Up to resume forward",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
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
    tokens: List<String>,
    index: Int,
    onSeek: (Int) -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSettings: () -> Unit,
    palette: com.focusedreader.ui.theme.ReaderPalette
) {
    var cursor by remember(index) { mutableStateOf(index) }

    Row(
        Modifier.fillMaxSize().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.weight(2f).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DocumentPreview(
                tokens = tokens,
                cursor = cursor,
                onCursorChange = { cursor = it },
                palette = palette,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Paused", color = palette.word, style = MaterialTheme.typography.titleMedium)
            Text("$wpm WPM", color = palette.word, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (cursor != index) onSeek(cursor)
                onResume()
            }) { Text(if (cursor != index) "Resume here" else "Resume") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onStop) { Text("Stop") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSettings) { Text("Settings") }
        }
    }
}

private const val WORDS_PER_LINE = 12
private const val WORDS_PER_PAGE = 250

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentPreview(
    tokens: List<String>,
    cursor: Int,
    onCursorChange: (Int) -> Unit,
    palette: com.focusedreader.ui.theme.ReaderPalette,
    modifier: Modifier = Modifier
) {
    if (tokens.isEmpty()) return

    val lines = remember(tokens) {
        tokens.chunked(WORDS_PER_LINE).map { it.joinToString(" ") }
    }
    val totalPages = ((tokens.size - 1) / WORDS_PER_PAGE) + 1

    val currentLine = (cursor / WORDS_PER_LINE).coerceIn(0, lines.lastIndex)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = (currentLine - 2).coerceAtLeast(0)
    )
    val scope = rememberCoroutineScope()

    // Map scroll position → cursor. Active line = whichever line is closest
    // to the vertical centre of the viewport. Picking by index-of-visible
    // (size/2) would mean the first/last few lines could never reach centre.
    val activeLine by remember {
        androidx.compose.runtime.derivedStateOf {
            val info = listState.layoutInfo
            val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - viewportCentre)
            }?.index?.coerceIn(0, lines.lastIndex) ?: 0
        }
    }
    LaunchedEffect(activeLine) {
        val newCursor = (activeLine * WORDS_PER_LINE).coerceAtMost(tokens.lastIndex)
        if (newCursor != cursor) onCursorChange(newCursor)
    }

    val currentPage = (cursor / WORDS_PER_PAGE) + 1
    var pageInput by remember(currentPage) { mutableStateOf(currentPage.toString()) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview window — about 5 lines tall, center line is the cursor.
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(palette.background.copy(alpha = 0.6f))
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                // Half the viewport height (~90dp) so the first and last lines
                // can be scrolled all the way to the centre reticle.
                contentPadding = PaddingValues(vertical = 90.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
            ) {
                items(lines.size) { idx ->
                    val isActive = idx == activeLine
                    Text(
                        lines[idx],
                        color = if (isActive) palette.orp else palette.word.copy(alpha = 0.55f),
                        style = if (isActive) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
            // Center reticle.
            HorizontalDivider(
                modifier = Modifier.align(Alignment.Center),
                color = palette.orp.copy(alpha = 0.5f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Page",
                color = palette.word,
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = pageInput,
                onValueChange = { raw ->
                    pageInput = raw.filter { it.isDigit() }.take(5)
                },
                singleLine = true,
                modifier = Modifier.width(80.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Text(
                "of $totalPages",
                color = palette.word,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = {
                val target = pageInput.toIntOrNull()?.coerceIn(1, totalPages) ?: return@TextButton
                val targetWord = ((target - 1) * WORDS_PER_PAGE).coerceIn(0, tokens.lastIndex)
                val targetLine = (targetWord / WORDS_PER_LINE).coerceIn(0, lines.lastIndex)
                scope.launch { listState.scrollToItem((targetLine - 2).coerceAtLeast(0)) }
                onCursorChange(targetWord)
            }) { Text("Go") }
        }
    }
}
