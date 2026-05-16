package com.focusedreader.ui.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.capture.RouterEvent
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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importFromFile(uri) { result ->
                val msg = when (result) {
                    ImportTextUseCase.Result.Empty -> "File was empty"
                    ImportTextUseCase.Result.Ok -> "Imported from file"
                    ImportTextUseCase.Result.FetchFailed -> "Failed to read file"
                    is ImportTextUseCase.Result.FileTooLarge -> "File too large (${result.sizeBytes / 1024 / 1024} MB; max 50 MB)"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    HomeScreenContent(
        session = session,
        isImporting = isImporting,
        showOnboarding = showOnboarding,
        onDismissOnboarding = vm::markOnboardingComplete,
        onRead = onRead,
        onSettings = onSettings,
        onPasteFromClipboard = {
            vm.importFromClipboard { result ->
                val msg = when (result) {
                    ImportTextUseCase.Result.Empty -> "Clipboard is empty"
                    ImportTextUseCase.Result.Ok -> "Imported from clipboard"
                    ImportTextUseCase.Result.FetchFailed -> "Failed to fetch URL"
                    is ImportTextUseCase.Result.FileTooLarge -> "File too large"
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }
        },
        onOpenFile = {
            filePickerLauncher.launch(
                arrayOf("text/plain", "text/html", "text/markdown")
            )
        }
    )

    LaunchedEffect(Unit) {
        vm.routerEvents.collect { event ->
            when (event) {
                RouterEvent.PasteClipboard -> vm.importFromClipboard { result ->
                    val msg = when (result) {
                        ImportTextUseCase.Result.Empty -> "Clipboard is empty"
                        ImportTextUseCase.Result.Ok -> "Imported from clipboard"
                        ImportTextUseCase.Result.FetchFailed -> "Failed to fetch URL"
                        is ImportTextUseCase.Result.FileTooLarge -> "File too large"
                    }
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                }
                RouterEvent.Resume -> if (session != null) onRead()
                RouterEvent.OpenFile -> filePickerLauncher.launch(
                    arrayOf("text/plain", "text/html", "text/markdown")
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    session: Session?,
    onRead: () -> Unit,
    onSettings: () -> Unit,
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
                onRead = onRead,
                onSettings = onSettings,
                onPasteFromClipboard = onPasteFromClipboard,
                onOpenFile = onOpenFile
            )
        }
        Text(
            "©",
            fontSize = androidx.compose.ui.unit.TextUnit(42f, androidx.compose.ui.unit.TextUnitType.Sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .semantics {
                    contentDescription = "View license"
                    role = Role.Button
                }
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
                    "Clipboard import: copy text or a URL, then tap Paste from clipboard on the home screen."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Open a file: tap Open file… to read any local .txt / .md / .html file directly."
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
    onRead: () -> Unit,
    onSettings: () -> Unit,
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
