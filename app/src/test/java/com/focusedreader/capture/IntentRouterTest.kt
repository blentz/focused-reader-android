package com.focusedreader.capture

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntentRouterTest {

    @Test
    fun `emit then first returns the event`() = runTest {
        val router = IntentRouter()
        val collector = async { router.actions.first() }
        kotlinx.coroutines.yield()
        router.emit(RouterEvent.PasteClipboard)
        assertEquals(RouterEvent.PasteClipboard, collector.await())
    }

    @Test
    fun `multiple emits are buffered`() = runTest {
        val router = IntentRouter()
        // Buffer is 8; emit before any subscribers and confirm a fresh subscriber
        // collects nothing (SharedFlow with no replay does not deliver past events).
        router.emit(RouterEvent.PasteClipboard)
        router.emit(RouterEvent.Resume)
        router.actions.test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `buffered emits delivered after collection starts`() = runTest {
        val router = IntentRouter()
        val collected = async {
            router.actions.take(3).toList()
        }
        // Yield so the collector is subscribed.
        kotlinx.coroutines.yield()
        router.emit(RouterEvent.PasteClipboard)
        router.emit(RouterEvent.Resume)
        router.emit(RouterEvent.OpenFile)
        assertEquals(
            listOf(RouterEvent.PasteClipboard, RouterEvent.Resume, RouterEvent.OpenFile),
            collected.await()
        )
    }

    @Test
    fun `two collectors both receive same event`() = runTest {
        val router = IntentRouter()
        val a = async { router.actions.first() }
        val b = async { router.actions.first() }
        kotlinx.coroutines.yield()
        router.emit(RouterEvent.ImportText("hello"))
        assertEquals(RouterEvent.ImportText("hello"), a.await())
        assertEquals(RouterEvent.ImportText("hello"), b.await())
    }

    @Test
    fun `ImportText carries payload`() = runTest {
        val router = IntentRouter()
        val collector = async { router.actions.first() }
        kotlinx.coroutines.yield()
        router.emit(RouterEvent.ImportText("payload"))
        val ev = collector.await()
        assertEquals(RouterEvent.ImportText("payload"), ev)
    }
}
