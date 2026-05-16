package com.focusedreader.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.focusedreader.MainActivity
import com.focusedreader.data.ImportSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject lateinit var importer: ImportTextUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val result = runBlocking { importer(text, ImportSource.SHARE) }
        when (result) {
            ImportTextUseCase.Result.Empty -> Toast.makeText(this, "No text to read", Toast.LENGTH_SHORT).show()
            ImportTextUseCase.Result.Ok -> startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}
