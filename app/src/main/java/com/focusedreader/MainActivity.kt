package com.focusedreader

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.focusedreader.capture.IntentRouter
import com.focusedreader.capture.RouterEvent
import com.focusedreader.capture.ShareReceiverActivity
import com.focusedreader.data.SettingsRepository
import com.focusedreader.nav.FocusedReaderNavGraph
import com.focusedreader.ui.theme.FocusedReaderTheme
import com.focusedreader.ui.theme.ThemeMode
import com.focusedreader.ui.theme.effectiveThemeMode
import com.focusedreader.ui.theme.palette
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var intentRouter: IntentRouter
    val keyEvents = MutableSharedFlow<Int>(extraBufferCapacity = 16) // KEYCODE_VOLUME_UP / DOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        publishShareTargetShortcut()
        handleIntent(intent)
        setContent {
            val s by settings.settings.collectAsState(initial = null)
            val themeMode = s?.themeMode ?: ThemeMode.AUTO
            val systemDark = isSystemInDarkTheme()
            val effective = effectiveThemeMode(themeMode, systemDark)
            val basePalette = palette(effective)
            val orpOverride = s?.orpColorArgb?.let { androidx.compose.ui.graphics.Color(it) }
            val finalPalette = if (orpOverride != null) basePalette.copy(orp = orpOverride) else basePalette
            FocusedReaderTheme(
                darkTheme = effective == ThemeMode.DARK,
                readerPalette = finalPalette
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            "com.focusedreader.action.PASTE_CLIPBOARD" -> intentRouter.emit(RouterEvent.PasteClipboard)
            "com.focusedreader.action.RESUME" -> intentRouter.emit(RouterEvent.Resume)
            "com.focusedreader.action.OPEN_FILE" -> intentRouter.emit(RouterEvent.OpenFile)
        }
    }

    private fun publishShareTargetShortcut() {
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND)
                .setClass(this, ShareReceiverActivity::class.java)
                .setType("text/plain")
            val shortcut = ShortcutInfoCompat.Builder(this, "share_target")
                .setShortLabel(getString(R.string.share_target_short))
                .setLongLabel(getString(R.string.share_target_long))
                .setIcon(IconCompat.createWithResource(this, android.R.drawable.ic_menu_edit))
                .setIntent(sendIntent)
                .setCategories(setOf("android.shortcut.conversation"))
                .setLongLived(true)
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
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
