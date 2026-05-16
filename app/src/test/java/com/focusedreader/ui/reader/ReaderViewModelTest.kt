package com.focusedreader.ui.reader

import com.focusedreader.data.HapticMode
import com.focusedreader.data.ImportSource
import com.focusedreader.data.ReaderFont
import com.focusedreader.data.Session
import com.focusedreader.data.SessionRepository
import com.focusedreader.data.Settings
import com.focusedreader.data.SettingsRepository
import com.focusedreader.reader.FaceOrientation
import com.focusedreader.reader.HapticController
import com.focusedreader.reader.OrientationMonitor
import com.focusedreader.reader.ReaderState
import com.focusedreader.reader.TtsController
import com.focusedreader.reader.Wpm
import com.focusedreader.ui.theme.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coJustRun
import io.mockk.every
import kotlinx.coroutines.async
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val defaultSettings = Settings(
        wpm = 300,
        wpmStep = 50,
        resumeDelaySec = 3,
        faceDownPauseEnabled = false,
        hapticMode = HapticMode.OFF,
        hapticIntensityPct = 0,
        ttsEnabled = false,
        ttsWpmCap = 400,
        themeMode = ThemeMode.AUTO,
        keepScreenAwake = true,
        font = ReaderFont.LEXEND,
        orpColorArgb = SettingsRepository.DEFAULT_ORP_COLOR_ARGB,
        hasSeenOnboarding = true
    )

    private lateinit var sessions: SessionRepository
    private lateinit var settings: SettingsRepository
    private lateinit var orientation: OrientationMonitor
    private lateinit var haptic: HapticController
    private lateinit var tts: TtsController
    private lateinit var settingsFlow: MutableStateFlow<Settings>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        sessions = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        orientation = mockk(relaxed = true)
        haptic = mockk(relaxed = true)
        tts = mockk(relaxed = true)

        settingsFlow = MutableStateFlow(defaultSettings)
        every { settings.settings } returns settingsFlow
        every { orientation.orientationEvents(any()) } returns emptyFlow()
        coJustRun { sessions.updatePosition(any()) }
        coJustRun { settings.setWpm(any()) }
        coEvery { tts.init() } returns true
        every { tts.shutdown() } just runs
        every { tts.speak(any()) } just runs
        every { haptic.tick(any(), any(), any()) } just runs
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vmWithSession(session: Session?): ReaderViewModel {
        coEvery { sessions.current() } returns session
        return ReaderViewModel(sessions, settings, orientation, haptic, tts, dispatcher)
    }

    @Test
    fun `no session keeps state Idle`() = runTest(dispatcher) {
        val vm = vmWithSession(null)
        runCurrent()
        assertEquals(ReaderState.Idle, vm.state.value)
    }

    @Test
    fun `with session enters Reading at saved position`() = runTest(dispatcher) {
        val text = "one two three four five"
        val vm = vmWithSession(
            Session(text = text, position = 2, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        val state = vm.state.value
        assertTrue(state is ReaderState.Reading)
        val reading = state as ReaderState.Reading
        assertEquals(2, reading.index)
        assertEquals(300, reading.wpm)
        assertEquals(listOf("one", "two", "three", "four", "five"), reading.tokens)
    }

    @Test
    fun `bumpWpm clamps to default max when tts off`() = runTest(dispatcher) {
        val vm = vmWithSession(
            Session(text = "one two", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        vm.bumpWpm(5_000) // way past max
        runCurrent()
        val state = vm.state.value as ReaderState.Reading
        assertEquals(Wpm.DEFAULT_MAX, state.wpm)
        coVerify { settings.setWpm(Wpm.DEFAULT_MAX) }
    }

    @Test
    fun `bumpWpm clamps to tts cap when tts enabled`() = runTest(dispatcher) {
        settingsFlow.value = defaultSettings.copy(ttsEnabled = true, ttsWpmCap = 350)
        val vm = vmWithSession(
            Session(text = "one two", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        vm.bumpWpm(5_000)
        runCurrent()
        val state = vm.state.value as ReaderState.Reading
        assertEquals(350, state.wpm)
    }

    @Test
    fun `bumpWpm emits on wpmHudPulse`() = runTest(dispatcher) {
        val vm = vmWithSession(
            Session(text = "one two three", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        // Replay buffer is 0; subscribe before bumping.
        val pulse = async { vm.wpmHudPulse.first() }
        runCurrent()
        vm.bumpWpm(50)
        runCurrent()
        assertEquals(350, pulse.await())
    }

    @Test
    fun `seekTo clamps to range`() = runTest(dispatcher) {
        val vm = vmWithSession(
            Session(text = "a b c d", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        vm.seekTo(9999)
        runCurrent()
        val s1 = vm.state.value as ReaderState.Reading
        assertEquals(3, s1.index)

        vm.seekTo(-5)
        runCurrent()
        val s2 = vm.state.value as ReaderState.Reading
        assertEquals(0, s2.index)
    }

    @Test
    fun `togglePause transitions Reading to Paused and persists position`() = runTest(dispatcher) {
        val vm = vmWithSession(
            Session(text = "a b c d e", position = 1, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        vm.togglePause()
        runCurrent()
        val state = vm.state.value
        assertTrue(state is ReaderState.Paused, "expected Paused, was $state")
        coVerify { sessions.updatePosition(1) }
    }

    @Test
    fun `stop transitions to Idle`() = runTest(dispatcher) {
        val vm = vmWithSession(
            Session(text = "a b c", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        vm.stop()
        runCurrent()
        assertEquals(ReaderState.Idle, vm.state.value)
    }

    @Test
    fun `enabling TTS mid-session calls tts init`() = runTest(dispatcher) {
        val vm = vmWithSession(
            Session(text = "a b c", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        settingsFlow.value = defaultSettings.copy(ttsEnabled = true)
        runCurrent()
        coVerify(atLeast = 1) { tts.init() }
    }

    @Test
    fun `disabling TTS calls tts shutdown`() = runTest(dispatcher) {
        settingsFlow.value = defaultSettings.copy(ttsEnabled = true)
        val vm = vmWithSession(
            Session(text = "a b", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        runCurrent()
        settingsFlow.value = defaultSettings.copy(ttsEnabled = false)
        runCurrent()
        verify(atLeast = 1) { tts.shutdown() }
    }

    @Test
    fun `engine completion resets state to Idle after delay`() = runTest(dispatcher) {
        // Very short text + max wpm so the engine completes quickly under virtual time.
        val vm = vmWithSession(
            Session(text = "a b", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        )
        advanceUntilIdle()
        // Advance enough virtual time for the engine to walk both tokens then run the
        // completion callback (which delays 1500ms).
        advanceTimeBy(30_000)
        advanceUntilIdle()
        assertEquals(ReaderState.Idle, vm.state.value)
        coVerify { sessions.updatePosition(0) }
    }
}
