package com.focusedreader.reader

/**
 * Confirms a sustained orientation change. Requires two consecutive
 * readings of a new orientation before reporting it. Filters out
 * one-off accelerometer noise.
 */
class OrientationConfirmer {
    private var pending: FaceOrientation? = null
    private var confirmed: FaceOrientation = FaceOrientation.UNKNOWN

    /** Returns the new confirmed orientation if it changed, else null. */
    fun observe(reading: FaceOrientation): FaceOrientation? {
        if (reading == confirmed) {
            pending = null
            return null
        }
        return if (pending == reading) {
            confirmed = reading
            pending = null
            confirmed
        } else {
            pending = reading
            null
        }
    }
}
