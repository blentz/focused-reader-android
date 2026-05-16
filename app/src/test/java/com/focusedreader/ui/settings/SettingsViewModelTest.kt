package com.focusedreader.ui.settings

import com.focusedreader.data.HapticMode
import com.focusedreader.data.ReaderFont
import com.focusedreader.data.Settings
import com.focusedreader.data.SessionRepository
import com.focusedreader.data.SettingsRepository
import com.focusedreader.ui.theme.ThemeMode
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val defaults = Settings(
        wpm = 300,
        wpmStep = 50,
        resumeDelaySec = 3,
        faceDownPauseEnabled = true,
        hapticMode = HapticMode.OFF,
        hapticIntensityPct = 10,
        ttsEnabled = false,
        ttsWpmCap = 400,
        themeMode = ThemeMode.AUTO,
        keepScreenAwake = true,
        font = ReaderFont.LEXEND,
        orpColorArgb = SettingsRepository.DEFAULT_ORP_COLOR_ARGB,
        hasSeenOnboarding = false
    )

    private lateinit var repo: SettingsRepository
    private lateinit var sessions: SessionRepository
    private lateinit var flow: MutableStateFlow<Settings>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = mockk(relaxed = true)
        sessions = mockk(relaxed = true)
        flow = MutableStateFlow(defaults)
        every { repo.settings } returns flow
        coJustRun { repo.setWpm(any()) }
        coJustRun { repo.setStep(any()) }
        coJustRun { repo.setResumeDelay(any()) }
        coJustRun { repo.setFaceDown(any()) }
        coJustRun { repo.setHapticMode(any()) }
        coJustRun { repo.setHapticIntensity(any()) }
        coJustRun { repo.setTtsEnabled(any()) }
        coJustRun { repo.setTheme(any()) }
        coJustRun { repo.setKeepAwake(any()) }
        coJustRun { repo.setFont(any()) }
        coJustRun { repo.setOrpColor(any()) }
        coJustRun { repo.resetToDefaults() }
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SettingsViewModel(repo, sessions)

    @Test
    fun `settings flow proxies repo`() = runTest(dispatcher) {
        val vm = vm()
        val job = launch { vm.settings.collect {} }
        runCurrent()
        assertSame(defaults, vm.settings.value)
        val updated = defaults.copy(wpm = 500)
        flow.value = updated
        runCurrent()
        assertEquals(500, vm.settings.value?.wpm)
        job.cancel()
    }

    @Test
    fun `setWpm forwards`() = runTest(dispatcher) {
        vm().setWpm(420); runCurrent()
        coVerify { repo.setWpm(420) }
    }

    @Test
    fun `setStep forwards`() = runTest(dispatcher) {
        vm().setStep(25); runCurrent()
        coVerify { repo.setStep(25) }
    }

    @Test
    fun `setResume forwards`() = runTest(dispatcher) {
        vm().setResume(7); runCurrent()
        coVerify { repo.setResumeDelay(7) }
    }

    @Test
    fun `setFaceDown forwards`() = runTest(dispatcher) {
        vm().setFaceDown(false); runCurrent()
        coVerify { repo.setFaceDown(false) }
    }

    @Test
    fun `setHaptic forwards`() = runTest(dispatcher) {
        vm().setHaptic(HapticMode.PER_PUNCTUATION); runCurrent()
        coVerify { repo.setHapticMode(HapticMode.PER_PUNCTUATION) }
    }

    @Test
    fun `setHapticIntensity forwards`() = runTest(dispatcher) {
        vm().setHapticIntensity(18); runCurrent()
        coVerify { repo.setHapticIntensity(18) }
    }

    @Test
    fun `setTts forwards`() = runTest(dispatcher) {
        vm().setTts(true); runCurrent()
        coVerify { repo.setTtsEnabled(true) }
    }

    @Test
    fun `setTheme forwards`() = runTest(dispatcher) {
        vm().setTheme(ThemeMode.DARK); runCurrent()
        coVerify { repo.setTheme(ThemeMode.DARK) }
    }

    @Test
    fun `setKeepAwake forwards`() = runTest(dispatcher) {
        vm().setKeepAwake(false); runCurrent()
        coVerify { repo.setKeepAwake(false) }
    }

    @Test
    fun `setFont forwards`() = runTest(dispatcher) {
        vm().setFont(ReaderFont.ATKINSON_HYPERLEGIBLE); runCurrent()
        coVerify { repo.setFont(ReaderFont.ATKINSON_HYPERLEGIBLE) }
    }

    @Test
    fun `setOrpColor forwards`() = runTest(dispatcher) {
        vm().setOrpColor(0x12345678); runCurrent()
        coVerify { repo.setOrpColor(0x12345678) }
    }

    @Test
    fun `reset forwards`() = runTest(dispatcher) {
        vm().reset(); runCurrent()
        coVerify { repo.resetToDefaults() }
    }
}
