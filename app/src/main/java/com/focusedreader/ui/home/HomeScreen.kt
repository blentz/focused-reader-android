package com.focusedreader.ui.home

import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.capture.PermissionStatus
import com.focusedreader.data.Session

@Composable
fun HomeScreen(
    onRead: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val session by vm.session.collectAsState()
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var a11yEnabled by remember { mutableStateOf(PermissionStatus.isAccessibilityServiceEnabled(ctx)) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                a11yEnabled = PermissionStatus.isAccessibilityServiceEnabled(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    HomeScreenContent(
        session = session,
        a11yEnabled = a11yEnabled,
        onRead = onRead,
        onSettings = onSettings,
        onEnableA11y = {
            ctx.startActivity(
                Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
        onPasteFromClipboard = {
            vm.importFromClipboard { result ->
                val msg = when (result) {
                    ImportTextUseCase.Result.Empty -> "Clipboard is empty"
                    ImportTextUseCase.Result.Ok -> "Imported from clipboard"
                    ImportTextUseCase.Result.FetchFailed -> "Failed to fetch URL"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
fun HomeScreenContent(
    session: Session?,
    a11yEnabled: Boolean,
    onRead: () -> Unit,
    onSettings: () -> Unit,
    onEnableA11y: () -> Unit,
    onPasteFromClipboard: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focused Reader", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (!a11yEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Accessibility capture is disabled",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "Enable in system settings to use the Quick Settings tile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onEnableA11y) { Text("Enable") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
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
