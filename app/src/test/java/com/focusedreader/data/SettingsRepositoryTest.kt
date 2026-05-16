package com.focusedreader.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.focusedreader.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var scope: CoroutineScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: SettingsRepository

    @BeforeEach
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(tempDir, "settings_${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create(scope = scope) { file }
        repo = SettingsRepository.forTesting(store)
    }

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `empty store yields all defaults`() = runBlocking {
        val s = repo.settings.first()
        assertEquals(300, s.wpm)
        assertEquals(50, s.wpmStep)
        assertEquals(3, s.resumeDelaySec)
        assertTrue(s.faceDownPauseEnabled)
        assertEquals(HapticMode.OFF, s.hapticMode)
        assertEquals(10, s.hapticIntensityPct)
        assertFalse(s.ttsEnabled)
        assertEquals(400, s.ttsWpmCap)
        assertEquals(ThemeMode.AUTO, s.themeMode)
        assertTrue(s.keepScreenAwake)
        assertEquals(ReaderFont.LEXEND, s.font)
        assertEquals(SettingsRepository.DEFAULT_ORP_COLOR_ARGB, s.orpColorArgb)
        assertFalse(s.hasSeenOnboarding)
        assertFalse(s.seenReverseHint)
    }

    @Test
    fun `setReverseHintSeen persists`() = runBlocking {
        repo.setReverseHintSeen(true)
        assertTrue(repo.settings.first().seenReverseHint)
    }

    @Test
    fun `setWpm persists and emits`() = runBlocking {
        repo.setWpm(525)
        assertEquals(525, repo.settings.first().wpm)
    }

    @Test
    fun `setStep persists`() = runBlocking {
        repo.setStep(25)
        assertEquals(25, repo.settings.first().wpmStep)
    }

    @Test
    fun `setResumeDelay persists`() = runBlocking {
        repo.setResumeDelay(7)
        assertEquals(7, repo.settings.first().resumeDelaySec)
    }

    @Test
    fun `setFaceDown persists`() = runBlocking {
        repo.setFaceDown(false)
        assertFalse(repo.settings.first().faceDownPauseEnabled)
    }

    @Test
    fun `setHapticMode persists`() = runBlocking {
        repo.setHapticMode(HapticMode.PER_PUNCTUATION)
        assertEquals(HapticMode.PER_PUNCTUATION, repo.settings.first().hapticMode)
    }

    @Test
    fun `setHapticIntensity clamps below zero`() = runBlocking {
        repo.setHapticIntensity(-5)
        assertEquals(0, repo.settings.first().hapticIntensityPct)
    }

    @Test
    fun `setHapticIntensity clamps above 33`() = runBlocking {
        repo.setHapticIntensity(50)
        assertEquals(33, repo.settings.first().hapticIntensityPct)
    }

    @Test
    fun `setHapticIntensity in-range stored as-is`() = runBlocking {
        repo.setHapticIntensity(20)
        assertEquals(20, repo.settings.first().hapticIntensityPct)
    }

    @Test
    fun `setTtsEnabled persists`() = runBlocking {
        repo.setTtsEnabled(true)
        assertTrue(repo.settings.first().ttsEnabled)
    }

    @Test
    fun `setTtsCap clamps below 100`() = runBlocking {
        repo.setTtsCap(50)
        assertEquals(100, repo.settings.first().ttsWpmCap)
    }

    @Test
    fun `setTtsCap clamps above 900`() = runBlocking {
        repo.setTtsCap(1500)
        assertEquals(900, repo.settings.first().ttsWpmCap)
    }

    @Test
    fun `setTtsCap in-range stored as-is`() = runBlocking {
        repo.setTtsCap(500)
        assertEquals(500, repo.settings.first().ttsWpmCap)
    }

    @Test
    fun `setTheme persists`() = runBlocking {
        repo.setTheme(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.settings.first().themeMode)
    }

    @Test
    fun `setKeepAwake persists`() = runBlocking {
        repo.setKeepAwake(false)
        assertFalse(repo.settings.first().keepScreenAwake)
    }

    @Test
    fun `setFont persists`() = runBlocking {
        // Pick a non-default font for proof of write.
        val other = ReaderFont.values().first { it != ReaderFont.LEXEND }
        repo.setFont(other)
        assertEquals(other, repo.settings.first().font)
    }

    @Test
    fun `setOrpColor persists`() = runBlocking {
        repo.setOrpColor(0xFF00FF00.toInt())
        assertEquals(0xFF00FF00.toInt(), repo.settings.first().orpColorArgb)
    }

    @Test
    fun `setOnboardingSeen persists`() = runBlocking {
        repo.setOnboardingSeen(true)
        assertTrue(repo.settings.first().hasSeenOnboarding)
    }

    @Test
    fun `resetToDefaults clears everything`() = runBlocking {
        repo.setWpm(700)
        repo.setTtsEnabled(true)
        repo.setOnboardingSeen(true)
        repo.setTheme(ThemeMode.DARK)
        repo.setOrpColor(0xFF112233.toInt())

        repo.resetToDefaults()

        val s = repo.settings.first()
        assertEquals(300, s.wpm)
        assertFalse(s.ttsEnabled)
        assertFalse(s.hasSeenOnboarding)
        assertEquals(ThemeMode.AUTO, s.themeMode)
        assertEquals(SettingsRepository.DEFAULT_ORP_COLOR_ARGB, s.orpColorArgb)
    }
}
