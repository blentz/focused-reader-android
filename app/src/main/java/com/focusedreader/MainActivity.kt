package com.focusedreader

import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
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
            NfcAdapter.ACTION_NDEF_DISCOVERED -> handleNfcIntent(intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun handleNfcIntent(intent: Intent) {
        // Try NDEF messages payload first.
        val rawMsgs: Array<Parcelable>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, Parcelable::class.java)
            } else {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }
        val payload = rawMsgs?.firstOrNull()?.let { (it as? NdefMessage)?.records?.firstOrNull() }
            ?.let { extractNdefPayload(it) }
        val text = payload ?: intent.dataString
        if (!text.isNullOrBlank()) {
            intentRouter.emit(RouterEvent.ImportText(text))
        }
    }

    private fun extractNdefPayload(record: NdefRecord): String? {
        // Type T (text record): [status][lang...][text...]
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            val payload = record.payload
            if (payload.isEmpty()) return null
            val status = payload[0].toInt()
            val langLen = status and 0x3F
            val encoding = if (status and 0x80 == 0) Charsets.UTF_8 else Charsets.UTF_16
            return runCatching {
                String(payload, 1 + langLen, payload.size - 1 - langLen, encoding)
            }.getOrNull()
        }
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)) {
            return runCatching { record.toUri()?.toString() }.getOrNull()
        }
        if (record.tnf == NdefRecord.TNF_ABSOLUTE_URI) {
            return runCatching { String(record.type, Charsets.UTF_8) }.getOrNull()
        }
        return null
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
