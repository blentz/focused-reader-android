package com.focusedreader.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.data.HapticMode
import com.focusedreader.data.ReaderFont
import com.focusedreader.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onCalibrateTts: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val current = s ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Section("Speed") {
            SliderRow("WPM step (${current.wpmStep})", current.wpmStep.toFloat(), 5f..100f, 18) { vm.setStep(it.toInt()) }
            SliderRow("WPM (${current.wpm})", current.wpm.toFloat(), 100f..900f, 16) { vm.setWpm(it.toInt()) }
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
        Section("Capture") {
            Button(onClick = {
                ctx.startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("Open Accessibility Settings") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    Column(content = content)
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Float) -> Unit) {
    Text(label)
    Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
