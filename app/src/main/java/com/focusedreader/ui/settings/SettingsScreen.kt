package com.focusedreader.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.focusedreader.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.data.HapticMode
import com.focusedreader.data.ReaderFont
import com.focusedreader.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onCalibrateTts: () -> Unit,
    onBack: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val current = s ?: return

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportSessionTo(ctx, uri) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importSessionFrom(ctx, uri) }

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(top = 56.dp, end = 72.dp)
    ) {
        Section("Speed") {
            SliderRow("WPM step (${current.wpmStep})", current.wpmStep.toFloat(), 5f..50f, 8) { vm.setStep(it.toInt()) }
            SliderRow("WPM (${current.wpm})", current.wpm.toFloat(), 100f..400f, 6) { vm.setWpm(it.toInt()) }
        }
        Section("Pause / Resume") {
            SliderRow("Resume delay (${current.resumeDelaySec}s)", current.resumeDelaySec.toFloat(), 0f..10f, 10) { vm.setResume(it.toInt()) }
            SwitchRow("Face-down pause", current.faceDownPauseEnabled) { vm.setFaceDown(it) }
        }
        Section("Haptic") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode: ")
                HapticMode.values().forEach { m ->
                    FilterChip(selected = current.hapticMode == m, onClick = { vm.setHaptic(m) }, label = { Text(m.name) }, modifier = Modifier.padding(end = 4.dp))
                }
            }
            SliderRow("Intensity (${current.hapticIntensityPct}%)", current.hapticIntensityPct.toFloat(), 0f..33f, 33) { vm.setHapticIntensity(it.toInt()) }
        }
        Section("TTS") {
            SwitchRow("Enable TTS", current.ttsEnabled) { vm.setTts(it) }
            Text("WPM cap: ${current.ttsWpmCap}")
            Button(onClick = onCalibrateTts) { Text("Calibrate") }
        }
        Section("Theme") {
            Row {
                ThemeMode.values().forEach { t ->
                    FilterChip(selected = current.themeMode == t, onClick = { vm.setTheme(t) }, label = { Text(t.name) }, modifier = Modifier.padding(end = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Highlight color")
            Spacer(Modifier.height(4.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OrpColorSwatches.values.forEach { argb ->
                    ColorSwatch(
                        argb = argb,
                        selected = current.orpColorArgb == argb,
                        onClick = { vm.setOrpColor(argb) }
                    )
                }
            }
        }
        Section("Display") {
            SwitchRow("Keep screen awake", current.keepScreenAwake) { vm.setKeepAwake(it) }
            Text("Font")
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReaderFont.values().forEach { f ->
                    val label = when (f) {
                        ReaderFont.OPEN_DYSLEXIC -> "OpenDyslexic"
                        ReaderFont.LEXEND -> "Lexend"
                        ReaderFont.ATKINSON_HYPERLEGIBLE -> "Atkinson Hyperlegible"
                        ReaderFont.INCLUSIVE_SANS -> "Inclusive Sans"
                    }
                    FilterChip(
                        selected = current.font == f,
                        onClick = { vm.setFont(f) },
                        label = { Text(label, fontFamily = com.focusedreader.ui.reader.resolveFontFamily(f)) }
                    )
                }
            }
        }
        Section("Maintenance") {
            var showConfirm by remember { mutableStateOf(false) }
            var crashContent by remember { mutableStateOf<String?>(null) }
            OutlinedButton(onClick = { showConfirm = true }) { Text("Reset to defaults") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                crashContent = com.focusedreader.CrashLogger.read(ctx) ?: "No crash on record."
            }) { Text("View last crash") }
            crashContent?.let { content ->
                AlertDialog(
                    onDismissRequest = { crashContent = null },
                    title = { Text("Last crash") },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(content, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            crashContent = null
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Focused Reader crash log")
                                putExtra(Intent.EXTRA_TEXT, content)
                            }
                            ctx.startActivity(Intent.createChooser(share, "Share crash log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) { Text("Share") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            com.focusedreader.CrashLogger.clear(ctx)
                            crashContent = null
                        }) { Text("Clear") }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val name = "focused-reader-session-${System.currentTimeMillis()}.json"
                exportLauncher.launch(name)
            }) { Text("Export session") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            }) { Text("Import session") }
            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Reset settings?") },
                    text = { Text("All settings will be restored to their defaults. Your imported text is not affected.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirm = false
                            vm.reset()
                            Toast.makeText(ctx, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                        }) { Text("Reset") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                    }
                )
            }
        }
        Section("About") {
            Text(
                "Focused Reader ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            AboutLink(label = "Source on GitHub", url = "https://github.com/blentz/focused-reader-android")
            AboutLink(label = "Privacy policy", url = "https://github.com/blentz/focused-reader-android/blob/main/docs/PRIVACY.md")
            AboutLink(label = "License (MIT)", url = "https://github.com/blentz/focused-reader-android/blob/main/LICENSE")
            Spacer(Modifier.height(12.dp))
            Text(
                "Attributions",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Fonts: OpenDyslexic (OFL), Lexend (OFL), Atkinson Hyperlegible (OFL), Inclusive Sans (OFL).",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Libraries: Jetpack Compose, Hilt, Room, Jsoup.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (expanded) "$title section, expanded" else "$title section, collapsed"
                role = Role.Button
            }
            .clickable { expanded = !expanded }
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (expanded) "▼" else "▶", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
    if (expanded) {
        Column(content = content)
    }
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Text(label)
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

object OrpColorSwatches {
    val values: List<Int> = listOf(
        0xFFE53935.toInt(), // red
        0xFFFB8C00.toInt(), // orange
        0xFFFDD835.toInt(), // yellow
        0xFF43A047.toInt(), // green
        0xFF00ACC1.toInt(), // cyan
        0xFF1E88E5.toInt(), // blue
        0xFF8E24AA.toInt(), // purple
        0xFFEC407A.toInt(), // pink
    )
    val names: Map<Int, String> = mapOf(
        0xFFE53935.toInt() to "red",
        0xFFFB8C00.toInt() to "orange",
        0xFFFDD835.toInt() to "yellow",
        0xFF43A047.toInt() to "green",
        0xFF00ACC1.toInt() to "cyan",
        0xFF1E88E5.toInt() to "blue",
        0xFF8E24AA.toInt() to "purple",
        0xFFEC407A.toInt() to "pink",
    )
}

@Composable
private fun ColorSwatch(argb: Int, selected: Boolean, onClick: () -> Unit) {
    val color = androidx.compose.ui.graphics.Color(argb)
    val border = if (selected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    val name = OrpColorSwatches.names[argb] ?: "color"
    val desc = if (selected) "Highlight color $name, selected" else "Highlight color $name"
    Surface(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                contentDescription = desc
                role = Role.RadioButton
            }
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = color,
        border = border
    ) {}
}

@Composable
private fun AboutLink(label: String, url: String) {
    val ctx = LocalContext.current
    Text(
        label,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                ctx.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
