package com.focusedreader.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onRead: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focused Reader")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRead) { Text("Read") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSettings) { Text("Settings") }
    }
}
