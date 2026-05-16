package com.focusedreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.focusedreader.data.SettingsRepository
import com.focusedreader.nav.FocusedReaderNavGraph
import com.focusedreader.ui.theme.FocusedReaderTheme
import com.focusedreader.ui.theme.PaletteMode
import com.focusedreader.ui.theme.ThemeMode
import com.focusedreader.ui.theme.effectiveThemeMode
import com.focusedreader.ui.theme.palette
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository
    val keyEvents = MutableSharedFlow<Int>(extraBufferCapacity = 16) // KEYCODE_VOLUME_UP / DOWN

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user choice — no follow-up needed for POC */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        setContent {
            val s by settings.settings.collectAsState(initial = null)
            val themeMode = s?.themeMode ?: ThemeMode.AUTO
            val paletteMode = s?.paletteMode ?: PaletteMode.SOFT
            val systemDark = isSystemInDarkTheme()
            val effective = effectiveThemeMode(themeMode, systemDark)
            FocusedReaderTheme(
                darkTheme = effective == ThemeMode.DARK,
                readerPalette = palette(effective, paletteMode)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FocusedReaderNavGraph()
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val perm = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) return
        requestNotificationPermission.launch(perm)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                keyEvents.tryEmit(keyCode)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
