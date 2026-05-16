package com.focusedreader.reader

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RsvpEngineTest {
    @Test fun `emits each token at wpm cadence`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RsvpEngine(dispatcher)
        val tokens = listOf("a", "b", "c")
        engine.start(tokens, startIndex = 0, wpm = 600) // 100ms/tick

        engine.index.test {
            assertEquals(0, awaitItem())
            advanceTimeBy(100); assertEquals(1, awaitItem())
            advanceTimeBy(100); assertEquals(2, awaitItem())
            advanceTimeBy(100); cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `pause stops emission`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RsvpEngine(dispatcher)
        engine.start(listOf("a", "b", "c"), 0, 600)
        engine.index.test {
            assertEquals(0, awaitItem())
            engine.pause()
            advanceTimeBy(500)
            expectNoEvents()
        }
    }

    @Test fun `setWpm changes next-tick delay`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = RsvpEngine(dispatcher)
        engine.start(listOf("a","b","c"), 0, 600)
        engine.index.test {
            assertEquals(0, awaitItem())
            engine.setWpm(300) // 200ms/tick
            advanceTimeBy(199); expectNoEvents()
            advanceTimeBy(1); assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
