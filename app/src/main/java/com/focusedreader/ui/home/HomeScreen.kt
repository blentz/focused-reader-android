package com.focusedreader.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.capture.PermissionStatus
import com.focusedreader.data.Session
import com.focusedreader.reader.WordTokenizer

@Composable
fun HomeScreen(
    onRead: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val session by vm.session.collectAsState()
    val isImporting by vm.isImporting.collectAsState()
    val showOnboarding by vm.showOnboarding.collectAsState()
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importFromFile(uri) { result ->
                val msg = when (result) {
                    ImportTextUseCase.Result.Empty -> "File was empty"
                    ImportTextUseCase.Result.Ok -> "Imported from file"
                    ImportTextUseCase.Result.FetchFailed -> "Failed to read file"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    HomeScreenContent(
        session = session,
        a11yEnabled = a11yEnabled,
        isImporting = isImporting,
        showOnboarding = showOnboarding,
        onDismissOnboarding = vm::markOnboardingComplete,
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
        },
        onOpenFile = { filePickerLauncher.launch(arrayOf("text/plain")) }
    )
}

@Composable
fun HomeScreenContent(
    session: Session?,
    a11yEnabled: Boolean,
    onRead: () -> Unit,
    onSettings: () -> Unit,
    onEnableA11y: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onOpenFile: () -> Unit,
    isImporting: Boolean = false,
    showOnboarding: Boolean = false,
    onDismissOnboarding: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val base = LocalDensity.current
    val scaled = Density(density = base.density, fontScale = base.fontScale * 1.25f)
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalDensity provides scaled) {
            HomeBody(
                session = session,
                a11yEnabled = a11yEnabled,
                onRead = onRead,
                onSettings = onSettings,
                onEnableA11y = onEnableA11y,
                onPasteFromClipboard = onPasteFromClipboard,
                onOpenFile = onOpenFile
            )
        }
        Text(
            "©",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .clickable {
                    ctx.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/blentz/focused-reader-android/blob/main/LICENSE")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
        )

        if (isImporting) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(enabled = false) {}
                    .then(Modifier),
                contentAlignment = Alignment.Center
            ) {
                // Translucent scrim
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(enabled = false) {}
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Importing…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (showOnboarding) {
            OnboardingDialog(onDismiss = onDismissOnboarding)
        }
    }
}

@Composable
private fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* require explicit dismissal */ },
        title = { Text("Welcome to Focused Reader") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Share text from any app: pick Focused Reader in the system share sheet. " +
                        "URLs are fetched and stripped down to readable text automatically."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Accessibility capture: enable the Focused Reader accessibility service to " +
                        "grab the text on screen with one tap from the Quick Settings tile."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Clipboard import: copy text or a URL, then tap Paste from clipboard on the home screen."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Open a file: tap Open file… to read any local .txt file directly."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
private fun HomeBody(
    session: Session?,
    a11yEnabled: Boolean,
    onRead: () -> Unit,
    onSettings: () -> Unit,
    onEnableA11y: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onOpenFile: () -> Unit
) {
    val totalWords = remember(session?.text) {
        session?.text?.let { WordTokenizer.tokenize(it).size } ?: 0
    }
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
            Text("Position: ${it.position} / $totalWords words", style = MaterialTheme.typography.bodySmall)
        } ?: Text("No imported text yet", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRead, enabled = session != null) { Text("Read") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onPasteFromClipboard) { Text("Paste from clipboard") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenFile) { Text("Open file…") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSettings) { Text("Settings") }
    }
}
