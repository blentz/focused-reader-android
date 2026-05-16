package com.focusedreader.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TtsCalibrationScreen(onDone: () -> Unit, vm: TtsCalibrationViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.init() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.done) {
            Text("Calibration complete: cap = ${state.current} WPM", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone) { Text("Done") }
        } else {
            Text("Testing ${state.current} WPM", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.speakCurrent() }) { Text("Play sample") }
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = { vm.answer(true) }) { Text("Understandable") }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { vm.answer(false) }) { Text("Too fast") }
            }
        }
    }
}
