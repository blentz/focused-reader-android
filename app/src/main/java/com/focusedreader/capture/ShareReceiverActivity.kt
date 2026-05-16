package com.focusedreader.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.focusedreader.MainActivity
import com.focusedreader.data.ImportSource
import com.focusedreader.ui.theme.FocusedReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject lateinit var importer: ImportTextUseCase
    @Inject lateinit var urlFetcher: UrlFetcher

    private var importJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.let { i ->
            i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                ?: i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT_READONLY)?.toString()
                ?: i.getStringExtra(Intent.EXTRA_TEXT)
        }
        val isUrl = text != null && urlFetcher.looksLikeUrl(text.trim())

        // For URL imports, show a visible loading UI since fetch may take up to 15s.
        // For plain text, keep the translucent fast-path (no setContent) so the
        // activity disappears within milliseconds without a UI flash.
        if (isUrl) {
            setContent {
                FocusedReaderTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Fetching URL…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.height(24.dp))
                                OutlinedButton(onClick = {
                                    importJob?.cancel()
                                    Toast.makeText(this@ShareReceiverActivity, "Cancelled", Toast.LENGTH_SHORT).show()
                                    finish()
                                }) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
        }

        importJob = lifecycleScope.launch {
            if (!isUrl) {
                Toast.makeText(this@ShareReceiverActivity, "Importing…", Toast.LENGTH_SHORT).show()
            }
            when (importer(text, ImportSource.SHARE)) {
                ImportTextUseCase.Result.Empty ->
                    Toast.makeText(this@ShareReceiverActivity, "No text to read", Toast.LENGTH_SHORT).show()
                ImportTextUseCase.Result.FetchFailed ->
                    Toast.makeText(this@ShareReceiverActivity, "Failed to fetch URL", Toast.LENGTH_LONG).show()
                ImportTextUseCase.Result.Ok ->
                    startActivity(Intent(this@ShareReceiverActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            finish()
        }
    }
}
