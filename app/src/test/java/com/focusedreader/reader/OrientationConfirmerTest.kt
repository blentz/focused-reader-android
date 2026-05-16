package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrientationConfirmerTest {
    @Test fun `first reading is pending not confirmed`() {
        val c = OrientationConfirmer()
        assertNull(c.observe(FaceOrientation.UP))
    }

    @Test fun `two consecutive same readings confirm`() {
        val c = OrientationConfirmer()
        c.observe(FaceOrientation.UP)
        assertEquals(FaceOrientation.UP, c.observe(FaceOrientation.UP))
    }

    @Test fun `alternating readings do not confirm`() {
        val c = OrientationConfirmer()
        c.observe(FaceOrientation.UP)
        assertNull(c.observe(FaceOrientation.DOWN))
        assertNull(c.observe(FaceOrientation.UP))
    }

    @Test fun `same as already confirmed returns null`() {
        val c = OrientationConfirmer()
        c.observe(FaceOrientation.UP); c.observe(FaceOrientation.UP)
        assertNull(c.observe(FaceOrientation.UP))
    }

    @Test fun `change requires confirmation`() {
        val c = OrientationConfirmer()
        c.observe(FaceOrientation.UP); c.observe(FaceOrientation.UP)
        assertNull(c.observe(FaceOrientation.DOWN))
        assertEquals(FaceOrientation.DOWN, c.observe(FaceOrientation.DOWN))
    }
}
