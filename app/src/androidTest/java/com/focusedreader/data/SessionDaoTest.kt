package com.focusedreader.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SessionDao

    @Before fun setup() {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        dao = db.sessions()
    }
    @After fun tearDown() { db.close() }

    @Test fun upsert_then_get() = runBlocking {
        dao.upsert(Session(text = "hello", position = 0, source = ImportSource.SHARE, importedAt = 1L))
        val got = dao.get()!!
        assertEquals("hello", got.text)
        assertEquals(ImportSource.SHARE, got.source)
    }

    @Test fun upsert_replaces_existing() = runBlocking {
        dao.upsert(Session(text = "a", position = 0, source = ImportSource.SHARE, importedAt = 1L))
        dao.upsert(Session(text = "b", position = 5, source = ImportSource.A11Y, importedAt = 2L))
        val got = dao.get()!!
        assertEquals("b", got.text); assertEquals(5, got.position)
    }

    @Test fun updatePosition() = runBlocking {
        dao.upsert(Session(text = "x", position = 0, source = ImportSource.CLIPBOARD, importedAt = 1L))
        dao.updatePosition(42)
        assertEquals(42, dao.get()!!.position)
    }

    @Test fun clear_empties() = runBlocking {
        dao.upsert(Session(text = "x", position = 0, source = ImportSource.SHARE, importedAt = 1L))
        dao.clear()
        assertNull(dao.get())
    }
}
