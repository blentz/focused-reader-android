package com.focusedreader.ui.home

import android.content.Context
import android.net.Uri
import com.focusedreader.capture.ClipboardImporter
import com.focusedreader.capture.FilePicker
import com.focusedreader.capture.ImportTextUseCase
import com.focusedreader.capture.IntentRouter
import com.focusedreader.data.HapticMode
import com.focusedreader.data.ImportSource
import com.focusedreader.data.ReaderFont
import com.focusedreader.data.Session
import com.focusedreader.data.SessionRepository
import com.focusedreader.data.Settings
import com.focusedreader.data.SettingsRepository
import com.focusedreader.ui.theme.ThemeMode
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val baseSettings = Settings(
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

    private lateinit var sessions: SessionRepository
    private lateinit var clipboard: ClipboardImporter
    private lateinit var importer: ImportTextUseCase
    private lateinit var filePicker: FilePicker
    private lateinit var settings: SettingsRepository
    private lateinit var router: IntentRouter
    private lateinit var ctx: Context

    private lateinit var sessionFlow: MutableStateFlow<Session?>
    private lateinit var settingsFlow: MutableStateFlow<Settings>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        sessions = mockk(relaxed = true)
        clipboard = mockk(relaxed = true)
        importer = mockk(relaxed = true)
        filePicker = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        router = mockk(relaxed = true)
        ctx = mockk(relaxed = true)

        sessionFlow = MutableStateFlow(null)
        settingsFlow = MutableStateFlow(baseSettings)
        every { sessions.observe() } returns sessionFlow
        every { settings.settings } returns settingsFlow
        every { router.actions } returns MutableSharedFlow()
        coJustRun { settings.setOnboardingSeen(any()) }
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = HomeViewModel(sessions, clipboard, importer, filePicker, settings, router, ctx)

    @Test
    fun `session proxies repo observe`() = runTest(dispatcher) {
        val s = Session(text = "hi", position = 0, source = ImportSource.SHARE, importedAt = 0L)
        val vm = vm()
        val job = launch { vm.session.collect {} }
        runCurrent()
        sessionFlow.value = s
        runCurrent()
        assertEquals(s, vm.session.value)
        job.cancel()
    }

    @Test
    fun `showOnboarding mirrors negation of hasSeenOnboarding`() = runTest(dispatcher) {
        val vm = vm()
        val job = launch { vm.showOnboarding.collect {} }
        runCurrent()
        assertTrue(vm.showOnboarding.value)
        settingsFlow.value = baseSettings.copy(hasSeenOnboarding = true)
        runCurrent()
        assertFalse(vm.showOnboarding.value)
        job.cancel()
    }

    @Test
    fun `importFromClipboard delegates and toggles isImporting`() = runTest(dispatcher) {
        coEvery { clipboard.importFromClipboard() } returns ImportTextUseCase.Result.Ok
        val vm = vm()
        var received: ImportTextUseCase.Result? = null
        vm.importFromClipboard { received = it }
        // before runCurrent the launch hasn't started
        runCurrent()
        assertEquals(ImportTextUseCase.Result.Ok, received)
        assertFalse(vm.isImporting.value)
        coVerify { clipboard.importFromClipboard() }
    }

    @Test
    fun `importFromFile reads file then imports with FILE source`() = runTest(dispatcher) {
        val uri = mockk<Uri>(relaxed = true)
        coEvery { filePicker.read(ctx, uri) } returns FilePicker.Result.Ok("some body")
        coEvery { importer.invoke("some body", ImportSource.FILE) } returns ImportTextUseCase.Result.Ok
        val vm = vm()
        var received: ImportTextUseCase.Result? = null
        vm.importFromFile(uri) { received = it }
        runCurrent()
        assertEquals(ImportTextUseCase.Result.Ok, received)
        assertFalse(vm.isImporting.value)
        coVerify { filePicker.read(ctx, uri) }
        coVerify { importer.invoke("some body", ImportSource.FILE) }
    }

    @Test
    fun `importFromFile forwards FetchFailed when file unreadable`() = runTest(dispatcher) {
        val uri = mockk<Uri>(relaxed = true)
        coEvery { filePicker.read(ctx, uri) } returns FilePicker.Result.ReadFailed
        val vm = vm()
        var received: ImportTextUseCase.Result? = null
        vm.importFromFile(uri) { received = it }
        runCurrent()
        assertEquals(ImportTextUseCase.Result.FetchFailed, received)
    }

    @Test
    fun `importFromFile forwards FileTooLarge when oversized`() = runTest(dispatcher) {
        val uri = mockk<Uri>(relaxed = true)
        coEvery { filePicker.read(ctx, uri) } returns FilePicker.Result.TooLarge(99_000_000)
        val vm = vm()
        var received: ImportTextUseCase.Result? = null
        vm.importFromFile(uri) { received = it }
        runCurrent()
        assertEquals(99_000_000L, (received as ImportTextUseCase.Result.FileTooLarge).sizeBytes)
    }

    @Test
    fun `importRawText delegates with SHARE source`() = runTest(dispatcher) {
        coEvery { importer.invoke("hello", ImportSource.SHARE) } returns ImportTextUseCase.Result.Ok
        val vm = vm()
        var received: ImportTextUseCase.Result? = null
        vm.importRawText("hello") { received = it }
        runCurrent()
        assertEquals(ImportTextUseCase.Result.Ok, received)
        assertFalse(vm.isImporting.value)
        coVerify { importer.invoke("hello", ImportSource.SHARE) }
    }

    @Test
    fun `markOnboardingComplete sets flag`() = runTest(dispatcher) {
        vm().markOnboardingComplete()
        runCurrent()
        coVerify { settings.setOnboardingSeen(true) }
    }
}
