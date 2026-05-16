package com.focusedreader.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.focusedreader.MainActivity
import com.focusedreader.data.ImportSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject lateinit var importer: ImportTextUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        lifecycleScope.launch {
            Toast.makeText(this@ShareReceiverActivity, "Importing…", Toast.LENGTH_SHORT).show()
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
