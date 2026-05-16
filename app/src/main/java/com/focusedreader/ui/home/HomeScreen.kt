package com.focusedreader.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.data.Session
import android.widget.Toast

@Composable
fun HomeScreen(
    onRead: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val session by vm.session.collectAsState()
    val ctx = LocalContext.current
    HomeScreenContent(
        session = session,
        onRead = onRead,
        onSettings = onSettings,
        onPasteFromClipboard = {
            vm.importFromClipboard { result ->
                val msg = when (result) {
                    ImportTextUseCase.Result.Empty -> "Clipboard is empty"
                    ImportTextUseCase.Result.Ok -> "Imported from clipboard"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
fun HomeScreenContent(
    session: Session?,
    onRead: () -> Unit,
    onSettings: () -> Unit,
    onPasteFromClipboard: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focused Reader", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        session?.let {
            Text("Last import: ${it.source.name}", style = MaterialTheme.typography.bodyMedium)
            Text(it.text.take(80) + if (it.text.length > 80) "…" else "", style = MaterialTheme.typography.bodySmall)
            Text("Position: ${it.position} / words", style = MaterialTheme.typography.bodySmall)
        } ?: Text("No imported text yet", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRead, enabled = session != null) { Text("Read") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onPasteFromClipboard) { Text("Paste from clipboard") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSettings) { Text("Settings") }
    }
}
