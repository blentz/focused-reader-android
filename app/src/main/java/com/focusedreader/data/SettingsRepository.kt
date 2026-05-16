package com.focusedreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.focusedreader.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("settings")

enum class HapticMode { OFF, PER_WORD, PER_PUNCTUATION }

data class Settings(
    val wpm: Int,
    val wpmStep: Int,
    val resumeDelaySec: Int,
    val faceDownPauseEnabled: Boolean,
    val hapticMode: HapticMode,
    val hapticIntensityPct: Int,
    val ttsEnabled: Boolean,
    val ttsWpmCap: Int,
    val themeMode: ThemeMode,
    val keepScreenAwake: Boolean,
    val font: ReaderFont,
    val orpColorArgb: Int,
    val hasSeenOnboarding: Boolean
)

@Singleton
class SettingsRepository private constructor(private val store: DataStore<Preferences>) {

    @Inject constructor(@ApplicationContext ctx: Context) : this(ctx.settingsStore)

    private object Keys {
        val WPM = intPreferencesKey("wpm")
        val STEP = intPreferencesKey("wpm_step")
        val RESUME = intPreferencesKey("resume_delay")
        val FACE_DOWN = booleanPreferencesKey("face_down")
        val HAPTIC = stringPreferencesKey("haptic_mode")
        val HAPTIC_INT = intPreferencesKey("haptic_int")
        val TTS = booleanPreferencesKey("tts_enabled")
        val TTS_CAP = intPreferencesKey("tts_cap")
        val THEME = stringPreferencesKey("theme")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val FONT = stringPreferencesKey("font")
        val ORP_COLOR = intPreferencesKey("orp_color_argb")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }

    companion object {
        const val DEFAULT_ORP_COLOR_ARGB: Int = 0xFFE53935.toInt() // material red 600

        /** Test-only entry point — allows injecting a JVM-friendly DataStore. */
        internal fun forTesting(store: DataStore<Preferences>): SettingsRepository =
            SettingsRepository(store)
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            wpm = p[Keys.WPM] ?: 300,
            wpmStep = p[Keys.STEP] ?: 50,
            resumeDelaySec = p[Keys.RESUME] ?: 3,
            faceDownPauseEnabled = p[Keys.FACE_DOWN] ?: true,
            hapticMode = HapticMode.valueOf(p[Keys.HAPTIC] ?: HapticMode.OFF.name),
            hapticIntensityPct = p[Keys.HAPTIC_INT] ?: 10,
            ttsEnabled = p[Keys.TTS] ?: false,
            ttsWpmCap = p[Keys.TTS_CAP] ?: 400,
            themeMode = ThemeMode.valueOf(p[Keys.THEME] ?: ThemeMode.AUTO.name),
            keepScreenAwake = p[Keys.KEEP_AWAKE] ?: true,
            font = ReaderFont.valueOf(p[Keys.FONT] ?: ReaderFont.LEXEND.name),
            orpColorArgb = p[Keys.ORP_COLOR] ?: DEFAULT_ORP_COLOR_ARGB,
            hasSeenOnboarding = p[Keys.ONBOARDING_SEEN] ?: false,
        )
    }

    suspend fun setWpm(v: Int) = edit { it[Keys.WPM] = v }
    suspend fun setStep(v: Int) = edit { it[Keys.STEP] = v }
    suspend fun setResumeDelay(v: Int) = edit { it[Keys.RESUME] = v }
    suspend fun setFaceDown(v: Boolean) = edit { it[Keys.FACE_DOWN] = v }
    suspend fun setHapticMode(v: HapticMode) = edit { it[Keys.HAPTIC] = v.name }
    suspend fun setHapticIntensity(v: Int) = edit { it[Keys.HAPTIC_INT] = v.coerceIn(0, 33) }
    suspend fun setTtsEnabled(v: Boolean) = edit { it[Keys.TTS] = v }
    suspend fun setTtsCap(v: Int) = edit { it[Keys.TTS_CAP] = v.coerceIn(100, 900) }
    suspend fun setTheme(v: ThemeMode) = edit { it[Keys.THEME] = v.name }
    suspend fun setKeepAwake(v: Boolean) = edit { it[Keys.KEEP_AWAKE] = v }
    suspend fun setFont(v: ReaderFont) = edit { it[Keys.FONT] = v.name }
    suspend fun setOrpColor(argb: Int) = edit { it[Keys.ORP_COLOR] = argb }
    suspend fun setOnboardingSeen(v: Boolean) = edit { it[Keys.ONBOARDING_SEEN] = v }

    /** Clears all persisted settings; the `settings` flow falls back to defaults. */
    suspend fun resetToDefaults() {
        store.edit { it.clear() }
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit(block)
    }
}
